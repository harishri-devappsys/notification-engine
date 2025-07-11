package com.valura.notification.service;

import com.valura.notification.config.RabbitMQConfig;
import com.valura.notification.model.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

    private final NotificationService notificationService;

    public RabbitMQConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(NotificationDTO notificationDTO) {
        logger.info("Received notification for userId: {}", notificationDTO.getUserId());
        notificationService.processNotification(notificationDTO.toNotification());
    }
}