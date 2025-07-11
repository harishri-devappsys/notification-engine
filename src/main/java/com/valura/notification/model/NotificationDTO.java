package com.valura.notification.model;

public class NotificationDTO {
    private int userId;
    private String title;
    private String body;

    public NotificationDTO() {}

    public NotificationDTO(int userId, String title, String body) {
        this.userId = userId;
        this.title = title;
        this.body = body;
    }

    public Notification toNotification() {
        return new Notification(
                userId,
                title,
                body,
                NotificationStatus.PENDING
        );
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}