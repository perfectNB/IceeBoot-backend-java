package com.iceeboot.framework.controller.redis;

import com.iceeboot.common.annotation.RateLimit;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.RateLimitType;
import com.iceeboot.framework.dto.create.redis.CreateRedisKeyDTO;
import com.iceeboot.framework.query.redis.RedisQuery;
import com.iceeboot.framework.service.redis.RedisService;
import com.iceeboot.framework.vo.redis.RedisKeyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
@Slf4j
public class RedisController {

    private final RedisService redisService;

    @Operation(summary = "获取Redis键列表", description = "分页查询Redis键列表")
    @GetMapping("/keys")
    @RequirePermission(dynamic = true)
    @RateLimit(count = 1, time = 60, limitType = RateLimitType.USER)
    public R<PageResult<RedisKeyVO>> getRedisKeys(RedisQuery query) {
        log.info("获取Redis键列表");
        PageResult<RedisKeyVO> result = redisService.getRedisKeys(query);
        return R.ok(result);
    }


    @Operation(summary = "获取Redis键详情", description = "根据键名获取Redis键详情")
    @GetMapping("/keys/{key}")
    @RequirePermission(dynamic = true)
    public R<RedisKeyVO> getRedisKeyDetail(@PathVariable("key") String key) {
        RedisKeyVO result = redisService.getRedisKeyDetail(key);
        return R.ok(result);
    }


    @Operation(summary = "创建Redis键", description = "创建新的Redis键值对")
    @PostMapping("/keys")
    @RequirePermission(dynamic = true)
    public R<Void> createRedisKey(@Validated @RequestBody CreateRedisKeyDTO createDTO) {

        redisService.createRedisKey(createDTO);
        return R.ok();
    }


    @Operation(summary = "批量删除Redis键", description = "根据键名数组批量删除Redis键")
    @DeleteMapping("/keys")
    @RequirePermission(dynamic = true)
    public R<Void> deleteRedisKeys(@RequestBody List<String> keys) {
        redisService.deleteRedisKeys(keys);
        return R.ok();
    }
}