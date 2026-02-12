package com.iceeboot.framework.dto.update.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码数据传输对象
 *
 * @author CodeIcee
 */
@Data
@Schema(description = "重置密码数据传输对象")
public class ResetPasswordDTO {

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
