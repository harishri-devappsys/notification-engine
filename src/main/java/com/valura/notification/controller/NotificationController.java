package com.valura.notification.controller;

import com.valura.notification.model.SendEmailModel;
import com.valura.notification.model.SendPhoneModel;
import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationFrequency;
import com.valura.notification.repository.NotificationRepository;
import com.valura.notification.repository.UserPreferenceRepository;
import com.valura.notification.service.impl.NotificationServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserPreferenceRepository userPreferenceRepository; // RabbitTemplate is no longer directly used in controller for sending
    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;

    public NotificationController(
            UserPreferenceRepository userPreferenceRepository,
            NotificationRepository notificationRepository,
            NotificationServiceImpl notificationService
    ) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // New DTO for Push Notification requests
    public static class PushNotificationRequest {
        private String recipientId;
        private String title;
        private String body;

        public String getRecipientId() { return recipientId; }
        public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    // DTO for returning Notification Stats (remains largely same)
    public static class NotificationStats {
        private String channelType;
        private int dailyCount;
        private String lastSentAt;

        public NotificationStats(String channelType, int dailyCount, String lastSentAt) {
            this.channelType = channelType;
            this.dailyCount = dailyCount;
            this.lastSentAt = lastSentAt;
        }

        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
        public int getDailyCount() { return dailyCount; }
        public void setDailyCount(int dailyCount) { this.dailyCount = dailyCount; }
        public String getLastSentAt() { return lastSentAt; }
        public void setLastSentAt(String lastSentAt) { this.lastSentAt = lastSentAt; }
    }


    @PostMapping("/send/email")
    public CompletableFuture<ResponseEntity<String>> sendEmailNotification(@RequestBody SendEmailModel request) {
        return notificationService.sendEmail(request)
                .thenApply(voidResult -> ResponseEntity.ok("Email notification request processed."))
                .exceptionally(ex -> ResponseEntity.badRequest().body("Failed to process email notification: " + ex.getMessage()));
    }
//
//    @PostMapping("/send/sms")
//    public CompletableFuture<ResponseEntity<String>> sendSmsNotification(@RequestBody SendPhoneModel request) {
//        return notificationService.sendSms(request)
//                .thenApply(voidResult -> ResponseEntity.ok("SMS notification request processed."))
//                .exceptionally(ex -> ResponseEntity.badRequest().body("Failed to process SMS notification: " + ex.getMessage()));
//    }
//
//    @PostMapping("/send/push")
//    public CompletableFuture<ResponseEntity<String>> sendPushNotification(@RequestBody PushNotificationRequest request) {
//        // notificationService.sendPush expects a String message which is then parsed internally.
//        // For consistency and cleaner API, we send the structured DTO and convert it to string here.
//        // Alternatively, the sendPush method in NotificationServiceImpl could be updated to take PushNotificationRequest.
//        // For now, maintaining the existing service signature.
//        String messagePayload = String.format("{\"recipientId\": \"%s\", \"title\": \"%s\", \"body\": \"%s\"}",
//                request.getRecipientId(), request.getTitle(), request.getBody());
//
//        return notificationService.sendPush(messagePayload)
//                .thenApply(voidResult -> ResponseEntity.ok("Push notification request processed."))
//                .exceptionally(ex -> ResponseEntity.badRequest().body("Failed to process Push notification: " + ex.getMessage()));
//    }
//
//    @GetMapping("/status/all")
//    public ResponseEntity<List<Notification>> getAllNotifications() {
//        List<Notification> notifications = notificationRepository.findAll();
//        return ResponseEntity.ok(notifications);
//    }
//
//    @GetMapping("/status/recipient/{recipientId}")
//    public ResponseEntity<List<Notification>> getRecipientNotifications(@PathVariable String recipientId) {
//        List<Notification> notifications = notificationRepository.findByRecipientId(recipientId);
//        return ResponseEntity.ok(notifications);
//    }
//
//    @GetMapping("/status/latest/recipient/{recipientId}")
//    public ResponseEntity<Notification> getLatestRecipientNotification(@PathVariable String recipientId) {
//        List<Notification> notifications = notificationRepository.findByRecipientId(recipientId);
//        Optional<Notification> latestNotification = notifications.stream()
//                .max((n1, n2) -> n1.getCreatedAt().compareTo(n2.getCreatedAt()));
//        if (latestNotification.isPresent()) {
//            return ResponseEntity.ok(latestNotification.get());
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @GetMapping("/stats/recipient/{recipientId}")
//    public ResponseEntity<List<NotificationStats>> getNotificationStats(
//            @PathVariable String recipientId,
//            @RequestParam(required = false) String channelType
//    ) {
//        var stats = notificationService.getNotificationStats(recipientId, channelType)
//                .stream()
//                .map(freq -> new NotificationStats(
//                        freq.getChannelType(),
//                        freq.getDailyCount(),
//                        freq.getLastSentAt().equals(Instant.EPOCH) ? "Never" : freq.getLastSentAt().toString()
//                ))
//                .collect(Collectors.toList());
//        return ResponseEntity.ok(stats);
//    }
}