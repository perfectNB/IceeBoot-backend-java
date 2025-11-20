/**
 * 支付配置实体类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.entity.pay;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 支付配置实体类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_payment_config")
@Schema(description = "支付配置")
@Accessors(chain = true)
public class PaymentConfigDO extends IceeBootDO {

    @Schema(description = "配置名称")
    @TableField("config_name")
    private String configName;

    @Schema(description = "支付金额（单位：分）")
    @TableField("amount")
    private Integer amount;

    @Schema(description = "配置描述")
    @TableField("description")
    private String description;

    @Schema(description = "状态（active-启用，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;
}

