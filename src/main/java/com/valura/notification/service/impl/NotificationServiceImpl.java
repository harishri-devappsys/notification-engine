package com.valura.notification.service.impl;

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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationFrequencyRepository notificationFrequencyRepository;
    private final EmailNotificationService emailService;

    @Value("${notification.frequency.min-interval-seconds:2}")
    private long minIntervalSeconds = 2;

    @Value("${notification.deduplication.window-minutes:30}")
    private long deduplicationWindowMinutes = 30;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserPreferenceRepository userPreferenceRepository,
            NotificationFrequencyRepository notificationFrequencyRepository,
            EmailNotificationService emailService
    ) {
        this.notificationRepository = notificationRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.notificationFrequencyRepository = notificationFrequencyRepository;
        this.emailService = emailService;
    }

    @Override
    public CompletableFuture<Void> processNotification(Notification notification) {
        return CompletableFuture.runAsync(() -> {
            try {
                Notification duplicateCheck = checkForDuplicates(notification);
                if (duplicateCheck != null) {
                    logger.info("Duplicate notification detected. Original notification ID: {}", duplicateCheck.getId());
                    Notification duplicateNotification = new Notification(
                            notification.getUserId(),
                            notification.getTitle(),
                            notification.getBody(),
                            NotificationStatus.DUPLICATE
                    );
                    duplicateNotification.setResponse(new NotificationResponse(
                            false,
                            "Duplicate of notification " + duplicateCheck.getId() + " sent at " + duplicateCheck.getCreatedAt()
                    ));
                    notificationRepository.save(duplicateNotification);
                    return;
                }

                List<UserPreference> userPreferences = userPreferenceRepository.findAllByUserId(notification.getUserId());
                if (userPreferences.isEmpty()) {
                    throw new IllegalStateException("User preferences not found for userId: " + notification.getUserId());
                }
                UserPreference userPreference = userPreferences.get(0);

                notification.setStatus(NotificationStatus.PROCESSING);
                notification.setUpdatedAt(Instant.now());
                notificationRepository.save(notification);

                NotificationResponse response = sendNotification(notification, userPreference);

                notification.setStatus(response.isSuccess() ? NotificationStatus.DELIVERED : NotificationStatus.FAILED);
                notification.setResponse(response);
                notification.setUpdatedAt(Instant.now());
                notificationRepository.save(notification);

            } catch (Exception e) {
                logger.error("Error processing notification for userId: {}", notification.getUserId(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setResponse(new NotificationResponse(
                        false,
                        e.getMessage()
                ));
                notification.setUpdatedAt(Instant.now());
                notificationRepository.save(notification);
            }
        });
    }

    private Notification checkForDuplicates(Notification notification) {
        Instant since = Instant.now().minus(deduplicationWindowMinutes, ChronoUnit.MINUTES);
        List<Notification> duplicates = notificationRepository.findDuplicates(
                notification.getUserId(),
                notification.getContentHash(),
                since
        );
        return duplicates.stream()
                .filter(n -> n.getStatus() == NotificationStatus.DELIVERED)
                .findFirst()
                .orElse(null);
    }

    @Override
    public CompletableFuture<NotificationResponse> sendNotification(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            List<UserPreference> userPreferences = userPreferenceRepository.findAllByUserId(notification.getUserId());
            if (userPreferences.isEmpty()) {
                throw new IllegalStateException("User preferences not found");
            }
            return sendNotification(notification, userPreferences.get(0));
        });
    }

    private NotificationResponse sendNotification(Notification notification, UserPreference userPreference) {
        List<NotificationResponse> responses = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (NotificationChannel channel : userPreference.getNotification()) {
            if (!channel.isEnabled()) continue;

            NotificationFrequency frequency = notificationFrequencyRepository.findByUserIdAndChannelTypeAndDate(
                    notification.getUserId(),
                    channel.getType(),
                    today
            );

            if (frequency == null) {
                frequency = new NotificationFrequency(
                        notification.getUserId(),
                        channel.getType(),
                        Instant.EPOCH,
                        0
                );
            }

            long secondsSinceLastNotification = ChronoUnit.SECONDS.between(frequency.getLastSentAt(), Instant.now());
            if (secondsSinceLastNotification < minIntervalSeconds) {
                String message = "Please wait " + (minIntervalSeconds - secondsSinceLastNotification) + " seconds before sending another notification";
                logger.warn(message);
                responses.add(new NotificationResponse(false, message));
                continue;
            }

            CompletableFuture<NotificationResponse> responseFuture;
            switch (channel.getType().toLowerCase()) {
                case "firebase":
                    responseFuture = CompletableFuture.completedFuture(new NotificationResponse(false, "Firebase notification not implemented yet"));
                    break;
                case "mail":
                    responseFuture = emailService.sendNotification(notification, channel.getToken());
                    break;
                case "teams":
                    responseFuture = CompletableFuture.completedFuture(new NotificationResponse(false, "Teams notification not implemented yet"));
                    break;
                default:
                    responseFuture = CompletableFuture.completedFuture(new NotificationResponse(false, "Unknown channel type: " + channel.getType()));
            }

            NotificationResponse response = responseFuture.join();

            if (response.isSuccess()) {
                frequency.setLastSentAt(Instant.now());
                frequency.setDailyCount(frequency.getDailyCount() + 1);
                notificationFrequencyRepository.save(frequency);
            }
            responses.add(response);
        }
        boolean success = responses.stream().anyMatch(NotificationResponse::isSuccess);
        String message = responses.stream()
                .map(r -> r.getMessage() != null ? r.getMessage() : "")
                .collect(Collectors.joining("; "));
        return new NotificationResponse(success, message);
    }

    @Override
    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<NotificationFrequency> getNotificationStats(int userId, String channelType) {
        LocalDate today = LocalDate.now();
        if (channelType != null) {
            NotificationFrequency frequency = notificationFrequencyRepository.findByUserIdAndChannelTypeAndDate(userId, channelType, today);
            return frequency != null ? List.of(frequency) : new ArrayList<>();
        } else {
            return notificationFrequencyRepository.findAllByUserIdAndDate(userId, today);
        }
    }
}