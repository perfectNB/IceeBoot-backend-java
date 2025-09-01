package com.zboot.framework.controller.redis;

import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.query.redis.RedisQuery;
import com.zboot.framework.service.redis.RedisService;
import com.zboot.framework.vo.redis.RedisKeyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis管理控制器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "Redis管理", description = "Redis管理相关接口")
@RestController
@RequestMapping("/v1/redis")
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;

    /**
     * 获取Redis键列表
     */
    @Operation(summary = "获取Redis键列表", description = "分页查询Redis键列表")
    @GetMapping("/keys")
    public R<PageResult<RedisKeyVO>> getRedisKeys(RedisQuery query) {
        PageResult<RedisKeyVO> result = redisService.getRedisKeys(query);
        return R.ok(result);
    }

    /**
     * 获取Redis键详情
     */
    @Operation(summary = "获取Redis键详情", description = "根据键名获取Redis键详情")
    @GetMapping("/keys/{key}")
    public R<RedisKeyVO> getRedisKeyDetail(@PathVariable String key) {
        RedisKeyVO result = redisService.getRedisKeyDetail(key);
        return R.ok(result);
    }

    /**
     * 批量删除Redis键
     */
    @Operation(summary = "批量删除Redis键", description = "根据键名数组批量删除Redis键")
    @DeleteMapping("/keys")
    public R<Void> deleteRedisKeys(@RequestBody List<String> keys) {
        redisService.deleteRedisKeys(keys);
        return R.ok();
    }
}