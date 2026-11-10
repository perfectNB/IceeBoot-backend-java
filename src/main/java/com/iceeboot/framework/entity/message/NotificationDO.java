package com.iceeboot.framework.entity.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notification")
public class NotificationDO extends IceeBootDO {

    @TableField("application_code")
    private String applicationCode;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("type")
    private String type;

    @TableField(value = "target_user_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long targetUserId;

    @TableField("status")
    private String status;

    @TableField(value = "publish_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime publishTime;

    @TableField(value = "expire_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime expireTime;
}
