/**
 * 用户分布VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分布VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class UserDistributionVO {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private Integer value;

    @Schema(description = "百分比")
    private Double percentage;
}