package com.iceeboot.framework.dto.create.system;

import jakarta.validation.constraints.NotBlank;

/** 微信临时凭证通过请求体兑换，避免进入请求地址日志。 */
public record WechatTicketDTO(@NotBlank(message = "ticket不能为空") String ticket) {
}
