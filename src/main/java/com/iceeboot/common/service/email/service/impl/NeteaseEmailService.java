package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.service.AbstractEmailService;

import java.util.Properties;

/**
 * 网易邮箱服务实现类
 * 基于SMTP协议实现网易邮箱发送功能
 * 支持163.com、126.com、yeah.net等网易邮箱
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
public class NeteaseEmailService extends AbstractEmailService {

    @Override
    public String getProvider() {
        return "netease";
    }

    @Override
    protected String getProviderDisplayName() {
        return "网易邮箱";
    }

    @Override
    protected String getDefaultSmtpHost() {
        return "smtp.163.com";
    }

    @Override
    protected int getDefaultSmtpPort() {
        return 25;
    }

    @Override
    protected int getDefaultSslPort() {
        return 465;
    }

    @Override
    protected void configureSessionProperties(Properties props, EmailRequest request) {
        String smtpPort = props.getProperty("mail.smtp.port");

        // 网易邮箱需要SSL Socket Factory
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");

        if (Boolean.TRUE.equals(request.getEnableSsl())) {
            props.put("mail.smtp.socketFactory.port", smtpPort);
        } else if (Boolean.TRUE.equals(request.getEnableTls())) {
            props.put("mail.smtp.starttls.required", "true");
        }

        props.put("mail.debug", "false");
    }
}
