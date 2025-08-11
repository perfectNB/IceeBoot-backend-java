/**
 * 权限查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.query.system;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 权限查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysPermissionQuery extends BaseQuery {

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限编码")
    private String code;

    @Schema(description = "权限类型")
    private String type;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}