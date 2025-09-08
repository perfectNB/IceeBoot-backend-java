/*
 * @Description: Email密钥管理控制器
 * @Date: 2025-09-05
 * @Author: CodeIcee
 */
package com.zboot.framework.controller.email;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.R;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.create.email.EmailKeyDTO;
import com.zboot.framework.dto.update.email.EmailKeyUpdateDTO;
import com.zboot.framework.query.email.EmailKeyQuery;
import com.zboot.framework.service.email.IEmailKeyService;
import com.zboot.framework.vo.email.EmailKeyVO;
import com.zboot.framework.vo.email.EmailProviderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Email密钥管理控制器
 *
 * @author CodeIcee
 * @since 2025-09-05
 */
@Tag(name = "Email密钥管理", description = "Email密钥相关接口")
@RestController
@RequestMapping("/v1/email-keys")
@RequiredArgsConstructor
public class EmailKeyController {

    private final IEmailKeyService emailKeyService;

    @Operation(summary = "获取所有厂商的密钥列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<EmailProviderVO>> getAllProviderKeys() {
        List<EmailProviderVO> result = emailKeyService.getAllProviderKeys();
        return R.ok(result);
    }

   @Operation(summary = "分页查询Email密钥列表")
   @GetMapping("/page")
   @RequirePermission(dynamic = true)
   public R<PageResult<EmailKeyVO>> getEmailKeyPage(EmailKeyQuery query) {
       PageResult<EmailKeyVO> result = emailKeyService.getEmailKeyPage(query);
       return R.ok(result);
   }

    @Operation(summary = "根据ID获取Email密钥详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<EmailKeyVO> getEmailKeyById(
            @Parameter(description = "密钥ID") @PathVariable Long id) {
        EmailKeyVO result = emailKeyService.getEmailKeyById(id);
        return R.ok(result);
    }

    @Operation(summary = "新增Email密钥")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addEmailKey(@Validated @RequestBody EmailKeyDTO dto) {
        Boolean result = emailKeyService.addEmailKey(dto);
        return result ? R.ok() : R.fail("新增失败");
    }

    @Operation(summary = "更新Email密钥", description = "更新Email密钥信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateEmailKey(@Validated @RequestBody EmailKeyUpdateDTO dto) {
        Boolean result = emailKeyService.updateEmailKey(dto);
        return result ? R.ok() : R.fail("更新失败");
    }

    @Operation(summary = "删除Email密钥")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteEmailKey(@RequestBody List<Long> ids) {
        Boolean result = emailKeyService.deleteEmailKey(ids);
        return result ? R.ok() : R.fail("删除失败");
    }

