package com.iceeboot.framework.service.email.impl;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.dto.EmailResponse;
import com.iceeboot.common.service.email.factory.EmailFactory;
import com.iceeboot.common.service.email.service.EmailService;
import com.iceeboot.framework.dto.create.email.EmailAttachmentDTO;
import com.iceeboot.framework.dto.create.email.EmailSendDTO;
import com.iceeboot.framework.dto.create.email.EmailStatusQueryDTO;
import com.iceeboot.framework.service.email.IEmailKeyService;
import com.iceeboot.framework.service.email.IEmailSendService;
import com.iceeboot.framework.vo.email.EmailKeyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailSendServiceImpl implements IEmailSendService {
    private final IEmailKeyService emailKeyService;

    @Override
    public EmailResponse sendEmail(EmailSendDTO dto) {
        EmailKeyVO key = resolveKey(dto.getKeyId(), dto.getProvider());
        EmailService emailService = EmailFactory.getService(key.getEmailProvider());
        EmailRequest request = buildRequest(dto, key);
        EmailResponse response = dispatchSend(emailService, dto, request);
        if (response == null) {
            throw new IceeBootException("邮件发送失败");
        }
        return response;
    }

    @Override
    public EmailResponse queryStatus(EmailStatusQueryDTO dto) {
        if (!StringUtils.hasText(dto.getMessageId())) {
            throw new IceeBootException("消息ID不能为空");
        }
        EmailKeyVO key = resolveKey(dto.getKeyId(), dto.getProvider());
        EmailService emailService = EmailFactory.getService(key.getEmailProvider());
        EmailRequest request = buildRequest(null, key);
        EmailResponse response = emailService.queryEmailStatus(dto.getMessageId(), request).block();
        if (response == null) {
            throw new IceeBootException("邮件状态查询失败");
        }
        return response;
    }

    private EmailResponse dispatchSend(EmailService emailService, EmailSendDTO dto, EmailRequest request) {
        String emailType = normalize(dto.getEmailType());
        if (!CollectionUtils.isEmpty(dto.getAttachments())) {
            return emailService.sendEmailWithAttachments(request).block();
        }
        if (!StringUtils.hasText(emailType)) {
            if (!CollectionUtils.isEmpty(dto.getToEmails()) && dto.getToEmails().size() > 1) {
                return emailService.sendBatchEmail(request).block();
            }
            return emailService.sendEmail(request).block();
        }
        return switch (emailType) {
            case "text" -> emailService.sendTextEmail(request).block();
            case "html" -> emailService.sendHtmlEmail(request).block();
            case "template" -> emailService.sendTemplateEmail(request).block();
            case "notification" -> emailService.sendNotificationEmail(request).block();
            case "marketing" -> emailService.sendMarketingEmail(request).block();
            case "system" -> emailService.sendSystemEmail(request).block();
            case "batch" -> emailService.sendBatchEmail(request).block();
            default -> emailService.sendEmail(request).block();
        };
    }

    private EmailRequest buildRequest(EmailSendDTO dto, EmailKeyVO key) {
        EmailRequest request = new EmailRequest()
                .setFromEmail(key.getEmailAddress())
                .setFromName(key.getSenderName())
                .setSmtpHost(key.getSmtpHost())
                .setSmtpPort(key.getSmtpPort())
                .setUsername(key.getEmailAddress())
                .setPassword(key.getEmailPassword())
                .setEnableSsl(key.getEnableSsl())
                .setEnableTls(key.getEnableTls())
                .setEnableAuth(key.getEnableAuth())
                .setConnectionTimeout(key.getConnectionTimeout())
                .setReadTimeout(key.getReadTimeout());
        if (dto == null) {
            return request;
        }
        if (StringUtils.hasText(dto.getToEmail())) {
            request.setToEmail(dto.getToEmail());
        }
        if (!CollectionUtils.isEmpty(dto.getToEmails())) {
            request.setToEmails(dto.getToEmails());
        }
        if (!CollectionUtils.isEmpty(dto.getCcEmails())) {
            request.setCcEmails(dto.getCcEmails());
        }
        if (!CollectionUtils.isEmpty(dto.getBccEmails())) {
            request.setBccEmails(dto.getBccEmails());
        }
        if (StringUtils.hasText(dto.getSubject())) {
            request.setSubject(dto.getSubject());
        }
        if (StringUtils.hasText(dto.getTextContent())) {
            request.setTextContent(dto.getTextContent());
        }
        if (StringUtils.hasText(dto.getHtmlContent())) {
            request.setHtmlContent(dto.getHtmlContent());
        }
        if (StringUtils.hasText(dto.getTemplateId())) {
            request.setTemplateId(dto.getTemplateId());
        }
        if (dto.getTemplateParams() != null) {
            request.setTemplateParams(dto.getTemplateParams());
        }
        if (StringUtils.hasText(dto.getFromName())) {
            request.setFromName(dto.getFromName());
        }
        if (StringUtils.hasText(dto.getEmailType())) {
            request.setEmailType(dto.getEmailType());
        }
        if (dto.getPriority() != null) {
            request.setPriority(dto.getPriority());
        }
        if (dto.getNeedReceipt() != null) {
            request.setNeedReceipt(dto.getNeedReceipt());
        }
        if (StringUtils.hasText(dto.getReplyTo())) {
            request.setReplyTo(dto.getReplyTo());
        }
        if (StringUtils.hasText(dto.getCharset())) {
            request.setCharset(dto.getCharset());
        }
        if (dto.getCustomHeaders() != null) {
            request.setCustomHeaders(dto.getCustomHeaders());
        }
        if (dto.getAsync() != null) {
            request.setAsync(dto.getAsync());
        }
        if (!CollectionUtils.isEmpty(dto.getAttachments())) {
            request.setAttachments(buildAttachments(dto.getAttachments()));
        }
        return request;
    }

    private List<EmailRequest.EmailAttachment> buildAttachments(List<EmailAttachmentDTO> attachments) {
        return attachments.stream().map(attachment -> new EmailRequest.EmailAttachment()
                .setFileName(attachment.getFileName())
                .setFilePath(attachment.getFilePath())
                .setMimeType(attachment.getMimeType())
                .setFileSize(attachment.getFileSize())
                .setInline(attachment.getInline())
                .setContentId(attachment.getContentId())).collect(Collectors.toList());
    }

    private EmailKeyVO resolveKey(Long keyId, String provider) {
        EmailKeyVO key;
        if (keyId != null) {
            key = emailKeyService.getEmailKeyById(keyId);
        } else if (StringUtils.hasText(provider)) {
            key = emailKeyService.getAvailableKeyByProvider(provider);
        } else {
            throw new IceeBootException("缺少邮件密钥或厂商信息");
        }
        if (key == null) {
            throw new IceeBootException("邮件密钥不存在或不可用");
        }
        return key;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
