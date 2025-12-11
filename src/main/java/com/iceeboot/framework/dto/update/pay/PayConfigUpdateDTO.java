/**
 * 支付配置更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.dto.update.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付配置更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@Schema(description = "支付配置更新数据传输对象")
public class PayConfigUpdateDTO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @Schema(description = "支付金额（单位：分）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于0")
    private Integer amount;

    @Schema(description = "配置描述")
    private String description;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "排序")
    private Integer sort;
}

