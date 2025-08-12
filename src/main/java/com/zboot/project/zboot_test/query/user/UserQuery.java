package com.zboot.project.zboot_test.query.user;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * User查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "User查询对象")
public class UserQuery extends BaseQuery {

    // TODO: 添加具体查询字段
    // 示例字段：
    // @Schema(description = "名称")
    // private String name;
    
    // @Schema(description = "状态")
    // private String status;
    
    // @Schema(description = "开始时间")
    // private LocalDateTime startTime;
    
    // @Schema(description = "结束时间")
    // private LocalDateTime endTime;
}