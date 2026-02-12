package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.service.AbstractEmailService;

import java.util.Properties;

/**
 * Outlook邮箱服务实现类
 * 基于SMTP协议实现Outlook邮箱发送功能
 * 支持Outlook.com、Hotmail.com、Live.com等微软邮箱
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
public class OutlookEmailService extends AbstractEmailService {

    @Override
    public String getProvider() {
        return "outlook";
    }

    @Override
    protected String getProviderDisplayName() {
        return "Outlook";
    }

    @Override
    protected String getDefaultSmtpHost() {
        return "smtp-mail.outlook.com";
    }

    @Override
    protected int getDefaultSmtpPort() {
        return 587;
    }

    @Override
    protected int getDefaultSslPort() {
        return 465;
    }

    @Override
    protected void configureSessionProperties(Properties props, EmailRequest request) {
        String smtpHost = request.getSmtpHost() != null ? request.getSmtpHost() : getDefaultSmtpHost();
        String smtpPort = props.getProperty("mail.smtp.port");

        // Outlook requires STARTTLS
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", smtpHost);

        if (Boolean.TRUE.equals(request.getEnableSsl())) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", smtpPort);
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        // Default timeouts if not specified
        if (request.getConnectionTimeout() == null) {
            props.put("mail.smtp.connectiontimeout", "30000");
        }
        if (request.getReadTimeout() == null) {
            props.put("mail.smtp.timeout", "60000");
        }

        props.put("mail.debug", "false");
        props.put("mail.smtp.ehlo", "false");
    }
}
