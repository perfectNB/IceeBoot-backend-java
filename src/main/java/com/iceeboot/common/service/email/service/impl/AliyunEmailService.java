package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.email.service.AbstractEmailService;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 阿里云邮箱服务实现类
 * 基于SMTP协议实现阿里云邮箱发送功能
 * 支持阿里云企业邮箱和个人邮箱
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
@Component
public class AliyunEmailService extends AbstractEmailService {

    @Override
    public String getProvider() {
        return SystemConstants.EmailProvider.ALIYUN;
    }

    @Override
    public String getProviderDisplayName() {
        return "阿里云邮件";
    }

    @Override
    public String getDefaultSmtpHost() {
        return "smtp.mxhichina.com";
    }

    @Override
    public int getDefaultSmtpPort() {
        return SystemConstants.SmtpPort.DEFAULT;
    }

    @Override
    public int getDefaultSslPort() {
        return SystemConstants.SmtpPort.SSL;
    }

    @Override
    protected void configureSessionProperties(Properties props, EmailRequest request) {
        String smtpHost = request.getSmtpHost() != null ? request.getSmtpHost() : getDefaultSmtpHost();
        String smtpPort = props.getProperty("mail.smtp.port");

        if (Boolean.TRUE.equals(request.getEnableSsl())) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", smtpPort);
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else if (Boolean.TRUE.equals(request.getEnableTls())) {
            props.put("mail.smtp.starttls.required", "true");
        }

        // Default timeouts if not specified
        if (request.getConnectionTimeout() == null) {
            props.put("mail.smtp.connectiontimeout", "30000");
        }
        if (request.getReadTimeout() == null) {
            props.put("mail.smtp.timeout", "60000");
        }

        props.put("mail.debug", "false");
        props.put("mail.smtp.ssl.trust", smtpHost);
    }
}
