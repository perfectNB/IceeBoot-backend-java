package com.zboot.framework.controller.log;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.log.LogBatchDeleteDTO;
import com.zboot.framework.dto.log.LogClearDTO;
import com.zboot.framework.dto.log.LogQueryDTO;
import com.zboot.framework.service.log.LogService;
import com.zboot.framework.vo.log.LogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 日志管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "日志管理", description = "日志管理相关接口")
@RestController
@RequestMapping("/v1/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * 获取日志列表
     */
    @Operation(summary = "获取日志列表", description = "分页查询日志列表")
    @GetMapping("/list")
    public R<PageResult<LogVO>> getLogList(LogQueryDTO queryDTO) {
        PageResult<LogVO> result = logService.getLogList(queryDTO);
        return R.ok(result);
    }

    /**
     * 删除日志
     */
    @Operation(summary = "删除日志", description = "根据ID删除日志")
    @DeleteMapping("/{id}")
    public R<Void> deleteLog(@PathVariable String id) {
        logService.deleteLog(id);
        return R.ok();
    }

    /**
     * 批量删除日志
     */
    @Operation(summary = "批量删除日志", description = "根据ID列表批量删除日志")
    @DeleteMapping("/batch")
    public R<Void> batchDeleteLog(@RequestBody LogBatchDeleteDTO deleteDTO) {
        logService.batchDeleteLogs(deleteDTO);
        return R.ok();
    }

    /**
     * 清空日志
     */
    @Operation(summary = "清空日志", description = "根据条件清空日志")
    @DeleteMapping("/clear")
    public R<Void> clearLog(@RequestBody LogClearDTO clearDTO) {
        logService.clearLogs(clearDTO);
        return R.ok();
    }
}