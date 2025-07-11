package com.valura.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationResponse;
import com.valura.notification.service.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "mailchimp")
public class MailChimpEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(MailChimpEmailProvider.class);

    @Value("${notification.mailchimp.api-key:}")
    private String apiKey;

    @Value("${notification.mailchimp.from-email:}")
    private String fromEmail;

    @Value("${notification.mailchimp.from-name:Valura Notifications}")
    private String fromName;

    @Value("${notification.mailchimp.list-id:}")
    private String listId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MailChimpEmailProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<NotificationResponse> sendEmail(Notification notification, String emailAddress) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Attempting to send Mailchimp email notification to: {}", emailAddress);
            logger.debug("Mailchimp email notification details - Title: {}, Body: {}", notification.getTitle(), notification.getBody());

            try {
                if (!isConfigured()) {
                    throw new IllegalStateException("Mailchimp provider is not properly configured");
                }

                // Extract datacenter from API key (e.g., us1, us2, etc.)
                String datacenter = apiKey.substring(apiKey.lastIndexOf('-') + 1);

                // First, check if the email exists in the list, if not add it
                String memberHash = createMemberHash(emailAddress);
                String memberUrl = String.format("https://%s.api.mailchimp.com/3.0/lists/%s/members/%s",
                        datacenter, listId, memberHash);

                // Add or update member
                addOrUpdateMember(memberUrl, emailAddress);

                // Create and send campaign
                String campaignId = createCampaign(datacenter, notification);
                if (campaignId != null) {
                    boolean sent = sendCampaign(datacenter, campaignId);
                    if (sent) {
                        logger.info("Successfully sent Mailchimp email notification to: {}", emailAddress);
                        return new NotificationResponse(
                                true,
                                "Successfully sent Mailchimp email notification to: " + emailAddress
                        );
                    }
                }

                return new NotificationResponse(
                        false,
                        "Failed to send Mailchimp email notification"
                );

            } catch (Exception e) {
                logger.error("Error sending Mailchimp email notification to: {}", emailAddress);
                logger.error("Error details: {}", e.getMessage());
                logger.error("Stack trace:", e);
                return new NotificationResponse(
                        false,
                        "Failed to send Mailchimp email notification: " + e.getMessage()
                );
            }
        });
    }

    private String createMemberHash(String email) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(email.toLowerCase().getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating member hash", e);
        }
    }

    private void addOrUpdateMember(String memberUrl, String emailAddress) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth("anystring", apiKey);

            Map<String, Object> memberData = new HashMap<>();
            memberData.put("email_address", emailAddress);
            memberData.put("status", "subscribed");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(memberData, headers);

            restTemplate.exchange(memberUrl, HttpMethod.PUT, request, String.class);
        } catch (Exception e) {
            logger.warn("Could not add/update member, continuing with send: {}", e.getMessage());
        }
    }

    private String createCampaign(String datacenter, Notification notification) {
        try {
            String url = String.format("https://%s.api.mailchimp.com/3.0/campaigns", datacenter);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth("anystring", apiKey);

            Map<String, Object> settings = new HashMap<>();
            settings.put("subject_line", notification.getTitle());
            settings.put("from_name", fromName);
            settings.put("reply_to", fromEmail);
            settings.put("to_name", "Subscriber");

            Map<String, Object> recipients = new HashMap<>();
            recipients.put("list_id", listId);

            Map<String, Object> campaignData = new HashMap<>();
            campaignData.put("type", "regular");
            campaignData.put("settings", settings);
            campaignData.put("recipients", recipients);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(campaignData, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Parse response to get campaign ID
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                String campaignId = (String) responseMap.get("id");

                // Set campaign content
                setCampaignContent(datacenter, campaignId, notification.getBody());

                return campaignId;
            }
        } catch (Exception e) {
            logger.error("Error creating campaign: {}", e.getMessage());
        }
        return null;
    }

    private void setCampaignContent(String datacenter, String campaignId, String htmlContent) {
        try {
            String url = String.format("https://%s.api.mailchimp.com/3.0/campaigns/%s/content",
                    datacenter, campaignId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth("anystring", apiKey);

            Map<String, Object> content = new HashMap<>();
            content.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(content, headers);

            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        } catch (Exception e) {
            logger.error("Error setting campaign content: {}", e.getMessage());
        }
    }

    private boolean sendCampaign(String datacenter, String campaignId) {
        try {
            String url = String.format("https://%s.api.mailchimp.com/3.0/campaigns/%s/actions/send",
                    datacenter, campaignId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth("anystring", apiKey);

            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error sending campaign: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Mailchimp";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() &&
                fromEmail != null && !fromEmail.trim().isEmpty() &&
                listId != null && !listId.trim().isEmpty();
    }
}