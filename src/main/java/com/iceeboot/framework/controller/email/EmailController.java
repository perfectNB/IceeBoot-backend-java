package com.iceeboot.framework.controller.email;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.service.email.dto.EmailResponse;
import com.iceeboot.framework.dto.create.email.EmailSendDTO;
import com.iceeboot.framework.dto.create.email.EmailStatusQueryDTO;
import com.iceeboot.framework.service.email.IEmailSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Email发送管理", description = "邮件发送与状态查询接口")
@RestController
@RequestMapping("/v1/emails")
@RequiredArgsConstructor
public class EmailController {
    private final IEmailSendService emailSendService;

    @Operation(summary = "发送邮件")
    @PostMapping("/send")
    @RequirePermission(dynamic = true)
    public R<EmailResponse> send(@Validated @RequestBody EmailSendDTO dto) {
        EmailResponse result = emailSendService.sendEmail(dto);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return R.ok(result);
        }
        return R.fail(result.getErrorMessage());
    }

    @Operation(summary = "查询邮件状态")
    @PostMapping("/status")
    @RequirePermission(dynamic = true)
    public R<EmailResponse> queryStatus(@Validated @RequestBody EmailStatusQueryDTO dto) {
        EmailResponse result = emailSendService.queryStatus(dto);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return R.ok(result);
        }
        return R.fail(result.getErrorMessage());
    }
}
