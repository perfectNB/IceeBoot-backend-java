package com.iceeboot.common.service.email.service;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.dto.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Email服务抽象基类
 * 提取所有SMTP邮件服务实现的公共逻辑，子类只需提供提供商特定的配置
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public abstract class AbstractEmailService implements EmailService {

    // ==================== 抽象方法（子类必须实现） ====================

    /**
     * 获取提供商标识
     */
    @Override
    public abstract String getProvider();

    /**
     * 获取提供商显示名称（用于日志）
     * @return 例如 "QQ邮箱", "Gmail"
     */
    public abstract String getProviderDisplayName();

    /**
     * 获取默认SMTP主机
     */
    public abstract String getDefaultSmtpHost();

    /**
     * 获取默认SMTP端口
     */
    public abstract int getDefaultSmtpPort();

    /**
     * 获取默认SSL端口
     */
    public abstract int getDefaultSslPort();

    // ==================== 可覆盖方法（子类可选择性覆盖） ====================

    /**
     * 配置提供商特定的Session属性
     * 子类可覆盖此方法添加额外的SMTP属性（如Gmail的SSL配置）
     *
     * @param props   已包含基础SMTP配置的Properties
     * @param request 邮件请求
     */
    protected void configureSessionProperties(Properties props, EmailRequest request) {
        // 默认无额外配置，子类可覆盖
    }

    // ==================== 函数式接口 ====================

    @FunctionalInterface
    protected interface SendAction {
        EmailResponse execute() throws Exception;
    }

    // ==================== 公共发送模板方法 ====================

    /**
     * 统一的发送执行模板，封装Mono包装、计时和异常处理
     */
    protected Mono<EmailResponse> executeSend(EmailRequest request, String operation, String errorCode, SendAction action) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                EmailResponse response = action.execute();
                return response.setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("{}{}失败: {}", getProviderDisplayName(), operation, e.getMessage(), e);
                return EmailResponse.failure(errorCode, operation + "失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setFromEmail(request.getFromEmail())
                        .setToEmail(request.getToEmail())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }

    // ==================== EmailService接口实现 ====================

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
        return executeSend(request, "发送文本邮件", "SEND_FAILED", () -> {
            Session session = createSession(request);
            MimeMessage message = createTextMessage(session, request);
            Transport.send(message);
            return EmailResponse.success()
                    .setMessageId(message.getMessageID())
                    .setFromEmail(request.getFromEmail())
                    .setToEmail(request.getToEmail())
                    .setSubject(request.getSubject())
                    .setStatus("SENT")
                    .setEmailSize(getEmailSize(request));
        });
    }

    @Override
    public Mono<EmailResponse> sendHtmlEmail(EmailRequest request) {
        return executeSend(request, "发送HTML邮件", "SEND_FAILED", () -> {
            Session session = createSession(request);
            MimeMessage message = createHtmlMessage(session, request);
            Transport.send(message);
            return EmailResponse.success()
                    .setMessageId(message.getMessageID())
                    .setFromEmail(request.getFromEmail())
                    .setToEmail(request.getToEmail())
                    .setSubject(request.getSubject())
                    .setStatus("SENT")
                    .setEmailSize(getEmailSize(request));
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
                log.error("{}发送模板邮件失败: {}", getProviderDisplayName(), e.getMessage(), e);
                return EmailResponse.failure("TEMPLATE_SEND_FAILED", "模板邮件发送失败: " + e.getMessage())
                        .setProvider(getProvider());
            }
        });
    }

    @Override
    public Mono<EmailResponse> sendBatchEmail(EmailRequest request) {
        return executeSend(request, "批量发送邮件", "BATCH_SEND_FAILED", () -> {
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

            return response
                    .setMessageIds(messageIds)
                    .setFromEmail(request.getFromEmail())
                    .setToEmails(request.getToEmails())
                    .setSubject(request.getSubject())
                    .setStatus(allSuccess ? "SENT" : "PARTIAL_FAILED")
                    .setSendResults(results);
        });
    }

    @Override
    public Mono<EmailResponse> sendEmailWithAttachments(EmailRequest request) {
        return executeSend(request, "发送带附件邮件", "SEND_WITH_ATTACHMENTS_FAILED", () -> {
            Session session = createSession(request);
            MimeMessage message = createMessageWithAttachments(session, request);
            Transport.send(message);
            return EmailResponse.success()
                    .setMessageId(message.getMessageID())
                    .setFromEmail(request.getFromEmail())
                    .setToEmail(request.getToEmail())
                    .setSubject(request.getSubject())
                    .setStatus("SENT")
                    .setEmailSize(getEmailSize(request))
                    .setAttachmentCount(request.getAttachments() != null ? request.getAttachments().size() : 0);
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
        return Mono.fromCallable(() -> EmailResponse.success()
                .setProvider(getProvider())
                .setMessageId(messageId)
                .setStatus("UNKNOWN")
                .setResponseTime(0L)
                .setErrorMessage(getProviderDisplayName() + "不支持状态查询"));
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
                Session session = createSession(request);
                return session != null;
            } catch (Exception e) {
                log.error("{}配置验证失败: {}", getProviderDisplayName(), e.getMessage());
                return false;
            }
        });
    }

    // ==================== 公共辅助方法 ====================

    /**
     * 创建邮件会话
     */
    protected Session createSession(EmailRequest request) {
        Properties props = new Properties();

        String smtpHost = request.getSmtpHost() != null ? request.getSmtpHost() : getDefaultSmtpHost();
        Integer smtpPort = request.getSmtpPort() != null ? request.getSmtpPort() :
                (Boolean.TRUE.equals(request.getEnableSsl()) ? getDefaultSslPort() : getDefaultSmtpPort());

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

        // 允许子类添加提供商特定的配置
        configureSessionProperties(props, request);

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
    protected MimeMessage createTextMessage(Session session, EmailRequest request) throws MessagingException {
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
                        } catch (AddressException ex) {
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
                        } catch (AddressException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(InternetAddress[]::new);
            message.setRecipients(Message.RecipientType.BCC, bccAddresses);
        }

        // 设置主题
        String charset = request.getCharset() != null ? request.getCharset() : "UTF-8";
        message.setSubject(request.getSubject(), charset);

        // 设置内容
        message.setText(request.getTextContent(), charset);

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
    protected MimeMessage createHtmlMessage(Session session, EmailRequest request) throws MessagingException {
        MimeMessage message = createTextMessage(session, request);
        String charset = request.getCharset() != null ? request.getCharset() : "UTF-8";
        message.setContent(request.getHtmlContent(), "text/html; charset=" + charset);
        return message;
    }

    /**
     * 创建带附件的邮件
     */
    protected MimeMessage createMessageWithAttachments(Session session, EmailRequest request) throws MessagingException {
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
        String charset = request.getCharset() != null ? request.getCharset() : "UTF-8";
        if (request.getHtmlContent() != null && !request.getHtmlContent().trim().isEmpty()) {
            textPart.setContent(request.getHtmlContent(), "text/html; charset=" + charset);
        } else {
            textPart.setText(request.getTextContent(), charset);
        }
        multipart.addBodyPart(textPart);

        // 添加附件
        if (request.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : request.getAttachments()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();

                if (attachment.getContent() != null) {
                    attachmentPart.setDataHandler(new DataHandler(
                            new DataSource() {
                                @Override
                                public InputStream getInputStream() {
                                    return new ByteArrayInputStream(attachment.getContent());
                                }

                                @Override
                                public OutputStream getOutputStream() {
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
     * 处理模板内容，替换占位符
     */
    protected String processTemplate(String template, Map<String, Object> params) {
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 克隆请求对象（用于批量发送时为每个收件人创建独立请求）
     */
    protected EmailRequest cloneRequest(EmailRequest original) {
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

    /**
     * 计算邮件大小（估算）
     */
    public long getEmailSize(EmailRequest request) {
        long size = 0;
        if (request.getTextContent() != null) {
            size += request.getTextContent().length();
        }
        if (request.getHtmlContent() != null) {
            size += request.getHtmlContent().length();
        }
        if (request.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : request.getAttachments()) {
                if (attachment.getContent() != null) {
                    size += attachment.getContent().length;
                }
            }
        }
        return size;
    }

    /**
     * 验证邮箱格式
     */
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }
}
