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

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "所属项目编码")
    @TableField("application_code")
    private String applicationCode;

    @Schema(description = "支付业务类型")
    @TableField("business_type")
    private String businessType;

    @Schema(description = "业务到账状态")
    @TableField("business_status")
    private String businessStatus;

    @Schema(description = "业务补偿重试次数")
    @TableField("business_retry_count")
    private Integer businessRetryCount;

    @Schema(description = "最近一次业务处理错误")
    @TableField("business_error")
    private String businessError;

    @Schema(description = "订单名称")
    @TableField("order_name")
    private String orderName;

    @Schema(description = "订单描述")
    @TableField("description")
    private String description;

    @Schema(description = "支付方式（1-微信，2-支付宝，3-网银）")
    @TableField("pay_type")
    private Integer payType;

    @Schema(description = "支付配置ID")
    @TableField("config_id")
    private Long configId;

    @Schema(description = "支付金额")
    @TableField(value = "amount", numericScale = "4")
    private java.math.BigDecimal amount;

    @Schema(description = "实际到账金额")
    @TableField(value = "actual_amount", numericScale = "4")
    private java.math.BigDecimal actualAmount;

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
