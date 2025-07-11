package com.valura.notification.service.impl;

import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationResponse;
import com.valura.notification.service.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
    public CompletableFuture<NotificationResponse> sendEmail(Notification notification, String emailAddress) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Attempting to send SMTP email notification to: {}", emailAddress);
            logger.debug("SMTP email notification details - Title: {}, Body: {}", notification.getTitle(), notification.getBody());

            try {
                logger.debug("Creating MIME message...");
                var message = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(message, true);

                logger.debug("Setting email parameters...");
                helper.setTo(emailAddress);
                helper.setSubject(notification.getTitle());
                helper.setText(notification.getBody(), true);
                helper.setFrom(System.getenv("MAIL_USERNAME") != null ? System.getenv("MAIL_USERNAME") : "mqstream@gmail.com");

                logger.info("Sending SMTP email message...");
                mailSender.send(message);
                logger.info("Successfully sent SMTP email notification to: {}", emailAddress);

                return new NotificationResponse(
                        true,
                        "Successfully sent SMTP email notification to: " + emailAddress
                );
            } catch (Exception e) {
                logger.error("Error sending SMTP email notification to: {}", emailAddress);
                logger.error("Error details: {}", e.getMessage());
                logger.error("Stack trace:", e);
                return new NotificationResponse(
                        false,
                        "Failed to send SMTP email notification: " + e.getMessage()
                );
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