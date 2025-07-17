package com.valura.notification.model;



public class RabbitMqNotificationServiceConstants {
    public static final String NOTIFICATION_EXCHANGE = "notification.exchangeee";


    public static final String EMAIL_SEND_QUEUE = "notification.email.send.queue";


    public static final String EMAIL_SEND_ROUTING_KEY = "notification.email.send";


    public static final String SMS_SEND_QUEUE = "notification.sms.send.queue";
    public static final String SMS_SEND_ROUTING_KEY = "notification.sms.send";

    public static final String PUSH_SEND_QUEUE = "notification.push.send.queue";
    public static final String PUSH_SEND_ROUTING_KEY = "notification.push.send";
}
