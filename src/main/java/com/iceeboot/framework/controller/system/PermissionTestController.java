/**
 * 权限测试控制器
 * 演示如何使用权限注解
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.controller.system;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 权限测试控制器
 * 演示权限注解的使用方法
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Tag(name = "权限测试", description = "权限注解使用示例")
@RestController
@RequestMapping("/v1/permission-test")
public class PermissionTestController {

    /**
     * 无权限要求的接口
     */
    @Operation(summary = "无权限要求", description = "任何人都可以访问")
    @GetMapping("/public")
    @RequirePermission(dynamic = true)
    public R<String> publicEndpoint() {
        return R.ok("这是一个公开接口，无需权限验证");
    }

    /**
     * 需要单个权限的接口
     */
    @Operation(summary = "需要用户查看权限", description = "需要user:view权限")
    @RequirePermission(dynamic = true)
    @GetMapping("/user-view")
    public R<String> userViewEndpoint() {
        return R.ok("您有用户查看权限，可以访问此接口");
    }

    /**
     * 需要多个权限之一的接口（OR关系）
     */
    @Operation(summary = "需要用户管理权限", description = "需要user:add或user:edit权限之一")
    @RequirePermission(dynamic = true)
    @PostMapping("/user-manage")
    public R<String> userManageEndpoint() {
        return R.ok("您有用户管理权限，可以访问此接口");
    }

    /**
     * 需要多个权限都满足的接口（AND关系）
     */
    @Operation(summary = "需要完整用户权限", description = "需要user:view和user:edit权限")
    @RequirePermission(dynamic = true)
    @PutMapping("/user-full")
    public R<String> userFullEndpoint() {
        return R.ok("您有完整的用户权限，可以访问此接口");
    }

    /**
     * 需要管理员权限的接口
     */
    @Operation(summary = "需要管理员权限", description = "需要admin权限")
    @RequirePermission(dynamic = true)
    @DeleteMapping("/admin-only")
    public R<String> adminOnlyEndpoint() {
        return R.ok("您有管理员权限，可以访问此接口");
    }

    /**
     * 动态权限查询示例
     */
    @Operation(summary = "动态权限验证", description = "从Redis中动态获取权限要求")
    @RequirePermission(dynamic = true, description = "动态权限验证")
    @GetMapping("/dynamic")
    public R<String> dynamicPermissionEndpoint() {
        return R.ok("通过动态权限验证，可以访问此接口");
    }

    /**
     * 仅使用注解标记，无具体权限要求（动态查询）
     */
    @Operation(summary = "纯动态权限", description = "仅标记需要权限验证，具体权限从Redis获取")
    @RequirePermission(dynamic = true)
    @PostMapping("/pure-dynamic")
    public R<String> pureDynamicEndpoint() {
        return R.ok("通过纯动态权限验证");
    }

    /**
     * 类级别权限注解示例
     */
    @Tag(name = "系统管理", description = "需要系统管理权限的接口组")
    @RestController
    @RequestMapping("/v1/system-admin")
    @RequirePermission(dynamic = true)
    public static class SystemAdminController {

        @Operation(summary = "系统配置", description = "查看系统配置")
        @GetMapping("/config")
        @RequirePermission(dynamic = true)
        public R<String> getSystemConfig() {
            return R.ok("系统配置信息");
        }

        @Operation(summary = "系统监控", description = "查看系统监控")
        @GetMapping("/monitor")
        @RequirePermission(dynamic = true)
        public R<String> getSystemMonitor() {
            return R.ok("系统监控信息");
        }

        /**
         * 方法级别的权限注解会覆盖类级别的注解
         */
        @Operation(summary = "超级管理", description = "需要超级管理员权限")
        @RequirePermission(dynamic = true)
        @PostMapping("/super")
        public R<String> superAdminEndpoint() {
            return R.ok("超级管理员操作");
        }
    }
}