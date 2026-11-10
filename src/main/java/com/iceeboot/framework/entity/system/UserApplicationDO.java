package com.iceeboot.framework.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_user_application")
public class UserApplicationDO extends IceeBootDO {

    @TableField("user_id")
    private Long userId;

    @TableField("application_code")
    private String applicationCode;

    @TableField("status")
    private String status;

    @TableField("first_login_time")
    private LocalDateTime firstLoginTime;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
}
