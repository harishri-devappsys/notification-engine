package com.valura.notification.service;

import com.valura.notification.model.SendEmailModel;
import com.valura.notification.model.SendPhoneModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.valura.notification.model.RabbitMqNotificationServiceConstants.*;

@Component
public class RabbitMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private final NotificationService notificationService;

    public RabbitMQConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = EMAIL_SEND_QUEUE)
    public void receiveEmailRequest(SendEmailModel sendEmailModel) {
        logger.info("📧 Received email request for to: {}", sendEmailModel.getTo());
        notificationService.sendEmail(sendEmailModel)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("❌ Failed to process email for to: {}: {}", sendEmailModel.getTo(), throwable.getMessage());
                    } else {
                        logger.info("✅ Successfully processed email for to: {}", sendEmailModel.getTo());
                    }
                });
    }

    @RabbitListener(queues = SMS_SEND_QUEUE)
    public void receiveSMSRequest(SendPhoneModel sendPhoneModel) {
        logger.info("📱 Received SMS request for phone: {}", sendPhoneModel.phone());
        notificationService.sendSms(sendPhoneModel)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("❌ Failed to process SMS for phone: {}: {}", sendPhoneModel.phone(), throwable.getMessage());
                    } else {
                        logger.info("✅ Successfully processed SMS for phone: {}", sendPhoneModel.phone());
                    }
                });
    }

    @RabbitListener(queues = PUSH_SEND_QUEUE)
    public void receivePushNotificationRequest(String message) {
        logger.info("🔔 Received push notification request: {}", message);
        notificationService.sendPush(message)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("❌ Failed to process push notification: {}: {}", message, throwable.getMessage());
                    } else {
                        logger.info("✅ Successfully processed push notification: {}", message);
                    }
                });
    }
}