/**
 * 支付记录实体类
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
 * 支付记录实体类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_record")
@Schema(description = "支付记录")
@Accessors(chain = true)
public class PayRecordDO extends IceeBootDO {

    @Schema(description = "商户订单号（系统生成）")
    @TableField("out_trade_no")
    private String outTradeNo;

    @Schema(description = "第三方订单号（支付平台返回）")
    @TableField("trade_no")
    private String tradeNo;

    @Schema(description = "订单名称")
    @TableField("order_name")
    private String orderName;

    @Schema(description = "支付方式（1-微信，2-支付宝，3-网银）")
    @TableField("pay_type")
    private Integer payType;

    @Schema(description = "支付配置ID")
    @TableField("config_id")
    private Long configId;

    @Schema(description = "支付金额（单位：分）")
    @TableField("pay_amount")
    private Integer payAmount;

    @Schema(description = "支付状态（1-支付中，2-支付成功，3-支付失败，4-退款）")
    @TableField("pay_status")
    private Integer payStatus;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}

