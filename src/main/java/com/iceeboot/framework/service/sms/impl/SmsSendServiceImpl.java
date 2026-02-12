package com.iceeboot.framework.service.sms.impl;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.sms.dto.SMSRequest;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.common.service.sms.factory.SMSFactory;
import com.iceeboot.common.service.sms.service.SMSService;
import com.iceeboot.framework.query.sms.SmsBalanceQuery;
import com.iceeboot.framework.dto.create.sms.SmsSendDTO;
import com.iceeboot.framework.query.sms.SmsStatusQuery;
import com.iceeboot.framework.service.sms.ISmsKeyService;
import com.iceeboot.framework.service.sms.ISmsSendService;
import com.iceeboot.framework.vo.sms.SmsKeyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SmsSendServiceImpl implements ISmsSendService {
    private final ISmsKeyService smsKeyService;
    private final SMSFactory smsFactory;

    @Override
    public SMSResponse sendSms(SmsSendDTO dto) {
        SmsKeyVO key = resolveKey(dto.getKeyId(), dto.getProvider());
        SMSService smsService = smsFactory.getService(key.getSmsProvider());
        SMSRequest request = buildRequest(dto, key);
        SMSResponse response = dispatchSend(smsService, dto, request);
        if (response == null) {
            throw new IceeBootException("短信发送失败");
        }
        return response;
    }

    @Override
    public SMSResponse queryStatus(SmsStatusQuery dto) {
        if (!StringUtils.hasText(dto.getMessageId())) {
            throw new IceeBootException("消息ID不能为空");
        }
        SmsKeyVO key = resolveKey(dto.getKeyId(), dto.getProvider());
        SMSService smsService = smsFactory.getService(key.getSmsProvider());
        SMSRequest request = buildRequest(null, key);
        SMSResponse response = smsService.querySMSStatus(dto.getMessageId(), request).block();
        if (response == null) {
            throw new IceeBootException("短信状态查询失败");
        }
        return response;
    }

    @Override
    public SMSResponse queryBalance(SmsBalanceQuery dto) {
        SmsKeyVO key = resolveKey(dto.getKeyId(), dto.getProvider());
        SMSService smsService = smsFactory.getService(key.getSmsProvider());
        SMSRequest request = buildRequest(null, key);
        SMSResponse response = smsService.queryBalance(request).block();
        if (response == null) {
            throw new IceeBootException("短信余额查询失败");
        }
        return response;
    }

    private SMSResponse dispatchSend(SMSService smsService, SmsSendDTO dto, SMSRequest request) {
        String sendType = normalize(dto.getSendType());
        if (!StringUtils.hasText(sendType)) {
            if (StringUtils.hasText(dto.getTemplateId()) || !CollectionUtils.isEmpty(dto.getTemplateParams())) {
                sendType = "template";
            } else if (!CollectionUtils.isEmpty(dto.getPhoneNumbers())) {
                sendType = "batch";
            } else if (StringUtils.hasText(dto.getSmsType())) {
                sendType = dto.getSmsType();
            } else {
                sendType = "single";
            }
        }

        return switch (sendType) {
            case "template" -> smsService.sendTemplateSMS(request).block();
            case "batch" -> smsService.sendBatchSMS(request).block();
            case "verification" -> smsService.sendVerificationCode(request).block();
            case "notification" -> smsService.sendNotification(request).block();
            case "marketing" -> smsService.sendMarketing(request).block();
            default -> smsService.sendSMS(request).block();
        };
    }

    private SMSRequest buildRequest(SmsSendDTO dto, SmsKeyVO key) {
        SMSRequest request = new SMSRequest()
                .setAccessKeyId(key.getAccessKeyId())
                .setAccessKeySecret(key.getAccessKeySecret())
                .setEndpoint(key.getEndpoint())
                .setRegion(key.getRegion())
                .setSignName(key.getDefaultSignName())
                .setTemplateId(key.getDefaultTemplateId());
        if (dto == null) {
            return request;
        }
        if (StringUtils.hasText(dto.getPhoneNumber())) {
            request.setPhoneNumber(dto.getPhoneNumber());
        }
        if (!CollectionUtils.isEmpty(dto.getPhoneNumbers())) {
            request.setPhoneNumbers(dto.getPhoneNumbers().toArray(new String[0]));
        }
        if (StringUtils.hasText(dto.getContent())) {
            request.setContent(dto.getContent());
        }
        if (StringUtils.hasText(dto.getTemplateId())) {
            request.setTemplateId(dto.getTemplateId());
        }
        if (dto.getTemplateParams() != null) {
            request.setTemplateParams(dto.getTemplateParams());
        }
        if (StringUtils.hasText(dto.getSignName())) {
            request.setSignName(dto.getSignName());
        }
        if (StringUtils.hasText(dto.getSmsType())) {
            request.setSmsType(dto.getSmsType());
        }
        if (StringUtils.hasText(dto.getCountryCode())) {
            request.setCountryCode(dto.getCountryCode());
        }
        if (dto.getScheduleTime() != null) {
            request.setScheduleTime(dto.getScheduleTime());
        }
        if (dto.getValidityPeriod() != null) {
            request.setValidityPeriod(dto.getValidityPeriod());
        }
        if (StringUtils.hasText(dto.getCallbackUrl())) {
            request.setCallbackUrl(dto.getCallbackUrl());
        }
        if (StringUtils.hasText(dto.getUserData())) {
            request.setUserData(dto.getUserData());
        }
        return request;
    }

    private SmsKeyVO resolveKey(Long keyId, String provider) {
        SmsKeyVO key;
        if (keyId != null) {
            key = smsKeyService.getSmsKeyById(keyId);
        } else if (StringUtils.hasText(provider)) {
            key = smsKeyService.getAvailableKeyByProvider(provider);
        } else {
            throw new IceeBootException("缺少短信密钥或厂商信息");
        }
        if (key == null) {
            throw new IceeBootException("短信密钥不存在或不可用");
        }
        return key;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
