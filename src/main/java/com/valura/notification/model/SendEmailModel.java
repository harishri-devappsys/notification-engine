package com.valura.notification.model;


import lombok.*;

import java.util.List;
import java.util.Map;
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailModel {

    private String to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private boolean isHtml;
    private String from;
    private Map<String, String> headers;
    private Map<String, byte[]> attachments;
    private String templateId;
    private Map<String, Object> templateData;
    private String messageId;
    private String correlationId;
    private String replyTo;
}