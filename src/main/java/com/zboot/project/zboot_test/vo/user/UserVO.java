package com.zboot.project.zboot_test.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@Schema(description = "User视图对象")
public class UserVO {

    @Schema(description = "UserID")
    private Long id;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // TODO: 添加具体字段
    // 示例字段：
    // @Schema(description = "名称")
    // private String name;
    
    // @Schema(description = "状态")
    // private String status;
}