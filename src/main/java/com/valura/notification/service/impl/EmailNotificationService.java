package com.valura.notification.service.impl;

import com.valura.notification.model.SendEmailModel;
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

    public CompletableFuture<Void> sendEmail(SendEmailModel emailModel) {
        logger.info("Using {} email provider to send email from RabbitMQ to: {} with subject: {}",
                emailProvider.getProviderName(), emailModel.getTo(), emailModel.getSubject());

        if (!emailProvider.isConfigured()) {
            logger.error("{} email provider is not properly configured for SendEmailModel", emailProvider.getProviderName());
            return CompletableFuture.failedFuture(new IllegalStateException(
                    emailProvider.getProviderName() + " email provider is not properly configured"
            ));
        }

        return emailProvider.sendEmail(emailModel)
                .exceptionally(throwable -> {
                    logger.error("Error occurred while sending email via {} for SendEmailModel: {}",
                            emailProvider.getProviderName(), throwable.getMessage());
                    throw new RuntimeException("Failed to send email via " + emailProvider.getProviderName(), throwable);
                });
    }

    public String getCurrentProvider() {
        return emailProvider.getProviderName();
    }

    public boolean isProviderConfigured() {
        return emailProvider.isConfigured();
    }
}