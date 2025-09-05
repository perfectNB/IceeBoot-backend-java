/**
 * 创建Redis键DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.create.redis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建Redis键DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "创建Redis键数据传输对象")
public class CreateRedisKeyDTO {

    @Schema(description = "键名")
    @NotBlank(message = "键名不能为空")
    private String key;

    @Schema(description = "类型")
    @NotBlank(message = "类型不能为空")
    private String type;

    @Schema(description = "值")
    @NotNull(message = "值不能为空")
    private Object value;

    @Schema(description = "生存时间（秒）")
    private Long ttl;
}