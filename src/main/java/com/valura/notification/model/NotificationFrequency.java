package com.valura.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "notification_frequency")
public class NotificationFrequency {
    @Id
    private String id;
    private int userId;
    private String channelType;
    private Instant lastSentAt;
    private int dailyCount;
    private LocalDate date;

    public NotificationFrequency() {
        this.date = LocalDate.now();
    }

    public NotificationFrequency(int userId, String channelType, Instant lastSentAt, int dailyCount) {
        this.userId = userId;
        this.channelType = channelType;
        this.lastSentAt = lastSentAt;
        this.dailyCount = dailyCount;
        this.date = LocalDate.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public int getDailyCount() { return dailyCount; }
    public void setDailyCount(int dailyCount) { this.dailyCount = dailyCount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}