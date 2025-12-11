/**
 * 支付配置查询对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.query.pay;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付配置查询对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付配置查询对象")
public class PayConfigQuery extends BaseQuery {

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "支付策略码")
    private String payCode;

    @Schema(description = "最小金额（单位：分）")
    private Integer minAmount;

    @Schema(description = "最大金额（单位：分）")
    private Integer maxAmount;
}

