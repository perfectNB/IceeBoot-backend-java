/**
 * 访问趋势VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 访问趋势VO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class VisitTrendVO {

    @Schema(description = "日期")
    private String date;

    @Schema(description = "访问量")
    private Integer visits;

    @Schema(description = "用户数")
    private Integer users;
}