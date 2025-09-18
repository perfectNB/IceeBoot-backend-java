package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.dto.EmailResponse;
import com.iceeboot.common.service.email.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * QQ邮箱服务实现类
 * 基于SMTP协议实现QQ邮箱发送功能
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public class QQEmailService implements EmailService {
    
    private static final String PROVIDER_NAME = "qq";
    private static final String DEFAULT_SMTP_HOST = "smtp.qq.com";
    private static final Integer DEFAULT_SMTP_PORT = 587;
    private static final Integer DEFAULT_SSL_PORT = 465;
    
    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Mono<EmailResponse> sendEmail(EmailRequest request) {
        if (request.getHtmlContent() != null && !request.getHtmlContent().trim().isEmpty()) {
            return sendHtmlEmail(request);
        } else {
            return sendTextEmail(request);
        }
    }
    
    @Override
    public Mono<EmailResponse> sendTextEmail(EmailRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                Session session = createSession(request);
                MimeMessage message = createTextMessage(session, request);
                
                Transport.send(message);
                
                return EmailResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(message.getMessageID())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setSubject(request.getSubject())
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setEmailSize(getEmailSize(request));
                        
            } catch (Exception e) {
                log.error("QQ邮箱发送文本邮件失败: {}", e.getMessage(), e);
                return EmailResponse.failure("SEND_FAILED", "发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<EmailResponse> sendHtmlEmail(EmailRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                Session session = createSession(request);
                MimeMessage message = createHtmlMessage(session, request);
                
                Transport.send(message);
                
                return EmailResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(message.getMessageID())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setSubject(request.getSubject())
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setEmailSize(getEmailSize(request));
                        
            } catch (Exception e) {
                log.error("QQ邮箱发送HTML邮件失败: {}", e.getMessage(), e);
                return EmailResponse.failure("SEND_FAILED", "发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<EmailResponse> sendTemplateEmail(EmailRequest request) {
        // QQ邮箱不直接支持模板，这里通过替换HTML内容中的占位符实现
        return Mono.fromCallable(() -> {
            try {
                if (request.getHtmlContent() != null && request.getTemplateParams() != null) {
                    String processedContent = processTemplate(request.getHtmlContent(), request.getTemplateParams());
                    request.setHtmlContent(processedContent);
                }
                return sendHtmlEmail(request).block();
            } catch (Exception e) {
                log.error("QQ邮箱发送模板邮件失败: {}", e.getMessage(), e);
                return EmailResponse.failure("TEMPLATE_SEND_FAILED", "模板邮件发送失败: " + e.getMessage())
                        .setProvider(getProvider());
            }
        });
    }
    
    @Override
    public Mono<EmailResponse> sendBatchEmail(EmailRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                Session session = createSession(request);
                List<EmailResponse.EmailSendResult> results = new ArrayList<>();
                List<String> messageIds = new ArrayList<>();
                boolean allSuccess = true;
                
                for (String toEmail : request.getToEmails()) {
                    try {
                        EmailRequest singleRequest = cloneRequest(request).setToEmail(toEmail);
                        MimeMessage message = request.getHtmlContent() != null ? 
                                createHtmlMessage(session, singleRequest) : 
                                createTextMessage(session, singleRequest);
                        
                        Transport.send(message);
                        
                        messageIds.add(message.getMessageID());
                        results.add(new EmailResponse.EmailSendResult()
                                .setToEmail(toEmail)
                                .setSuccess(true)
                                .setMessageId(message.getMessageID())
                                .setStatus("SENT")
                                .setSendTime(LocalDateTime.now()));
                                
                    } catch (Exception e) {
                        allSuccess = false;
                        results.add(new EmailResponse.EmailSendResult()
                                .setToEmail(toEmail)
                                .setSuccess(false)
                                .setErrorMessage(e.getMessage())
                                .setStatus("FAILED")
                                .setSendTime(LocalDateTime.now()));
                    }
                }
                
                EmailResponse response = allSuccess ? EmailResponse.success() : 
                        EmailResponse.failure("BATCH_PARTIAL_FAILED", "批量发送部分失败");
                        
                return response.setProvider(getProvider())
                        .setMessageIds(messageIds)
                        .setFromEmail(request.getFromEmail())
                        .setToEmails(request.getToEmails())
                        .setSubject(request.getSubject())
                        .setStatus(allSuccess ? "SENT" : "PARTIAL_FAILED")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setSendResults(results);
                        
            } catch (Exception e) {
                log.error("QQ邮箱批量发送邮件失败: {}", e.getMessage(), e);
                return EmailResponse.failure("BATCH_SEND_FAILED", "批量发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setFromEmail(request.getFromEmail())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<EmailResponse> sendEmailWithAttachments(EmailRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                Session session = createSession(request);
                MimeMessage message = createMessageWithAttachments(session, request);
                
                Transport.send(message);
                
                return EmailResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(message.getMessageID())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setSubject(request.getSubject())
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setEmailSize(getEmailSize(request))
                        .setAttachmentCount(request.getAttachments() != null ? request.getAttachments().size() : 0);
                        
            } catch (Exception e) {
                log.error("QQ邮箱发送带附件邮件失败: {}", e.getMessage(), e);
                return EmailResponse.failure("SEND_WITH_ATTACHMENTS_FAILED", "发送带附件邮件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<EmailResponse> sendNotificationEmail(EmailRequest request) {
        return sendEmail(request.setEmailType("notification").setPriority(2));
    }
    
    @Override
    public Mono<EmailResponse> sendMarketingEmail(EmailRequest request) {
        return sendEmail(request.setEmailType("marketing").setPriority(3));
    }
    
    @Override
    public Mono<EmailResponse> sendSystemEmail(EmailRequest request) {
        return sendEmail(request.setEmailType("system").setPriority(1));
    }
    
    @Override
    public Mono<EmailResponse> queryEmailStatus(String messageId, EmailRequest request) {
        return Mono.fromCallable(() -> {
            // QQ邮箱SMTP不支持状态查询，返回默认状态
            return EmailResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus("UNKNOWN")
                    .setResponseTime(0L)
                    .setErrorMessage("QQ邮箱SMTP不支持状态查询");
        });
    }
    
    @Override
    public Mono<Boolean> validateConfig(EmailRequest request) {
        return Mono.fromCallable(() -> {
            try {
                if (request.getFromEmail() == null || !isValidEmail(request.getFromEmail())) {
                    return false;
                }
                if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                    return false;
                }
                if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                    return false;
                }
                
                // 尝试创建会话来验证配置
                Session session = createSession(request);
                return session != null;
                
            } catch (Exception e) {
                log.error("QQ邮箱配置验证失败: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 创建邮件会话
     */
    private Session createSession(EmailRequest request) {
        Properties props = new Properties();
        
        String smtpHost = request.getSmtpHost() != null ? request.getSmtpHost() : DEFAULT_SMTP_HOST;
        Integer smtpPort = request.getSmtpPort() != null ? request.getSmtpPort() : 
                (Boolean.TRUE.equals(request.getEnableSsl()) ? DEFAULT_SSL_PORT : DEFAULT_SMTP_PORT);
        
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort.toString());
        props.put("mail.smtp.auth", "true");
        
        if (Boolean.TRUE.equals(request.getEnableSsl())) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        } else if (Boolean.TRUE.equals(request.getEnableTls())) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        
        if (request.getConnectionTimeout() != null) {
            props.put("mail.smtp.connectiontimeout", request.getConnectionTimeout().toString());
        }
        if (request.getReadTimeout() != null) {
            props.put("mail.smtp.timeout", request.getReadTimeout().toString());
        }
        
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(request.getUsername(), request.getPassword());
            }
        });
    }
    
    /**
     * 创建文本邮件
     */
    private MimeMessage createTextMessage(Session session, EmailRequest request) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        
        // 设置发件人
        try {
            if (request.getFromName() != null) {
                message.setFrom(new InternetAddress(request.getFromEmail(), request.getFromName()));
            } else {
                message.setFrom(new InternetAddress(request.getFromEmail()));
            }
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(request.getFromEmail()));
        }
        
        // 设置收件人
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getToEmail()));
        
        // 设置抄送
        if (request.getCcEmails() != null && !request.getCcEmails().isEmpty()) {
            InternetAddress[] ccAddresses = request.getCcEmails().stream()
                    .map(email -> {
                        try {
                            return new InternetAddress(email);
                        } catch (AddressException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(InternetAddress[]::new);
            message.setRecipients(Message.RecipientType.CC, ccAddresses);
        }
        
        // 设置密送
        if (request.getBccEmails() != null && !request.getBccEmails().isEmpty()) {
            InternetAddress[] bccAddresses = request.getBccEmails().stream()
                    .map(email -> {
                        try {
                            return new InternetAddress(email);
                        } catch (AddressException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(InternetAddress[]::new);
            message.setRecipients(Message.RecipientType.BCC, bccAddresses);
        }
        
        // 设置主题
        message.setSubject(request.getSubject(), request.getCharset() != null ? request.getCharset() : "UTF-8");
        
        // 设置内容
        message.setText(request.getTextContent(), request.getCharset() != null ? request.getCharset() : "UTF-8");
        
        // 设置发送时间
        message.setSentDate(new Date());
        
        // 设置优先级
        if (request.getPriority() != null) {
            message.setHeader("X-Priority", request.getPriority().toString());
        }
        
        // 设置回复地址
        if (request.getReplyTo() != null) {
            message.setReplyTo(new InternetAddress[]{new InternetAddress(request.getReplyTo())});
        }
        
        // 设置自定义头
        if (request.getCustomHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getCustomHeaders().entrySet()) {
                message.setHeader(entry.getKey(), entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * 创建HTML邮件
     */
    private MimeMessage createHtmlMessage(Session session, EmailRequest request) throws MessagingException {
        MimeMessage message = createTextMessage(session, request);
        
        // 设置HTML内容
        message.setContent(request.getHtmlContent(), "text/html; charset=" + 
                (request.getCharset() != null ? request.getCharset() : "UTF-8"));
        
        return message;
    }
    
    /**
     * 创建带附件的邮件
     */
    private MimeMessage createMessageWithAttachments(Session session, EmailRequest request) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        
        // 设置基本信息（复用createTextMessage的逻辑）
        MimeMessage baseMessage = createTextMessage(session, request);
        message.setFrom(baseMessage.getFrom()[0]);
        message.setRecipients(Message.RecipientType.TO, baseMessage.getRecipients(Message.RecipientType.TO));
        if (baseMessage.getRecipients(Message.RecipientType.CC) != null) {
            message.setRecipients(Message.RecipientType.CC, baseMessage.getRecipients(Message.RecipientType.CC));
        }
        if (baseMessage.getRecipients(Message.RecipientType.BCC) != null) {
            message.setRecipients(Message.RecipientType.BCC, baseMessage.getRecipients(Message.RecipientType.BCC));
        }
        message.setSubject(baseMessage.getSubject());
        message.setSentDate(new Date());
        
        // 创建多部分内容
        MimeMultipart multipart = new MimeMultipart();
        
        // 添加邮件正文
        MimeBodyPart textPart = new MimeBodyPart();
        if (request.getHtmlContent() != null && !request.getHtmlContent().trim().isEmpty()) {
            textPart.setContent(request.getHtmlContent(), "text/html; charset=" + 
                    (request.getCharset() != null ? request.getCharset() : "UTF-8"));
        } else {
            textPart.setText(request.getTextContent(), request.getCharset() != null ? request.getCharset() : "UTF-8");
        }
        multipart.addBodyPart(textPart);
        
        // 添加附件
        if (request.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : request.getAttachments()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                
                if (attachment.getContent() != null) {
                    attachmentPart.setDataHandler(new javax.activation.DataHandler(
                            new javax.activation.DataSource() {
                                @Override
                                public java.io.InputStream getInputStream() {
                                    return new ByteArrayInputStream(attachment.getContent());
                                }
                                
                                @Override
                                public java.io.OutputStream getOutputStream() {
                                    throw new UnsupportedOperationException();
                                }
                                
                                @Override
                                public String getContentType() {
                                    return attachment.getMimeType() != null ? attachment.getMimeType() : "application/octet-stream";
                                }
                                
                                @Override
                                public String getName() {
                                    return attachment.getFileName();
                                }
                            }));
                } else if (attachment.getFilePath() != null) {
                    try {
                        attachmentPart.attachFile(attachment.getFilePath());
                    } catch (java.io.IOException e) {
                        log.warn("附件文件读取失败: {}", attachment.getFilePath(), e);
                        continue;
                    }
                }
                
                attachmentPart.setFileName(attachment.getFileName());
                
                if (Boolean.TRUE.equals(attachment.getInline()) && attachment.getContentId() != null) {
                    attachmentPart.setHeader("Content-ID", "<" + attachment.getContentId() + ">");
                    attachmentPart.setDisposition(MimeBodyPart.INLINE);
                } else {
                    attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
                }
                
                multipart.addBodyPart(attachmentPart);
            }
        }
        
        message.setContent(multipart);
        return message;
    }
    
    /**
     * 处理模板内容
     */
    private String processTemplate(String template, Map<String, Object> params) {
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    /**
     * 克隆请求对象
     */
    private EmailRequest cloneRequest(EmailRequest original) {
        return new EmailRequest()
                .setFromEmail(original.getFromEmail())
                .setFromName(original.getFromName())
                .setSubject(original.getSubject())
                .setTextContent(original.getTextContent())
                .setHtmlContent(original.getHtmlContent())
                .setTemplateId(original.getTemplateId())
                .setTemplateParams(original.getTemplateParams())
                .setAttachments(original.getAttachments())
                .setSmtpHost(original.getSmtpHost())
                .setSmtpPort(original.getSmtpPort())
                .setUsername(original.getUsername())
                .setPassword(original.getPassword())
                .setEnableSsl(original.getEnableSsl())
                .setEnableTls(original.getEnableTls())
                .setEnableAuth(original.getEnableAuth())
                .setConnectionTimeout(original.getConnectionTimeout())
                .setReadTimeout(original.getReadTimeout())
                .setPriority(original.getPriority())
                .setNeedReceipt(original.getNeedReceipt())
                .setReplyTo(original.getReplyTo())
                .setCharset(original.getCharset())
                .setCustomHeaders(original.getCustomHeaders())
                .setExtraParams(original.getExtraParams())
                .setEmailType(original.getEmailType())
                .setAsync(original.getAsync());
    }
}