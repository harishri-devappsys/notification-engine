package com.valura.notification.service;

import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for email notification providers
 * Supports different email service providers like SMTP, Mailchimp, etc.
 */
public interface EmailProvider {

    /**
     * Send email notification using the specific provider implementation
     *
     * @param notification The notification to send
     * @param emailAddress The recipient email address
     * @return CompletableFuture containing the notification response
     */
    CompletableFuture<NotificationResponse> sendEmail(Notification notification, String emailAddress);

    /**
     * Get the provider name for logging and identification
     *
     * @return The name of the email provider
     */
    String getProviderName();

    /**
     * Check if the provider is properly configured and ready to send emails
     *
     * @return true if provider is ready, false otherwise
     */
    boolean isConfigured();
}