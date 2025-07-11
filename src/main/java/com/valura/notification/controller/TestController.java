package com.valura.notification.controller;

import com.valura.notification.config.RabbitMQConfig;
import com.valura.notification.model.Notification;
import com.valura.notification.model.NotificationStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final RabbitTemplate rabbitTemplate;

    public TestController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public void sendTestNotification(
            @RequestParam int userId,
            @RequestParam String title,
            @RequestParam String body) {

        Notification notification = new Notification(
                userId,
                title,
                body,
                NotificationStatus.PENDING
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                notification
        );
    }
}