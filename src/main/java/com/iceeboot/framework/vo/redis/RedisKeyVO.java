/**
 * Redis键VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.vo.redis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Redis键VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class RedisKeyVO {

    @Schema(description = "键名")
    private String key;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "值")
    private Object value;

    @Schema(description = "生存时间")
    private Long ttl;

    @Schema(description = "大小")
    private String size;
}