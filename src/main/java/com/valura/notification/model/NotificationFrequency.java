package com.valura.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "notification_frequency")
public class NotificationFrequency {
    @Id
    private String id;
    // Changed from int userId to String recipientId
    private String recipientId;
    private String channelType; // e.g., "email", "sms", "push"
    private Instant lastSentAt;
    private int dailyCount;
    private LocalDate date;

    public NotificationFrequency() {
        this.date = LocalDate.now();
    }

    // Updated constructor to use String recipientId
    public NotificationFrequency(String recipientId, String channelType, Instant lastSentAt, int dailyCount) {
        this.recipientId = recipientId;
        this.channelType = channelType;
        this.lastSentAt = lastSentAt;
        this.dailyCount = dailyCount;
        this.date = LocalDate.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Updated getter and setter for recipientId
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public int getDailyCount() { return dailyCount; }
    public void setDailyCount(int dailyCount) { this.dailyCount = dailyCount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}