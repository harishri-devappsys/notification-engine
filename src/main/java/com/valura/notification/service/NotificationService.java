package com.valura.notification.service;

import com.valura.notification.model.SendEmailModel;
import com.valura.notification.model.SendPhoneModel;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    CompletableFuture<Void> sendEmail(SendEmailModel emailModel);
    CompletableFuture<Void> sendSms(SendPhoneModel phoneModel);
    CompletableFuture<Void> sendPush(String message);
}