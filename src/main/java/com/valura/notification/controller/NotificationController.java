package com.valura.notification.controller;

import com.valura.notification.config.RabbitMQConfig;
import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationStatus;
import com.valura.notification.repository.NotificationRepository;
import com.valura.notification.repository.UserPreferenceRepository;
import com.valura.notification.service.impl.NotificationServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final RabbitTemplate rabbitTemplate;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;

    public NotificationController(
            RabbitTemplate rabbitTemplate,
            UserPreferenceRepository userPreferenceRepository,
            NotificationRepository notificationRepository,
            NotificationServiceImpl notificationService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.userPreferenceRepository = userPreferenceRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    public static class NotificationRequest {
        private int userId;
        private String title;
        private String body;

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

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

    @PostMapping("/test")
    public ResponseEntity<String> sendTestNotification(@RequestBody NotificationRequest request) {
        var userPreferences = userPreferenceRepository.findAllByUserId(request.getUserId());
        if (userPreferences.isEmpty()) {
            return ResponseEntity.badRequest().body("User preferences not found for userId: " + request.getUserId());
        }

        Notification notification = new Notification(
                request.getUserId(),
                request.getTitle(),
                request.getBody(),
                NotificationStatus.PENDING
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                notification
        );

        return ResponseEntity.ok("Notification queued successfully");
    }

    @GetMapping("/status/all")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/status/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable int userId) {
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/status/latest/{userId}")
    public ResponseEntity<Notification> getLatestUserNotification(@PathVariable int userId) {
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        Optional<Notification> latestNotification = notifications.stream()
                .max((n1, n2) -> n1.getCreatedAt().compareTo(n2.getCreatedAt()));
        if (latestNotification.isPresent()) {
            return ResponseEntity.ok(latestNotification.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<List<NotificationStats>> getNotificationStats(
            @PathVariable int userId,
            @RequestParam(required = false) String channelType
    ) {
        var stats = notificationService.getNotificationStats(userId, channelType)
                .stream()
                .map(freq -> new NotificationStats(
                        freq.getChannelType(),
                        freq.getDailyCount(),
                        freq.getLastSentAt().toString()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(stats);
    }
}