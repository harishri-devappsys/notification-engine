package com.valura.notification.model;

public class NotificationChannel {
    private String token;
    private String type;
    private boolean enabled;

    public NotificationChannel() {
        this.enabled = true;
    }

    public NotificationChannel(String token, String type) {
        this.token = token;
        this.type = type;
        this.enabled = true;
    }

    public NotificationChannel(String token, String type, boolean enabled) {
        this.token = token;
        this.type = type;
        this.enabled = enabled;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
