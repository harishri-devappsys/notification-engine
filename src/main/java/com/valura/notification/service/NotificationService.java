package com.valura.notification.service;

import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationResponse;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    CompletableFuture<Void> processNotification(Notification notification);
    CompletableFuture<NotificationResponse> sendNotification(Notification notification);
    Notification saveNotification(Notification notification);
}