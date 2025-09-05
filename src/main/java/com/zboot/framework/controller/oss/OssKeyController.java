/*
 * @Description: OSS密钥管理控制器
 * @Date: 2025-09-05
 * @Author: CodeIcee
 */
package com.zboot.framework.controller.oss;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.oss.OssKeyDTO;
import com.zboot.framework.dto.update.oss.OssKeyUpdateDTO;
import com.zboot.framework.query.oss.OssKeyQuery;
import com.zboot.framework.service.oss.IOssKeyService;
import com.zboot.framework.vo.oss.OssKeyVO;
import com.zboot.framework.vo.oss.OssProviderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OSS密钥管理控制器
 *
 * @author CodeIcee
 * @since 2025-09-05
 */
@Tag(name = "OSS密钥管理", description = "OSS密钥相关接口")
@RestController
@RequestMapping("/v1/oss-keys")
@RequiredArgsConstructor
public class OssKeyController {

    private final IOssKeyService ossKeyService;

    @Operation(summary = "获取所有厂商的密钥列表")
    @GetMapping
    @RequirePermission(dynamic = true)
    public R<List<OssProviderVO>> getAllProviderKeys() {
        List<OssProviderVO> result = ossKeyService.getAllProviderKeys();
        return R.ok(result);
    }

    @Operation(summary = "分页查询OSS密钥列表")
    @GetMapping("/page")
    @RequirePermission(dynamic = true)
    public R<IPage<OssKeyVO>> getOssKeyPage(OssKeyQuery query) {
        IPage<OssKeyVO> result = ossKeyService.getOssKeyPage(query);
        return R.ok(result);
    }

    @Operation(summary = "根据ID获取OSS密钥详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<OssKeyVO> getOssKeyById(
            @Parameter(description = "密钥ID") @PathVariable Long id) {
        OssKeyVO result = ossKeyService.getOssKeyById(id);
        return R.ok(result);
    }

    @Operation(summary = "新增OSS密钥")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addOssKey(@Validated @RequestBody OssKeyDTO dto) {
        Boolean result = ossKeyService.addOssKey(dto);
        return result ? R.ok() : R.fail("新增失败");
    }

    @Operation(summary = "更新OSS密钥", description = "更新OSS密钥信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateOssKey(@Validated @RequestBody OssKeyUpdateDTO dto) {
        Boolean result = ossKeyService.updateOssKey(dto);
        return result ? R.ok() : R.fail("更新失败");
    }

    @Operation(summary = "删除OSS密钥")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteOssKey(@RequestBody List<Long> ids) {
        Boolean result = ossKeyService.deleteOssKey(ids);
        return result ? R.ok() : R.fail("删除失败");
    }

