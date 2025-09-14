/**
 * 菜单查询DTO
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.dto.create.system;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 菜单查询DTO
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuQueryDTO extends BaseQuery {

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型")
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