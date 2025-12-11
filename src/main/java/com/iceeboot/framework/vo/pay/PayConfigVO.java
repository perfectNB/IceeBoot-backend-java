/**
 * 支付配置视图对象
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
 * 支付配置视图对象
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Data
@Schema(description = "支付配置视图对象")
public class PayConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "支付金额（单位：分）")
    private Integer amount;

    @Schema(description = "支付金额（元）")
    private String amountYuan;

    @Schema(description = "配置描述")
    private String description;

    @Schema(description = "支付策略代")
    private String payCode;

    @Schema(description = "状态（active-启用，inactive-禁用）")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建者")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

