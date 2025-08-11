/**
 * Redis查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.redis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Redis查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class RedisQueryDTO {

    @Schema(description = "页码")
    private Integer page = 1;

    @Schema(description = "每页数量")
    private Integer size = 10;

    @Schema(description = "键名筛选")
    private String key;

    @Schema(description = "类型筛选")
    private String type;
}