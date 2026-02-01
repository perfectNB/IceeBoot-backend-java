package com.iceeboot.framework.controller.sms;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.framework.query.sms.SmsBalanceQuery;
import com.iceeboot.framework.dto.create.sms.SmsSendDTO;
import com.iceeboot.framework.query.sms.SmsStatusQuery;
import com.iceeboot.framework.service.sms.ISmsSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SMS发送管理", description = "短信发送与状态查询接口")
@RestController
@RequestMapping("/v1/sms")
@RequiredArgsConstructor
public class SmsController {
    private final ISmsSendService smsSendService;

    @Operation(summary = "发送短信")
    @PostMapping("/send")
    @RequirePermission(dynamic = true)
    public R<SMSResponse> send(@Validated @RequestBody SmsSendDTO dto) {
        SMSResponse result = smsSendService.sendSms(dto);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return R.ok(result);
        }
        return R.fail(result.getErrorMessage());
    }

    @Operation(summary = "查询短信状态")
    @PostMapping("/status")
    @RequirePermission(dynamic = true)
    public R<SMSResponse> queryStatus(@Validated @RequestBody SmsStatusQuery dto) {
        SMSResponse result = smsSendService.queryStatus(dto);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return R.ok(result);
        }
        return R.fail(result.getErrorMessage());
    }

    @Operation(summary = "查询短信余额")
    @PostMapping("/balance")
    @RequirePermission(dynamic = true)
    public R<SMSResponse> queryBalance(@Validated @RequestBody SmsBalanceQuery dto) {
        SMSResponse result = smsSendService.queryBalance(dto);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return R.ok(result);
        }
        return R.fail(result.getErrorMessage());
    }
}
