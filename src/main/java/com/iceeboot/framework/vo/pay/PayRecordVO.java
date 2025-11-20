/**
 * 支付记录视图对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.vo.pay;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付记录视图对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@Schema(description = "支付记录视图对象")
public class PayRecordVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "第三方订单号")
    private String tradeNo;

    @Schema(description = "订单名称")
    private String orderName;

    @Schema(description = "支付方式（1-微信，2-支付宝，3-网银）")
    private Integer payType;

    @Schema(description = "支付方式名称")
    private String payTypeName;

    @Schema(description = "支付配置ID")
    private Long configId;

    @Schema(description = "支付配置名称")
    private String configName;

    @Schema(description = "支付金额（单位：分）")
    private Integer payAmount;

    @Schema(description = "支付金额（元）")
    private String payAmountYuan;

    @Schema(description = "支付状态（1-支付中，2-支付成功，3-支付失败，4-退款）")
    private Integer payStatus;

    @Schema(description = "支付状态名称")
    private String payStatusName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建者")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

