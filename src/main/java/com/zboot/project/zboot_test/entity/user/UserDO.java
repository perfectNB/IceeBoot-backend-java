package com.zboot.project.zboot_test.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User实体类
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
@Schema(description = "User实体")
public class UserDO extends ZbootDO {

    // TODO: 添加具体字段
    // 示例字段：
    // @Schema(description = "名称")
    // @TableField("name")
    // private String name;
    
    // @Schema(description = "状态")
    // @TableField("status")
    // private String status;
}