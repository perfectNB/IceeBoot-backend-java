/**
 * 阿里云邮箱服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
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
 * 阿里云邮箱服务实现类
 * 基于SMTP协议实现阿里云邮箱发送功能
 * 支持阿里云企业邮箱和个人邮箱
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public class AliyunEmailService implements EmailService {
    
    private static final String PROVIDER_NAME = "aliyun";
    private static final String DEFAULT_SMTP_HOST = "smtp.mxhichina.com";
    private static final Integer DEFAULT_SMTP_PORT = 25;
    private static final Integer DEFAULT_SSL_PORT = 465;
    private static final Integer DEFAULT_TLS_PORT = 587;
    
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
                log.error("阿里云邮箱发送文本邮件失败: {}", e.getMessage(), e);
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
                log.error("阿里云邮箱发送HTML邮件失败: {}", e.getMessage(), e);
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
        return Mono.fromCallable(() -> {
            try {
                if (request.getHtmlContent() != null && request.getTemplateParams() != null) {
                    String processedContent = processTemplate(request.getHtmlContent(), request.getTemplateParams());
                    request.setHtmlContent(processedContent);
                }
                return sendHtmlEmail(request).block();
            } catch (Exception e) {
                log.error("阿里云邮箱发送模板邮件失败: {}", e.getMessage(), e);
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
                log.error("阿里云邮箱批量发送邮件失败: {}", e.getMessage(), e);
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
                log.error("阿里云邮箱发送带附件邮件失败: {}", e.getMessage(), e);
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
            // 阿里云邮箱SMTP不支持状态查询，返回默认状态
            return EmailResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus("UNKNOWN")
                    .setResponseTime(0L)
                    .setErrorMessage("阿里云邮箱SMTP不支持状态查询");
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
                
                // 验证是否为阿里云邮箱
                if (!isAliyunEmail(request.getFromEmail())) {
                    log.warn("邮箱地址不是阿里云邮箱: {}", request.getFromEmail());
                }
                
                // 尝试创建会话来验证配置
                Session session = createSession(request);
                return session != null;
                
            } catch (Exception e) {
                log.error("阿里云邮箱配置验证失败: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 检查是否为阿里云邮箱
     */
    private boolean isAliyunEmail(String email) {
        if (email == null) {
            return false;
        }
        String lowerEmail = email.toLowerCase();
        return lowerEmail.endsWith("@aliyun.com") || 
               lowerEmail.contains("@") && (lowerEmail.split("@")[1].endsWith(".aliyun.com") ||
               lowerEmail.split("@")[1].endsWith(".alibaba-inc.com") ||
               lowerEmail.split("@")[1].endsWith(".taobao.com") ||
               lowerEmail.split("@")[1].endsWith(".tmall.com"));
    }
    
    /**
     * 根据邮箱地址获取SMTP服务器
     */
    private String getSmtpHost(String email) {
        if (email == null) {
            return DEFAULT_SMTP_HOST;
        }
        
        // 阿里云企业邮箱通常使用统一的SMTP服务器
        return DEFAULT_SMTP_HOST;
    }
    
    /**
     * 创建邮件会话
     */
    private Session createSession(EmailRequest request) {
        Properties props = new Properties();
        
        String smtpHost = request.getSmtpHost() != null ? request.getSmtpHost() : getSmtpHost(request.getFromEmail());
        Integer smtpPort = request.getSmtpPort() != null ? request.getSmtpPort() : 
                (Boolean.TRUE.equals(request.getEnableSsl()) ? DEFAULT_SSL_PORT : 
                (Boolean.TRUE.equals(request.getEnableTls()) ? DEFAULT_TLS_PORT : DEFAULT_SMTP_PORT));
        
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort.toString());
        props.put("mail.smtp.auth", "true");
        
        // 阿里云邮箱特殊配置
        if (Boolean.TRUE.equals(request.getEnableSsl())) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", smtpPort.toString());
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else if (Boolean.TRUE.equals(request.getEnableTls())) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        
        if (request.getConnectionTimeout() != null) {
            props.put("mail.smtp.connectiontimeout", request.getConnectionTimeout().toString());
        } else {
            props.put("mail.smtp.connectiontimeout", "30000");
        }
        
        if (request.getReadTimeout() != null) {
            props.put("mail.smtp.timeout", request.getReadTimeout().toString());
        } else {
            props.put("mail.smtp.timeout", "60000");
        }
        
        // 阿里云邮箱需要的额外配置
        props.put("mail.debug", "false");
        props.put("mail.smtp.ssl.trust", smtpHost);
        
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
        
        // 设置抄送和密送
        setRecipients(message, request);
        
        // 设置主题
        message.setSubject(request.getSubject(), request.getCharset() != null ? request.getCharset() : "UTF-8");
        
        // 设置内容
        message.setText(request.getTextContent(), request.getCharset() != null ? request.getCharset() : "UTF-8");
        
        // 设置其他属性
        setMessageProperties(message, request);
        
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
        
        // 设置基本信息
        try {
            if (request.getFromName() != null) {
                message.setFrom(new InternetAddress(request.getFromEmail(), request.getFromName()));
            } else {
                message.setFrom(new InternetAddress(request.getFromEmail()));
            }
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(request.getFromEmail()));
        }
        
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getToEmail()));
        setRecipients(message, request);
        message.setSubject(request.getSubject());
        
        // 创建多部分内容
        MimeMultipart multipart = new MimeMultipart("mixed");
        
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
        addAttachments(multipart, request.getAttachments());
        
        message.setContent(multipart);
        setMessageProperties(message, request);
        
        return message;
    }
    
    /**
     * 设置收件人（抄送、密送）
     */
    private void setRecipients(MimeMessage message, EmailRequest request) throws MessagingException {
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
    }
    
    /**
     * 设置邮件属性
     */
    private void setMessageProperties(MimeMessage message, EmailRequest request) throws MessagingException {
        // 设置发送时间
        message.setSentDate(new Date());
        
        // 设置优先级
        if (request.getPriority() != null) {
            message.setHeader("X-Priority", request.getPriority().toString());
        }
        
        // 设置回复地址
        if (request.getReplyTo() != null) {
            try {
                message.setReplyTo(new InternetAddress[]{new InternetAddress(request.getReplyTo())});
            } catch (AddressException e) {
                log.warn("回复地址格式错误: {}", request.getReplyTo());
            }
        }
        
        // 设置自定义头
        if (request.getCustomHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getCustomHeaders().entrySet()) {
                message.setHeader(entry.getKey(), entry.getValue());
            }
        }
        
        // 阿里云邮箱特殊头设置
        message.setHeader("X-Mailer", "Aliyun Email Service");
    }
    
    /**
     * 添加附件
     */
    private void addAttachments(MimeMultipart multipart, List<EmailRequest.EmailAttachment> attachments) throws MessagingException {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        
        for (EmailRequest.EmailAttachment attachment : attachments) {
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
            
            // 设置附件文件名
            try {
                attachmentPart.setFileName(javax.mail.internet.MimeUtility.encodeText(attachment.getFileName()));
            } catch (java.io.UnsupportedEncodingException e) {
                attachmentPart.setFileName(attachment.getFileName());
            }
            
            if (Boolean.TRUE.equals(attachment.getInline()) && attachment.getContentId() != null) {
                attachmentPart.setHeader("Content-ID", "<" + attachment.getContentId() + ">");
                attachmentPart.setDisposition(MimeBodyPart.INLINE);
            } else {
                attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
            }
            
            multipart.addBodyPart(attachmentPart);
        }
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