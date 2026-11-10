package com.iceeboot.framework.entity.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notification_read")
public class NotificationReadDO extends IceeBootDO {

    @TableField("notification_id")
    private Long notificationId;

    @TableField("user_id")
    private Long userId;

    @TableField("read_time")
    private LocalDateTime readTime;
}
