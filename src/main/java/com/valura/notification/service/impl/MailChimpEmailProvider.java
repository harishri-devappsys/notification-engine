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

import java.util.Collections;
import java.util.HashMap;
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
            logger.info("Attempting to send Mailchimp Transactional (Mandrill) email notification to: {}", emailAddress);
            logger.debug("Mandrill email notification details - Title: {}, Body: {}", notification.getTitle(), notification.getBody());

            try {
                if (!isConfigured()) {
                    throw new IllegalStateException("Mailchimp Transactional (Mandrill) provider is not properly configured. Check API key and from email.");
                }

                String mandrillSendUrl = "https://mandrillapp.com/api/1.0/messages/send.json";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBasicAuth("anystring", apiKey);

                Map<String, Object> message = new HashMap<>();
                message.put("from_email", fromEmail);
                message.put("from_name", fromName);
                message.put("to", Collections.singletonList(Map.of("email", emailAddress, "type", "to")));
                message.put("subject", notification.getTitle());
                message.put("html", notification.getBody());
                message.put("text", notification.getBody());

                Map<String, Object> payload = new HashMap<>();
                payload.put("key", apiKey);
                payload.put("message", message);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.exchange(mandrillSendUrl, HttpMethod.POST, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Successfully sent Mailchimp Transactional (Mandrill) email notification to: {}", emailAddress);
                    return new NotificationResponse(
                            true,
                            "Successfully sent Mailchimp Transactional (Mandrill) email notification to: " + emailAddress
                    );
                } else {
                    logger.error("Failed to send Mailchimp Transactional (Mandrill) email. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                    return new NotificationResponse(
                            false,
                            "Failed to send Mailchimp Transactional (Mandrill) email: " + response.getStatusCode() + " - " + response.getBody()
                    );
                }

            } catch (Exception e) {
                logger.error("Error sending Mailchimp Transactional (Mandrill) email notification to: {}", emailAddress);
                logger.error("Error details: {}", e.getMessage());
                logger.error("Stack trace:", e);
                return new NotificationResponse(
                        false,
                        "Failed to send Mailchimp Transactional (Mandrill) email: " + e.getMessage()
                );
            }
        });
    }

    @Override
    public String getProviderName() {
        return "Mailchimp Transactional (Mandrill)";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() &&
                fromEmail != null && !fromEmail.trim().isEmpty();
    }
}