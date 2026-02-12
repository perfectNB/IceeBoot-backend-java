package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.email.service.AbstractEmailService;
import org.springframework.stereotype.Component;

/**
 * QQ邮箱服务实现类
 * 基于SMTP协议实现QQ邮箱发送功能
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
@Component
public class QQEmailService extends AbstractEmailService {

    @Override
    public String getProvider() {
        return SystemConstants.EmailProvider.QQ;
    }

    @Override
    public String getProviderDisplayName() {
        return "QQ邮箱";
    }

    @Override
    public String getDefaultSmtpHost() {
        return "smtp.qq.com";
    }

    @Override
    public int getDefaultSmtpPort() {
        return SystemConstants.SmtpPort.SUBMISSION;
    }

    @Override
    public int getDefaultSslPort() {
        return SystemConstants.SmtpPort.SSL;
    }
}
