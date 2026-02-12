package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.service.AbstractEmailService;

import java.util.Properties;

/**
 * Gmail邮箱服务实现类
 * 基于SMTP协议实现Gmail邮箱发送功能
 * 支持Gmail和Google Workspace邮箱
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
public class GmailEmailService extends AbstractEmailService {

    @Override
    public String getProvider() {
        return "gmail";
    }

    @Override
    protected String getProviderDisplayName() {
        return "Gmail";
    }

    @Override
    protected String getDefaultSmtpHost() {
        return "smtp.gmail.com";
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

        // Gmail requires STARTTLS
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

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

        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.debug", "false");
    }
}
