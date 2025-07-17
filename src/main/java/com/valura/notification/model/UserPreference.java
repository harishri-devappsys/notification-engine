package com.valura.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "user_preferences")
public class UserPreference {
    @Id
    private String id;
    // Changed from int userId to String recipientId
    // This field will store the email, phone number, or push token as the unique identifier
    private String recipientId;
    @Field("notification")
    private List<NotificationChannel> notification;

    public UserPreference() {}

    // Updated constructor to use String recipientId
    public UserPreference(String recipientId, List<NotificationChannel> notification) {
        this.recipientId = recipientId;
        this.notification = notification;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Updated getter and setter for recipientId
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public List<NotificationChannel> getNotification() { return notification; }
    public void setNotification(List<NotificationChannel> notification) { this.notification = notification; }
}