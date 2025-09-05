package com.zboot.framework.controller.log;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;


import com.zboot.framework.query.log.LogQuery;
import com.zboot.framework.service.log.LogService;
import com.zboot.framework.vo.log.LogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日志管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "日志管理", description = "日志管理相关接口")
@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * 获取日志列表
     */
    @Operation(summary = "获取日志列表", description = "分页查询日志列表")
    @GetMapping()
    public R<PageResult<LogVO>> getLogList(LogQuery query) {
        PageResult<LogVO> result = logService.getLogList(query);
        return R.ok(result);
    }

    /**
     * 删除日志
     */
    @Operation(summary = "删除日志", description = "单个删除或批量删除日志")
    @DeleteMapping
    public R<Void> deleteLogs(@RequestBody List<String> ids) {
        boolean success = logService.deleteLogs(ids);
        return success ? R.ok("删除成功") : R.fail("删除失败");
    }


}