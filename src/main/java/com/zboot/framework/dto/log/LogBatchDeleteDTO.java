/**
 * 日志批量删除DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 日志批量删除DTO类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LogBatchDeleteDTO {

    @Schema(description = "日志ID列表")
    private List<String> ids;
}