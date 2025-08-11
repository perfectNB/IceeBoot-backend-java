/**
 * Redis键实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.redis;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Redis键实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_redis_key")
public class RedisDO extends ZbootDO {

    @Schema(description = "键名")
    @TableField("key_name")
    private String key;

    @Schema(description = "类型")
    @TableField("type")
    private String type;

    @Schema(description = "值")
    @TableField("value")
    private String value;

    @Schema(description = "生存时间")
    @TableField("ttl")
    private Long ttl;

    @Schema(description = "大小")
    @TableField("size")
    private String size;
}