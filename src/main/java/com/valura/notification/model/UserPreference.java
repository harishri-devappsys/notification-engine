package com.valura.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "user_preferences")
public class UserPreference {
    @Id
    private String id;
    private int userId;
    @Field("notification")
    private List<NotificationChannel> notification;

    public UserPreference() {}

    public UserPreference(int userId, List<NotificationChannel> notification) {
        this.userId = userId;
        this.notification = notification;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public List<NotificationChannel> getNotification() { return notification; }
    public void setNotification(List<NotificationChannel> notification) { this.notification = notification; }
}

