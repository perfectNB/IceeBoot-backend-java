/**
 * 支付记录查询对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.query.pay;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 支付记录查询对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付记录查询对象")
public class PayRecordQuery extends BaseQuery {

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "支付方式（1-微信，2-支付宝，3-网银）")
    private Integer payType;

    @Schema(description = "支付状态（1-支付中，2-支付成功，3-支付失败，4-退款）")
    private Integer payStatus;

    @Schema(description = "支付配置ID")
    private Long configId;

    @Schema(description = "所属项目编码")
    private String applicationCode;

    @Schema(description = "支付业务类型")
    private String businessType;

    @Schema(description = "业务到账状态")
    private String businessStatus;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