    @Operation(summary = "更新OSS密钥状态")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateOssKeyStatus(
            @Parameter(description = "密钥ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Boolean result = ossKeyService.updateOssKeyStatus(id, status);
        return result ? R.ok() : R.fail("状态更新失败");
    }

    @Operation(summary = "根据厂商获取激活的密钥列表")
    @GetMapping("/provider/{provider}")
    @RequirePermission(dynamic = true)
    public R<List<OssKeyVO>> getActiveOssKeysByProvider(
            @Parameter(description = "OSS厂商") @PathVariable String provider) {
        List<OssKeyVO> result = ossKeyService.getActiveOssKeysByProvider(provider);
        return R.ok(result);
    }

    @Operation(summary = "获取所有支持的OSS厂商列表")
    @GetMapping("/providers")
    @RequirePermission(dynamic = true)
    public R<List<String>> getSupportedProviders() {
        List<String> result = ossKeyService.getSupportedProviders();
        return R.ok(result);
    }

    @Operation(summary = "根据厂商获取可用的密钥")
    @GetMapping("/provider/{provider}/available")
    @RequirePermission(dynamic = true)
    public R<OssKeyVO> getAvailableKeyByProvider(
            @Parameter(description = "OSS厂商") @PathVariable String provider) {
        OssKeyVO result = ossKeyService.getAvailableKeyByProvider(provider);
        return R.ok(result);
    }

    @Operation(summary = "验证密钥是否有效")
    @PostMapping("/{id}/validate")
    @RequirePermission(dynamic = true)
    public R<Boolean> validateOssKey(
            @Parameter(description = "密钥ID") @PathVariable Long id) {
        Boolean result = ossKeyService.validateOssKey(id);
        return R.ok(result);
    }

    @Operation(summary = "批量导入密钥")
    @PostMapping("/batch-import")
    @RequirePermission(dynamic = true)
    public R<Integer> batchImportKeys(@RequestBody List<OssKeyDTO> keys) {
        Integer result = ossKeyService.batchImportKeys(keys);
        return R.ok(result);
    }

    @Operation(summary = "获取密钥使用统计")
    @GetMapping("/stats")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getKeyUsageStats(
            @Parameter(description = "OSS厂商（可选）") @RequestParam(required = false) String provider) {
        Map<String, Object> result = ossKeyService.getKeyUsageStats(provider);
        return R.ok(result);
    }

    @Operation(summary = "获取厂商配置信息")
    @GetMapping("/provider/{provider}/config")
    @RequirePermission(dynamic = true)
    public R<Map<String, Object>> getProviderConfig(
            @Parameter(description = "OSS厂商") @PathVariable String provider) {
        Map<String, Object> config = Map.of(
            "provider", provider,
            "displayName", getProviderDisplayName(provider),
            "description", getProviderDescription(provider),
            "authType", getProviderAuthType(provider),
            "defaultEndpoint", getProviderDefaultEndpoint(provider),
            "supportedFeatures", getSupportedFeatures(provider)
        );
        return R.ok(config);
    }

    /**
     * 获取厂商显示名称
     */
    private String getProviderDisplayName(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun" -> "阿里云 OSS";
            case "huawei" -> "华为云 OBS";
            case "tencent" -> "腾讯云 COS";
            case "qiniu" -> "七牛云 Kodo";
            case "minio" -> "MinIO";
            default -> provider;
        };
    }

    /**
     * 获取厂商描述
     */
    private String getProviderDescription(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun" -> "阿里云对象存储服务";
            case "huawei" -> "华为云对象存储服务";
            case "tencent" -> "腾讯云对象存储服务";
            case "qiniu" -> "七牛云对象存储服务";
            case "minio" -> "开源对象存储服务";
            default -> "对象存储服务";
        };
    }

    /**
     * 获取厂商认证类型
     */
    private String getProviderAuthType(String provider) {
        return switch (provider.toLowerCase()) {
            case "tencent" -> "SecretId + SecretKey 认证";
            default -> "AccessKeyId + AccessKeySecret 认证";
        };
    }

    /**
     * 获取厂商默认端点
     */
    private String getProviderDefaultEndpoint(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun" -> "oss-cn-hangzhou.aliyuncs.com";
            case "huawei" -> "obs.cn-north-4.myhuaweicloud.com";
            case "tencent" -> "cos.ap-beijing.myqcloud.com";
            case "qiniu" -> "upload.qiniup.com";
            case "minio" -> "localhost:9000";
            default -> "";
        };
    }

    /**
     * 获取支持的功能
     */
    private List<String> getSupportedFeatures(String provider) {
        return switch (provider.toLowerCase()) {
            case "aliyun", "huawei", "tencent", "minio" -> 
                List.of("上传", "下载", "删除", "列表", "容量查询", "文件存在检查", "复制", "移动", "预签名URL", "存储桶管理");
            case "qiniu" -> 
                List.of("上传", "下载", "删除", "列表", "容量查询", "文件存在检查", "复制", "移动", "预签名URL");
            default -> 
                List.of("上传", "下载", "删除", "列表", "容量查询", "文件存在检查");
        };
    }
}