package com.valura.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valura.notification.model.SendEmailModel;
import com.valura.notification.model.NotificationResponse;
import com.valura.notification.service.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
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
    public CompletableFuture<Void> sendEmail(SendEmailModel emailModel) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Attempting to send Mailchimp Transactional (Mandrill) email from SendEmailModel to: {} with subject: {}",
                    emailModel.getTo(), emailModel.getSubject());

            try {
                if (!isConfigured()) {
                    throw new IllegalStateException("Mailchimp Transactional (Mandrill) provider is not properly configured. Check API key and from email.");
                }

                String mandrillSendUrl = "https://mandrillapp.com/api/1.0/messages/send.json";
                String mandrillSendTemplateUrl = "https://mandrillapp.com/api/1.0/messages/send-template.json";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBasicAuth("anystring", apiKey);

                Map<String, Object> payload = new HashMap<>();
                payload.put("key", apiKey);

                if (emailModel.getTemplateId() != null && !emailModel.getTemplateId().isEmpty()) {
                    payload.put("template_name", emailModel.getTemplateId());

                    List<Map<String, String>> templateContent = new ArrayList<>();
                    payload.put("template_content", templateContent);

                    Map<String, Object> message = new HashMap<>();
                    message.put("from_email", emailModel.getFrom() != null ? emailModel.getFrom() : fromEmail);
                    message.put("from_name", fromName);

                    List<Map<String, String>> toRecipients = new ArrayList<>();
                    toRecipients.add(Map.of("email", emailModel.getTo(), "type", "to"));
                    if (emailModel.getCc() != null) {
                        emailModel.getCc().forEach(cc -> toRecipients.add(Map.of("email", cc, "type", "cc")));
                    }
                    if (emailModel.getBcc() != null) {
                        emailModel.getBcc().forEach(bcc -> toRecipients.add(Map.of("email", bcc, "type", "bcc")));
                    }
                    message.put("to", toRecipients);

                    message.put("subject", emailModel.getSubject());

                    if (emailModel.getTemplateData() != null && !emailModel.getTemplateData().isEmpty()) {
                        List<Map<String, Object>> mergeVars = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : emailModel.getTemplateData().entrySet()) {
                            mergeVars.add(Map.of("name", entry.getKey(), "content", entry.getValue()));
                        }
                        message.put("global_merge_vars", mergeVars);
                    }

                    if (emailModel.getAttachments() != null && !emailModel.getAttachments().isEmpty()) {
                        List<Map<String, Object>> attachments = new ArrayList<>();
                        for (Map.Entry<String, byte[]> entry : emailModel.getAttachments().entrySet()) {
                            Map<String, Object> attachment = new HashMap<>();
                            attachment.put("type", "application/octet-stream");
                            attachment.put("name", entry.getKey());
                            attachment.put("content", Base64.getEncoder().encodeToString(entry.getValue()));
                            attachments.add(attachment);
                        }
                        message.put("attachments", attachments);
                    }

                    if (emailModel.getHeaders() != null && !emailModel.getHeaders().isEmpty()) {
                        Map<String, String> customHeaders = new HashMap<>();
                        emailModel.getHeaders().forEach((k, v) -> customHeaders.put("X-" + k, v));
                        message.put("headers", customHeaders);
                    }

                    if (emailModel.getReplyTo() != null && !emailModel.getReplyTo().isEmpty()) {
                        message.put("reply_to", emailModel.getReplyTo());
                    }

                    payload.put("message", message);

                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                    logger.info("Sending Mailchimp Transactional (Mandrill) template email...");
                    ResponseEntity<String> response = restTemplate.exchange(mandrillSendTemplateUrl, HttpMethod.POST, request, String.class);
                    processMailchimpResponse(response, emailModel.getTo());

                } else {
                    Map<String, Object> message = new HashMap<>();
                    message.put("from_email", emailModel.getFrom() != null ? emailModel.getFrom() : fromEmail);
                    message.put("from_name", fromName);

                    List<Map<String, String>> toRecipients = new ArrayList<>();
                    toRecipients.add(Map.of("email", emailModel.getTo(), "type", "to"));
                    if (emailModel.getCc() != null) {
                        emailModel.getCc().forEach(cc -> toRecipients.add(Map.of("email", cc, "type", "cc")));
                    }
                    if (emailModel.getBcc() != null) {
                        emailModel.getBcc().forEach(bcc -> toRecipients.add(Map.of("email", bcc, "type", "bcc")));
                    }
                    message.put("to", toRecipients);

                    message.put("subject", emailModel.getSubject());
                    if (emailModel.isHtml()) {
                        message.put("html", emailModel.getBody());
                        message.put("text", "Please view this email in an HTML-enabled client.");
                    } else {
                        message.put("text", emailModel.getBody());
                    }

                    if (emailModel.getAttachments() != null && !emailModel.getAttachments().isEmpty()) {
                        List<Map<String, Object>> attachments = new ArrayList<>();
                        for (Map.Entry<String, byte[]> entry : emailModel.getAttachments().entrySet()) {
                            Map<String, Object> attachment = new HashMap<>();
                            attachment.put("type", "application/octet-stream");
                            attachment.put("name", entry.getKey());
                            attachment.put("content", Base64.getEncoder().encodeToString(entry.getValue()));
                            attachments.add(attachment);
                        }
                        message.put("attachments", attachments);
                    }

                    if (emailModel.getHeaders() != null && !emailModel.getHeaders().isEmpty()) {
                        Map<String, String> customHeaders = new HashMap<>();
                        emailModel.getHeaders().forEach((k, v) -> customHeaders.put("X-" + k, v));
                        message.put("headers", customHeaders);
                    }

                    if (emailModel.getReplyTo() != null && !emailModel.getReplyTo().isEmpty()) {
                        message.put("reply_to", emailModel.getReplyTo());
                    }

                    payload.put("message", message);

                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                    logger.info("Sending Mailchimp Transactional (Mandrill) non-template email...");
                    ResponseEntity<String> response = restTemplate.exchange(mandrillSendUrl, HttpMethod.POST, request, String.class);
                    processMailchimpResponse(response, emailModel.getTo());
                }

            } catch (Exception e) {
                logger.error("Error sending Mailchimp Transactional (Mandrill) email for SendEmailModel to: {}", emailModel.getTo());
                logger.error("Error details: {}", e.getMessage());
                logger.error("Stack trace:", e);
                throw new RuntimeException("Failed to send Mailchimp Transactional (Mandrill) email: " + e.getMessage(), e);
            }
        });
    }

    private void processMailchimpResponse(ResponseEntity<String> response, String emailAddress) {
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Successfully sent Mailchimp Transactional (Mandrill) email to: {}", emailAddress);
        } else {
            logger.error("Failed to send Mailchimp Transactional (Mandrill) email. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException(
                    "Failed to send Mailchimp Transactional (Mandrill) email: " + response.getStatusCode() + " - " + response.getBody()
            );
        }
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