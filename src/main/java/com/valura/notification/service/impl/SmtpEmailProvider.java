package com.valura.notification.service.impl;

import com.valura.notification.model.SendEmailModel;
import com.valura.notification.model.NotificationResponse;
import com.valura.notification.service.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final JavaMailSender mailSender;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public CompletableFuture<Void> sendEmail(SendEmailModel emailModel) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Attempting to send SMTP email from SendEmailModel to: {}", emailModel.getTo());
            logger.debug("SMTP email details - Subject: {}, Body length: {}", emailModel.getSubject(), emailModel.getBody() != null ? emailModel.getBody().length() : 0);

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                if (emailModel.getFrom() != null && !emailModel.getFrom().isEmpty()) {
                    helper.setFrom(emailModel.getFrom());
                } else {
                    helper.setFrom(System.getenv("MAIL_USERNAME") != null ? System.getenv("MAIL_USERNAME") : "mqstream@gmail.com");
                }

                helper.setTo(emailModel.getTo());
                helper.setSubject(emailModel.getSubject());
                helper.setText(emailModel.getBody(), emailModel.isHtml());

                if (emailModel.getCc() != null && !emailModel.getCc().isEmpty()) {
                    helper.setCc(emailModel.getCc().toArray(new String[0]));
                }

                if (emailModel.getBcc() != null && !emailModel.getBcc().isEmpty()) {
                    helper.setBcc(emailModel.getBcc().toArray(new String[0]));
                }

                if (emailModel.getReplyTo() != null && !emailModel.getReplyTo().isEmpty()) {
                    helper.setReplyTo(emailModel.getReplyTo());
                }

                if (emailModel.getAttachments() != null && !emailModel.getAttachments().isEmpty()) {
                    for (Map.Entry<String, byte[]> entry : emailModel.getAttachments().entrySet()) {
                        helper.addAttachment(entry.getKey(), new ByteArrayDataSource(entry.getValue(), "application/octet-stream"));
                    }
                }

                if (emailModel.getHeaders() != null && !emailModel.getHeaders().isEmpty()) {
                    for (Map.Entry<String, String> entry : emailModel.getHeaders().entrySet()) {
                        message.addHeader(entry.getKey(), entry.getValue());
                    }
                }

                logger.info("Sending SMTP email message for SendEmailModel...");
                mailSender.send(message);
                logger.info("Successfully sent SMTP email from SendEmailModel to: {}", emailModel.getTo());

            } catch (Exception e) {
                logger.error("Error sending SMTP email from SendEmailModel to: {}", emailModel.getTo());
                logger.error("Error details: {}", e.getMessage());
                logger.error("Stack trace:", e);
                throw new RuntimeException("Failed to send SMTP email from SendEmailModel: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public String getProviderName() {
        return "SMTP";
    }

    @Override
    public boolean isConfigured() {
        return mailSender != null;
    }
}