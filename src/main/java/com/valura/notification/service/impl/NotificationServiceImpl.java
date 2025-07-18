package com.valura.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valura.notification.model.SendEmailModel;
import com.valura.notification.model.SendPhoneModel;
import com.valura.notification.model.*;
import com.valura.notification.repository.NotificationFrequencyRepository;
import com.valura.notification.repository.NotificationRepository;
import com.valura.notification.repository.UserPreferenceRepository;
import com.valura.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationFrequencyRepository notificationFrequencyRepository;
    private final EmailNotificationService emailService;
    private final ObjectMapper objectMapper;

    @Value("${notification.frequency.min-interval-seconds:2}")
    private long minIntervalSeconds = 2;

    @Value("${notification.deduplication.window-minutes:30}")
    private long deduplicationWindowMinutes = 30;

    @Value("${notification.frequency.max-daily:500}")
    private int maxDailyNotifications = 500;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserPreferenceRepository userPreferenceRepository,
            NotificationFrequencyRepository notificationFrequencyRepository,
            EmailNotificationService emailService,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.notificationFrequencyRepository = notificationFrequencyRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<Void> sendEmail(SendEmailModel emailModel) {
        logger.info("Received email request for to: {} with subject: {}", emailModel.getTo(), emailModel.getSubject());

        Notification notification = new Notification(
                emailModel.getTo(),
                emailModel.getSubject(),
                emailModel.getBody(),
                "email",
                NotificationStatus.PENDING
        );

        return processAndDispatchNotification(notification)
                .thenCompose(preCheckResponse -> {
                    if (!preCheckResponse.isSuccess()) {
                        return CompletableFuture.failedFuture(new RuntimeException(preCheckResponse.getMessage()));
                    }
                    logger.info("Tracking successful for email to {}, proceeding to send via provider.", emailModel.getTo());
                    return emailService.sendEmail(emailModel)
                            .handle((voidResult, throwable) -> {
                                NotificationResponse finalResponse;
                                if (throwable != null) {
                                    logger.error("Error sending email via provider for {}: {}", emailModel.getTo(), throwable.getMessage());
                                    finalResponse = new NotificationResponse(false, "Provider error: " + throwable.getMessage());
                                    notification.setStatus(NotificationStatus.FAILED);
                                } else {
                                    logger.info("Email sent successfully via provider for {}.", emailModel.getTo());
                                    finalResponse = new NotificationResponse(true, "Email sent successfully via " + emailService.getCurrentProvider() + ".");
                                    notification.setStatus(NotificationStatus.DELIVERED);
                                    updateNotificationFrequency(notification);
                                }
                                notification.setResponse(finalResponse);
                                notification.setUpdatedAt(Instant.now());
                                notificationRepository.save(notification);
                                if (!finalResponse.isSuccess()) {
                                    throw new RuntimeException(finalResponse.getMessage());
                                }
                                return null;
                            });
                });
    }

    @Override
    public CompletableFuture<Void> sendSms(SendPhoneModel phoneModel) {
        logger.info("Received SMS request for phone: {}", phoneModel.phone());

        Notification notification = new Notification(
                phoneModel.phone(),
                "SMS Notification",
                phoneModel.message(),
                "sms",
                NotificationStatus.PENDING
        );

        return processAndDispatchNotification(notification)
                .thenCompose(preCheckResponse -> {
                    if (!preCheckResponse.isSuccess()) {
                        return CompletableFuture.failedFuture(new RuntimeException(preCheckResponse.getMessage()));
                    }
                    logger.info("Tracking successful for SMS to {}, proceeding to send via provider (placeholder).", phoneModel.phone());
                    CompletableFuture<Void> smsSendFuture = CompletableFuture.completedFuture(null);

                    return smsSendFuture.handle((voidResult, throwable) -> {
                        NotificationResponse finalResponse;
                        if (throwable != null) {
                            logger.error("Error sending SMS via provider for {}: {}", phoneModel.phone(), throwable.getMessage());
                            finalResponse = new NotificationResponse(false, "Provider error: " + throwable.getMessage());
                            notification.setStatus(NotificationStatus.FAILED);
                        } else {
                            logger.info("SMS sent successfully via provider (placeholder) for {}.", phoneModel.phone());
                            finalResponse = new NotificationResponse(true, "SMS sent successfully (placeholder).");
                            notification.setStatus(NotificationStatus.DELIVERED);
                            updateNotificationFrequency(notification);
                        }
                        notification.setResponse(finalResponse);
                        notification.setUpdatedAt(Instant.now());
                        notificationRepository.save(notification);
                        if (!finalResponse.isSuccess()) {
                            throw new RuntimeException(finalResponse.getMessage());
                        }
                        return null;
                    });
                });
    }

    @Override
    public CompletableFuture<Void> sendPush(String message) {
        logger.info("Received Push Notification request: {}", message);

        String recipientId;
        String title = "Push Notification";
        String body = message;

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            recipientId = jsonNode.has("recipientId") ? jsonNode.get("recipientId").asText() : null;
            if (jsonNode.has("title")) {
                title = jsonNode.get("title").asText();
            }
            if (jsonNode.has("body")) {
                body = jsonNode.get("body").asText();
            }

            if (recipientId == null || recipientId.isEmpty()) {
                throw new IllegalArgumentException("Push message must contain a 'recipientId' field.");
            }

        } catch (Exception e) {
            logger.error("Failed to parse push message or extract recipientId: {}", e.getMessage());
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid push message format or missing recipientId: " + e.getMessage()));
        }

        Notification notification = new Notification(
                recipientId,
                title,
                body,
                "push",
                NotificationStatus.PENDING
        );

        return processAndDispatchNotification(notification)
                .thenCompose(preCheckResponse -> {
                    if (!preCheckResponse.isSuccess()) {
                        return CompletableFuture.failedFuture(new RuntimeException(preCheckResponse.getMessage()));
                    }
                    logger.info("Tracking successful for Push to {}, proceeding to send via provider (placeholder).", recipientId);
                    CompletableFuture<Void> pushSendFuture = CompletableFuture.completedFuture(null);

                    return pushSendFuture.handle((voidResult, throwable) -> {
                        NotificationResponse finalResponse;
                        if (throwable != null) {
                            logger.error("Error sending Push via provider for {}: {}", recipientId, throwable.getMessage());
                            finalResponse = new NotificationResponse(false, "Provider error: " + throwable.getMessage());
                            notification.setStatus(NotificationStatus.FAILED);
                        } else {
                            logger.info("Push sent successfully via provider (placeholder) for {}.", recipientId);
                            finalResponse = new NotificationResponse(true, "Push notification sent successfully (placeholder).");
                            notification.setStatus(NotificationStatus.DELIVERED);
                            updateNotificationFrequency(notification);
                        }
                        notification.setResponse(finalResponse);
                        notification.setUpdatedAt(Instant.now());
                        notificationRepository.save(notification);
                        if (!finalResponse.isSuccess()) {
                            throw new RuntimeException(finalResponse.getMessage());
                        }
                        return null;
                    });
                });
    }

    private CompletableFuture<NotificationResponse> processAndDispatchNotification(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Notification duplicateCheck = checkForDuplicates(notification);
                if (duplicateCheck != null) {
                    logger.info("Duplicate notification detected for {}. Original ID: {}", notification.getRecipientId(), duplicateCheck.getId());
                    notification.setStatus(NotificationStatus.DUPLICATE);
                    notification.setResponse(new NotificationResponse(
                            false,
                            "Duplicate of notification " + duplicateCheck.getId() + " sent at " + duplicateCheck.getCreatedAt()
                    ));
                    notificationRepository.save(notification);
                    return notification.getResponse();
                }

                Optional<UserPreference> userPreferenceOptional = userPreferenceRepository.findByRecipientId(notification.getRecipientId());
                if (userPreferenceOptional.isPresent()) {
                    UserPreference userPreference = userPreferenceOptional.get();
                    boolean channelEnabled = userPreference.getNotification().stream()
                            .filter(channel -> channel.getType().equalsIgnoreCase(notification.getChannelType()))
                            .anyMatch(NotificationChannel::isEnabled);

                    if (!channelEnabled) {
                        logger.warn("Notification blocked for {} via channel {} due to user preferences.", notification.getRecipientId(), notification.getChannelType());
                        notification.setStatus(NotificationStatus.BLOCKED);
                        notification.setResponse(new NotificationResponse(
                                false,
                                "Notification blocked by user preference for channel: " + notification.getChannelType()
                        ));
                        notificationRepository.save(notification);
                        return notification.getResponse();
                    }
                } else {
                    logger.info("No specific user preferences found for {}. Assuming opt-in for sending.", notification.getRecipientId());
                }

                LocalDate today = LocalDate.now();
                NotificationFrequency frequency = notificationFrequencyRepository.findByRecipientIdAndChannelTypeAndDate(
                        notification.getRecipientId(),
                        notification.getChannelType(),
                        today
                );

                NotificationFrequency currentFrequency = (frequency == null) ?
                        new NotificationFrequency(notification.getRecipientId(), notification.getChannelType(), Instant.EPOCH, 0) :
                        frequency;

                // Frequency Interval Check
                long secondsSinceLastNotification = ChronoUnit.SECONDS.between(currentFrequency.getLastSentAt(), Instant.now());
                if (secondsSinceLastNotification < minIntervalSeconds) {
                    String message = "Notification rate limit hit for " + notification.getRecipientId() + " on channel " + notification.getChannelType() +
                            " due to min interval. Please wait " + (minIntervalSeconds - secondsSinceLastNotification) + " seconds.";
                    logger.warn(message);
                    notification.setStatus(NotificationStatus.BLOCKED_FREQUENCY);
                    notification.setResponse(new NotificationResponse(false, message));
                    notificationRepository.save(notification);
                    return notification.getResponse();
                }

                // Daily Count Check
                if (currentFrequency.getDailyCount() >= maxDailyNotifications) {
                    String message = "Notification daily limit hit for " + notification.getRecipientId() + " on channel " + notification.getChannelType() +
                            ". Max daily limit of " + maxDailyNotifications + " reached.";
                    logger.warn(message);
                    notification.setStatus(NotificationStatus.BLOCKED_DAILY_LIMIT);
                    notification.setResponse(new NotificationResponse(false, message));
                    notificationRepository.save(notification);
                    return notification.getResponse();
                }

                notification.setStatus(NotificationStatus.PROCESSING);
                notification.setUpdatedAt(Instant.now());
                notificationRepository.save(notification);

                return new NotificationResponse(true, "Notification cleared for dispatch.");

            } catch (Exception e) {
                logger.error("Error during pre-dispatch checks for recipient {}: {}", notification.getRecipientId(), e.getMessage(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setResponse(new NotificationResponse(
                        false,
                        "Pre-dispatch error: " + e.getMessage()
                ));
                notification.setUpdatedAt(Instant.now());
                notificationRepository.save(notification);
                throw new RuntimeException("Error during notification pre-dispatch: " + e.getMessage(), e);
            }
        });
    }

    private Notification checkForDuplicates(Notification notification) {
        Instant since = Instant.now().minus(deduplicationWindowMinutes, ChronoUnit.MINUTES);
        List<Notification> duplicates = notificationRepository.findDuplicates(
                notification.getRecipientId(),
                notification.getChannelType(),
                notification.getContentHash(),
                since
        );
        return duplicates.stream()
                .filter(n -> n.getStatus() == NotificationStatus.DELIVERED)
                .findFirst()
                .orElse(null);
    }

    private CompletableFuture<Void> updateNotificationFrequency(Notification notification) {
        return CompletableFuture.runAsync(() -> {
            try {
                LocalDate today = LocalDate.now();
                NotificationFrequency frequency = notificationFrequencyRepository.findByRecipientIdAndChannelTypeAndDate(
                        notification.getRecipientId(),
                        notification.getChannelType(),
                        today
                );

                if (frequency == null) {
                    frequency = new NotificationFrequency(
                            notification.getRecipientId(),
                            notification.getChannelType(),
                            Instant.now(),
                            1
                    );
                } else {
                    if (!frequency.getDate().isEqual(today)) {
                        frequency.setDailyCount(1);
                        frequency.setDate(today);
                    } else {
                        frequency.setDailyCount(frequency.getDailyCount() + 1);
                    }
                    frequency.setLastSentAt(Instant.now());
                }
                notificationFrequencyRepository.save(frequency);
                logger.debug("Notification frequency updated for {}: Channel {}, Count {}, Last Sent At {}",
                        notification.getRecipientId(), notification.getChannelType(), frequency.getDailyCount(), frequency.getLastSentAt());
            } catch (Exception e) {
                logger.error("Error updating notification frequency for {}: {}", notification.getRecipientId(), e.getMessage(), e);
            }
        });
    }

    public List<NotificationFrequency> getNotificationStats(String recipientId, String channelType) {
        LocalDate today = LocalDate.now();
        if (channelType != null) {
            NotificationFrequency frequency = notificationFrequencyRepository.findByRecipientIdAndChannelTypeAndDate(recipientId, channelType, today);
            return frequency != null ? List.of(frequency) : new ArrayList<>();
        } else {
            return notificationFrequencyRepository.findAllByRecipientIdAndDate(recipientId, today);
        }
    }
}