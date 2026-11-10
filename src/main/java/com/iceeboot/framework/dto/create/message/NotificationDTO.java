package com.iceeboot.framework.dto.create.message;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {

    private Long id;

    @NotBlank(message = "项目编码不能为空")
    private String applicationCode;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "消息类型不能为空")
    private String type;

    private Long targetUserId;
    private LocalDateTime expireTime;
}
