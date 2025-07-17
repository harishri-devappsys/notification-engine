package com.valura.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.valura.notification.model.RabbitMqNotificationServiceConstants.*;

@Configuration
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange notificationExchange() {
        logger.info("Creating topic exchange: {}", NOTIFICATION_EXCHANGE);
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    // ------------------- EMAIL -------------------

    @Bean
    public Queue sendEmailQueue() {
        logger.info("Creating email queue: {}", EMAIL_SEND_QUEUE);
        return new Queue(EMAIL_SEND_QUEUE, true);
    }

    @Bean
    public Binding sendEmailBinding() {
        logger.info("Creating binding: {} -> {} with routing key: {}",
                NOTIFICATION_EXCHANGE, EMAIL_SEND_QUEUE, EMAIL_SEND_ROUTING_KEY);
        return BindingBuilder
                .bind(sendEmailQueue())
                .to(notificationExchange())
                .with(EMAIL_SEND_ROUTING_KEY);
    }

    // ------------------- SMS (Placeholder as per your request) -------------------

    @Bean
    public Queue sendSmsQueue() {
        logger.info("Creating SMS queue: {}", SMS_SEND_QUEUE);
        return new Queue(SMS_SEND_QUEUE, true);
    }

    @Bean
    public Binding sendSmsBinding() {
        logger.info("Creating binding: {} -> {} with routing key: {}",
                NOTIFICATION_EXCHANGE, SMS_SEND_QUEUE, SMS_SEND_ROUTING_KEY);
        return BindingBuilder
                .bind(sendSmsQueue())
                .to(notificationExchange())
                .with(SMS_SEND_ROUTING_KEY);
    }

    // ------------------- PUSH (Placeholder as per your request) -------------------

    @Bean
    public Queue sendPushQueue() {
        logger.info("Creating push queue: {}", PUSH_SEND_QUEUE);
        return new Queue(PUSH_SEND_QUEUE, true);
    }

    @Bean
    public Binding sendPushBinding() {
        logger.info("Creating binding: {} -> {} with routing key: {}",
                NOTIFICATION_EXCHANGE, PUSH_SEND_QUEUE, PUSH_SEND_ROUTING_KEY);
        return BindingBuilder
                .bind(sendPushQueue())
                .to(notificationExchange())
                .with(PUSH_SEND_ROUTING_KEY);
    }


    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin() {
        logger.info("Creating RabbitAdmin");
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}