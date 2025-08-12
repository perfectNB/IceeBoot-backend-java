package com.zboot.project.zboot_test.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * User数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@Schema(description = "User数据传输对象")
public class UserDTO {

    @Schema(description = "UserID")
    private Long id;

    // TODO: 添加具体字段
    // 示例字段：
    // @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    // @NotBlank(message = "名称不能为空")
    // private String name;
    
    // @Schema(description = "状态")
    // private String status;
}