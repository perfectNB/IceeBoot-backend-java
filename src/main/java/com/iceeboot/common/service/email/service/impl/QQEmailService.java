package com.iceeboot.common.service.email.service.impl;

import com.iceeboot.common.service.email.service.AbstractEmailService;

/**
 * QQ邮箱服务实现类
 * 基于SMTP协议实现QQ邮箱发送功能
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
public class QQEmailService extends AbstractEmailService {

    @Override
    public String getProvider() {
        return "qq";
    }

    @Override
    protected String getProviderDisplayName() {
        return "QQ邮箱";
    }

    @Override
    protected String getDefaultSmtpHost() {
        return "smtp.qq.com";
    }

    @Override
    protected int getDefaultSmtpPort() {
        return 587;
    }

    @Override
    protected int getDefaultSslPort() {
        return 465;
    }
}
