/*
 * @Description: SMS密钥管理控制器
 * @Date: 2025-09-05
 * @Author: CodeIcee
 */
package com.zboot.framework.controller.sms;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.R;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.create.sms.SmsKeyDTO;
import com.zboot.framework.dto.update.sms.SmsKeyUpdateDTO;
import com.zboot.framework.query.sms.SmsKeyQuery;
import com.zboot.framework.service.sms.ISmsKeyService;
import com.zboot.framework.vo.sms.SmsKeyVO;
import com.zboot.framework.vo.sms.SmsProviderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SMS密钥管理控制器
 *
 * @author CodeIcee
 * @since 2025-09-05
 */
@Tag(name = "SMS密钥管理", description = "SMS密钥相关接口")
@RestController
@RequestMapping("/v1/sms-keys")
@RequiredArgsConstructor
public class SmsKeyController {

    private final ISmsKeyService smsKeyService;

    @Operation(summary = "获取所有厂商的密钥列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<SmsProviderVO>> getAllProviderKeys() {
        List<SmsProviderVO> result = smsKeyService.getAllProviderKeys();
        return R.ok(result);
    }

   @Operation(summary = "分页查询SMS密钥列表")
   @GetMapping("/page")
   @RequirePermission(dynamic = true)
   public R<PageResult<SmsKeyVO>> getSmsKeyPage(SmsKeyQuery query) {
       PageResult<SmsKeyVO> result = smsKeyService.getSmsKeyPage(query);
       return R.ok(result);
   }

    @Operation(summary = "根据ID获取SMS密钥详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<SmsKeyVO> getSmsKeyById(
            @Parameter(description = "密钥ID") @PathVariable Long id) {
        SmsKeyVO result = smsKeyService.getSmsKeyById(id);
        return R.ok(result);
    }

    @Operation(summary = "新增SMS密钥")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addSmsKey(@Validated @RequestBody SmsKeyDTO dto) {
        Boolean result = smsKeyService.addSmsKey(dto);
        return result ? R.ok() : R.fail("新增失败");
    }

    @Operation(summary = "更新SMS密钥", description = "更新SMS密钥信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateSmsKey(@Validated @RequestBody SmsKeyUpdateDTO dto) {
        Boolean result = smsKeyService.updateSmsKey(dto);
        return result ? R.ok() : R.fail("更新失败");
    }

    @Operation(summary = "删除SMS密钥")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteSmsKey(@RequestBody List<Long> ids) {
        Boolean result = smsKeyService.deleteSmsKey(ids);
        return result ? R.ok() : R.fail("删除失败");
    }