    @Operation(summary = "更新Email密钥状态")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateEmailKeyStatus(
            @Parameter(description = "密钥ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Boolean result = emailKeyService.updateEmailKeyStatus(id, status);
        return result ? R.ok() : R.fail("状态更新失败");
    }

    @Operation(summary = "根据厂商获取激活的密钥列表")
    @GetMapping("/provider/{provider}")
    @RequirePermission(dynamic = true)
    public R<List<EmailKeyVO>> getActiveEmailKeysByProvider(
            @Parameter(description = "Email厂商") @PathVariable String provider) {
        List<EmailKeyVO> result = emailKeyService.getActiveEmailKeysByProvider(provider);
        return R.ok(result);
    }

    @Operation(summary = "获取所有支持的Email厂商列表")
    @GetMapping("/providers")
    @RequirePermission(dynamic = true)
    public R<List<String>> getSupportedProviders() {
        List<String> result = emailKeyService.getSupportedProviders();
        return R.ok(result);
    }

    @Operation(summary = "根据厂商获取可用的密钥")
    @GetMapping("/provider/{provider}/available")
    @RequirePermission(dynamic = true)
    public R<EmailKeyVO> getAvailableKeyByProvider(
            @Parameter(description = "Email厂商") @PathVariable String provider) {
        EmailKeyVO result = emailKeyService.getAvailableKeyByProvider(provider);
        return R.ok(result);
    }

    @Operation(summary = "验证密钥是否有效")
    @PostMapping("/{id}/validate")
    @RequirePermission(dynamic = true)
    public R<Boolean> validateEmailKey(
            @Parameter(description = "密钥ID") @PathVariable Long id) {
        Boolean result = emailKeyService.validateEmailKey(id);
        return R.ok(result);
    }

    @Operation(summary = "批量导入密钥")
    @PostMapping("/batch-import")
    @RequirePermission(dynamic = true)
    public R<Integer> batchImportKeys(@RequestBody List<EmailKeyDTO> keys) {
        Integer result = emailKeyService.batchImportKeys(keys);
        return R.ok(result);
    }

    @Operation(summary = "获取密钥使用统计")
    @GetMapping("/stats")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getKeyUsageStats(
            @Parameter(description = "Email厂商（可选）") @RequestParam(required = false) String provider) {
        Map<String, Object> result = emailKeyService.getKeyUsageStats(provider);
        return R.ok(result);
    }

    @Operation(summary = "获取厂商配置信息")
    @GetMapping("/provider/{provider}/config")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getProviderConfig(
            @Parameter(description = "Email厂商") @PathVariable String provider) {
        Map<String, Object> config = Map.of(
            "provider", provider,
            "displayName", getProviderDisplayName(provider),
            "description", getProviderDescription(provider),
            "authType", getProviderAuthType(provider),
            "defaultSmtpHost", getProviderDefaultSmtpHost(provider),
            "defaultSmtpPort", getProviderDefaultSmtpPort(provider),
            "supportedEmailTypes", getSupportedEmailTypes(provider),
            "supportedFeatures", getSupportedFeatures(provider)
        );
        return R.ok(config);
    }

    /**
     * 获取厂商显示名称
     */
    private String getProviderDisplayName(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq" -> "QQ邮箱";
            case "netease" -> "网易邮箱";
            case "gmail" -> "Gmail";
            case "outlook" -> "Outlook";
            case "aliyun" -> "阿里云邮箱";
            case "tencent" -> "腾讯企业邮箱";
            default -> provider;
        };
    }

    /**
     * 获取厂商描述
     */
    private String getProviderDescription(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq" -> "腾讯QQ邮箱服务";
            case "netease" -> "网易163/126邮箱服务";
            case "gmail" -> "Google Gmail邮箱服务";
            case "outlook" -> "微软Outlook邮箱服务";
            case "aliyun" -> "阿里云邮箱推送服务";
            case "tencent" -> "腾讯企业邮箱服务";
            default -> "邮箱服务";
        };
    }

    /**
     * 获取厂商认证类型
     */
    private String getProviderAuthType(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq", "netease" -> "邮箱地址 + 授权码";
            case "gmail" -> "邮箱地址 + 应用专用密码";
            case "outlook" -> "邮箱地址 + 密码";
            case "aliyun" -> "Access Key + Secret Key";
            case "tencent" -> "企业邮箱地址 + 密码";
            default -> "邮箱地址 + 密码/授权码";
        };
    }

    /**
     * 获取厂商默认SMTP主机
     */
    private String getProviderDefaultSmtpHost(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq" -> "smtp.qq.com";
            case "netease" -> "smtp.163.com";
            case "gmail" -> "smtp.gmail.com";
            case "outlook" -> "smtp-mail.outlook.com";
            case "aliyun" -> "smtpdm.aliyun.com";
            case "tencent" -> "smtp.exmail.qq.com";
            default -> "";
        };
    }

    /**
     * 获取厂商默认SMTP端口
     */
    private Integer getProviderDefaultSmtpPort(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq", "netease", "gmail", "outlook", "tencent" -> 587;
            case "aliyun" -> 25;
            default -> 587;
        };
    }

    /**
     * 获取支持的邮件类型
     */
    private List<String> getSupportedEmailTypes(String provider) {
        return List.of("纯文本邮件", "HTML邮件", "模板邮件", "通知邮件", "营销邮件", "系统邮件");
    }

    /**
     * 获取支持的功能
     */
    private List<String> getSupportedFeatures(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq", "netease", "gmail", "outlook", "tencent" -> 
                List.of("单发邮件", "批量发送", "模板邮件", "附件支持", "HTML格式", "纯文本格式", "抄送", "密送", "优先级设置", "回复地址", "自定义邮件头");
            case "aliyun" -> 
                List.of("单发邮件", "批量发送", "模板邮件", "HTML格式", "纯文本格式", "状态查询");
            default -> 
                List.of("单发邮件", "纯文本格式");
        };
    }
}