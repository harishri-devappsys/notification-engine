package com.valura.notification.service.impl;

import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public CompletableFuture<NotificationResponse> sendNotification(Notification notification, String emailAddress) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Attempting to send email notification to: {}", emailAddress);
            logger.debug("Email notification details - Title: {}, Body: {}", notification.getTitle(), notification.getBody());

            try {
                logger.debug("Creating MIME message...");
                var message = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(message, true);

                logger.debug("Setting email parameters...");
                helper.setTo(emailAddress);
                helper.setSubject(notification.getTitle());
                helper.setText(notification.getBody(), true);
                helper.setFrom(System.getenv("MAIL_USERNAME") != null ? System.getenv("MAIL_USERNAME") : "mqstream@gmail.com");

                logger.info("Sending email message...");
                mailSender.send(message);
                logger.info("Successfully sent email notification to: {}", emailAddress);

                return new NotificationResponse(
                        true,
                        "Successfully sent email notification to: " + emailAddress
                );
            } catch (Exception e) {
                logger.error("Error sending email notification to: {}", emailAddress);
                logger.error("Error details: {}", e.getMessage());
                logger.error("Stack trace:", e);
                return new NotificationResponse(
                        false,
                        "Failed to send email notification: " + e.getMessage()
                );
            }
        });
    }
}