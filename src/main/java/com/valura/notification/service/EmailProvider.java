package com.valura.notification.service;

import com.valura.notification.model.SendEmailModel;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for email notification providers
 * Supports different email service providers like SMTP, Mailchimp, etc.
 */
public interface EmailProvider {

    /**
     * Send email notification using the specific provider implementation, directly from the RabbitMQ model.
     *
     * @param emailModel The SendEmailModel containing all email details
     * @return CompletableFuture that completes when the email is sent, or completes exceptionally on failure
     */
    CompletableFuture<Void> sendEmail(SendEmailModel emailModel);

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