package com.valura.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private int userId;
    private String title;
    private String body;
    private NotificationStatus status;
    private int deliveryAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private NotificationResponse response;
    private String contentHash;

    public Notification() {}

    public Notification(int userId, String title, String body, NotificationStatus status) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.status = status != null ? status : NotificationStatus.PENDING;
        this.deliveryAttempts = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.contentHash = generateContentHash(userId, title, body);
    }

    public static String generateContentHash(int userId, String title, String body) {
        try {
            String content = userId + ":" + title + ":" + body;
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public int getDeliveryAttempts() { return deliveryAttempts; }
    public void setDeliveryAttempts(int deliveryAttempts) { this.deliveryAttempts = deliveryAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public NotificationResponse getResponse() { return response; }
    public void setResponse(NotificationResponse response) { this.response = response; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}

