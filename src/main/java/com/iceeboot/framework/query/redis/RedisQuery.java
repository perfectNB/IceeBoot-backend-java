/**
 * Redis查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.query.redis;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Redis查询DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "redis查询对象")
public class RedisQuery extends BaseQuery {

    @Schema(description = "键名筛选")
    private String key;

    @Schema(description = "类型筛选")
    private String type;
}