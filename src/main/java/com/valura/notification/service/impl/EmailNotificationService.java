package com.valura.notification.service.impl;

import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationResponse;
import com.valura.notification.service.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Value("${notification.email.provider:smtp}")
    private String configuredProvider;

    private final EmailProvider emailProvider;

    public EmailNotificationService(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    public CompletableFuture<NotificationResponse> sendNotification(Notification notification, String emailAddress) {
        logger.info("Using {} email provider to send notification to: {}",
                emailProvider.getProviderName(), emailAddress);

        if (!emailProvider.isConfigured()) {
            logger.error("{} email provider is not properly configured", emailProvider.getProviderName());
            return CompletableFuture.completedFuture(new NotificationResponse(
                    false,
                    emailProvider.getProviderName() + " email provider is not properly configured"
            ));
        }

        return emailProvider.sendEmail(notification, emailAddress)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        logger.error("Error occurred while sending email via {}: {}",
                                emailProvider.getProviderName(), throwable.getMessage());
                        return new NotificationResponse(
                                false,
                                "Error sending email via " + emailProvider.getProviderName() + ": " + throwable.getMessage()
                        );
                    }
                    return response;
                });
    }

    public String getCurrentProvider() {
        return emailProvider.getProviderName();
    }

    public boolean isProviderConfigured() {
        return emailProvider.isConfigured();
    }
}