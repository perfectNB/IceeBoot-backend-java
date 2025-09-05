package com.zboot.framework.query.email;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Email密钥查询对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Email密钥查询对象")
public class EmailKeyQuery extends BaseQuery {

    @Schema(description = "Email厂商")
    private String emailProvider;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}