    @Operation(summary = "更新SMS密钥状态")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateSmsKeyStatus(
            @Parameter(description = "密钥ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Boolean result = smsKeyService.updateSmsKeyStatus(id, status);
        return result ? R.ok() : R.fail("状态更新失败");
    }

    @Operation(summary = "根据厂商获取激活的密钥列表")
    @GetMapping("/provider/{provider}")
    @RequirePermission(dynamic = true)
    public R<List<SmsKeyVO>> getActiveSmsKeysByProvider(
            @Parameter(description = "SMS厂商") @PathVariable String provider) {
        List<SmsKeyVO> result = smsKeyService.getActiveSmsKeysByProvider(provider);
        return R.ok(result);
    }

    @Operation(summary = "获取所有支持的SMS厂商列表")
    @GetMapping("/providers")
    @RequirePermission(dynamic = true)
    public R<List<String>> getSupportedProviders() {
        List<String> result = smsKeyService.getSupportedProviders();
        return R.ok(result);
    }

    @Operation(summary = "根据厂商获取可用的密钥")
    @GetMapping("/provider/{provider}/available")
    @RequirePermission(dynamic = true)
    public R<SmsKeyVO> getAvailableKeyByProvider(
            @Parameter(description = "SMS厂商") @PathVariable String provider) {
        SmsKeyVO result = smsKeyService.getAvailableKeyByProvider(provider);
        return R.ok(result);
    }

    @Operation(summary = "验证密钥是否有效")
    @PostMapping("/{id}/validate")
    @RequirePermission(dynamic = true)
    public R<Boolean> validateSmsKey(
            @Parameter(description = "密钥ID") @PathVariable Long id) {
        Boolean result = smsKeyService.validateSmsKey(id);
        return R.ok(result);
    }

    @Operation(summary = "批量导入密钥")
    @PostMapping("/batch-import")
    @RequirePermission(dynamic = true)
    public R<Integer> batchImportKeys(@RequestBody List<SmsKeyDTO> keys) {
        Integer result = smsKeyService.batchImportKeys(keys);
        return R.ok(result);
    }

    @Operation(summary = "获取密钥使用统计")
    @GetMapping("/stats")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getKeyUsageStats(
            @Parameter(description = "SMS厂商（可选）") @RequestParam(required = false) String provider) {
        Map<String, Object> result = smsKeyService.getKeyUsageStats(provider);
        return R.ok(result);
    }

    @Operation(summary = "获取厂商配置信息")
    @GetMapping("/provider/{provider}/config")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getProviderConfig(
            @Parameter(description = "SMS厂商") @PathVariable String provider) {
        Map<String, Object> config = Map.of(
            "provider", provider,
            "displayName", getProviderDisplayName(provider),
            "description", getProviderDescription(provider),
            "authType", getProviderAuthType(provider),
            "defaultEndpoint", getProviderDefaultEndpoint(provider),
            "supportedSmsTypes", getSupportedSmsTypes(provider),
            "supportedFeatures", getSupportedFeatures(provider)
        );
        return R.ok(config);
    }

    /**
     * 获取厂商显示名称
     */
    private String getProviderDisplayName(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun" -> "阿里云短信";
            case "tencent" -> "腾讯云短信";
            case "huawei" -> "华为云短信";
            case "netease" -> "网易云信";
            case "ronglian" -> "容联云通讯";
            case "qiniu" -> "七牛云短信";
            default -> provider;
        };
    }

    /**
     * 获取厂商描述
     */
    private String getProviderDescription(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun" -> "阿里云短信服务";
            case "tencent" -> "腾讯云短信服务";
            case "huawei" -> "华为云短信服务";
            case "netease" -> "网易云信短信服务";
            case "ronglian" -> "容联云通讯短信服务";
            case "qiniu" -> "七牛云短信服务";
            default -> "短信服务";
        };
    }

    /**
     * 获取厂商认证类型
     */
    private String getProviderAuthType(String provider) {
        return switch (provider.toLowerCase()) {
            case "huawei" -> "App Key + App Secret 认证";
            case "ronglian" -> "Account SID + Auth Token 认证";
            default -> "Access Key ID + Access Key Secret 认证";
        };
    }

    /**
     * 获取厂商默认端点
     */
    private String getProviderDefaultEndpoint(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun" -> "https://dysmsapi.aliyuncs.com";
            case "tencent" -> "https://sms.tencentcloudapi.com";
            case "huawei" -> "https://smsapi.cn-north-4.myhuaweicloud.com:443";
            case "netease" -> "https://api.netease.im";
            case "ronglian" -> "https://app.cloopen.com:8883";
            case "qiniu" -> "https://sms.qiniuapi.com";
            default -> "";
        };
    }

    /**
     * 获取支持的短信类型
     */
    private List<String> getSupportedSmsTypes(String provider) {
        return List.of("验证码短信", "通知短信", "营销短信");
    }

    /**
     * 获取支持的功能
     */
    private List<String> getSupportedFeatures(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun", "tencent", "huawei" -> 
                List.of("单发短信", "批量发送", "模板短信", "状态查询", "余额查询", "验证码短信", "通知短信", "营销短信");
            case "netease", "ronglian", "qiniu" -> 
                List.of("单发短信", "批量发送", "模板短信", "状态查询", "验证码短信", "通知短信");
            default -> 
                List.of("单发短信", "模板短信", "验证码短信", "通知短信");
        };
    }
}