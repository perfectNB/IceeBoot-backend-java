package com.iceeboot.framework.controller.oss;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.oss.OssBucketDTO;
import com.iceeboot.framework.dto.create.oss.OssFileUploadDTO;
import com.iceeboot.framework.dto.update.oss.OssBucketUpdateDTO;
import com.iceeboot.framework.query.oss.OssBucketQuery;
import com.iceeboot.framework.query.oss.OssFileQuery;
import com.iceeboot.framework.service.oss.IOssBucketService;
import com.iceeboot.framework.service.oss.IOssFileService;
import com.iceeboot.framework.vo.oss.OssBucketVO;
import com.iceeboot.framework.vo.oss.OssFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OSS文件管理控制器
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Tag(name = "OSS文件管理", description = "OSS文件和存储桶管理相关接口")
@RestController
@RequestMapping("/v1/oss-files")
@RequiredArgsConstructor
public class OssFileController {

    private final IOssBucketService ossBucketService;
    private final IOssFileService ossFileService;

    // ==================== 文件管理接口 ====================

    @Operation(summary = "分页查询文件列表")
    @GetMapping()
    @RequirePermission(dynamic = true)
    public R<IPage<OssFileVO>> getFilePage(OssFileQuery query) {
        IPage<OssFileVO> result = ossFileService.getFilePage(query);
        return R.ok(result);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteFile(@RequestBody List<Long> ids) {
        Boolean result = ossFileService.deleteFile(ids);
        return result ? R.ok() : R.fail(ResponseMessage.OSS_DELETE_FAIL);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    @RequirePermission(dynamic = true)
    public R<OssFileVO> uploadFile(@Validated @ModelAttribute OssFileUploadDTO dto) {
        OssFileVO result = ossFileService.uploadFile(dto);
        return R.ok(result);
    }

    @Operation(summary = "获取文件下载链接")
    @GetMapping("/download/{id}")
    @RequirePermission(dynamic = true)
    public R<String> getDownloadUrl(
            @Parameter(description = "文件ID") @PathVariable("id") Long id) {
        try {
            String downloadUrl = ossFileService.generateDownloadUrl(id, 3600L);
            return R.ok(downloadUrl);
        } catch (Exception e) {
            return R.fail("获取下载链接失败: " + e.getMessage());
        }
    }

    @Operation(summary = "直接下载文件（兼容旧版本）")
    @GetMapping("/download/direct/{id}")
    @RequirePermission(dynamic = true)
    public ResponseEntity<Resource> downloadFileDirect(
            @Parameter(description = "文件ID") @PathVariable("id") Long id) {
        try {
            OssFileVO fileInfo = ossFileService.getFileById(id);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = ossFileService.downloadFile(id);
            
            // 处理文件名编码，同时支持ASCII和UTF-8格式
            String originalName = fileInfo.getOriginalName();
            if (originalName == null || originalName.isEmpty()) {
                originalName = fileInfo.getFileName();
            }
            // 去除可能导致问题的字符
            String safeName = originalName.replaceAll("[\\\\\\//\\r\\n\"*?:<>|]", "_");

            // 手动构建Content-Disposition头，确保filename部分只包含ASCII字符
            String asciiName = safeName.replaceAll("[^\\x00-\\x7F]", "_"); // 将非ASCII字符替换为下划线
            String encodedName = URLEncoder.encode(safeName, StandardCharsets.UTF_8);
            String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                asciiName,
                encodedName
            );
            
            // 根据文件类型设置Content-Type
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            if (fileInfo.getFileType() != null && !fileInfo.getFileType().isEmpty()) {
                try {
                    contentType = MediaType.parseMediaType(fileInfo.getFileType());
                } catch (Exception e) {
                    // 如果解析失败，使用默认的APPLICATION_OCTET_STREAM
                }
            }
            
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


    // ==================== 存储桶管理接口 ====================

    @Operation(summary = "分页查询存储桶列表")
    @GetMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<IPage<OssBucketVO>> getBucketPage(OssBucketQuery query) {
        IPage<OssBucketVO> result = ossBucketService.getBucketPage(query);
        return R.ok(result);
    }


    @Operation(summary = "新增存储桶")
    @PostMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<Void> addBucket(@Validated @RequestBody OssBucketDTO dto) {
        Boolean result = ossBucketService.addBucket(dto);
        return result ? R.ok() : R.fail(ResponseMessage.OSS_CREATE_FAIL);
    }

    @Operation(summary = "更新存储桶")
    @PutMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<Void> updateBucket(@Validated @RequestBody OssBucketUpdateDTO dto) {
        Boolean result = ossBucketService.updateBucket(dto);
        return result ? R.ok() : R.fail(ResponseMessage.OSS_UPDATE_FAIL);
    }

    @Operation(summary = "删除存储桶")
    @DeleteMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<Void> deleteBucket(@RequestBody List<Long> ids) {
        Boolean result = ossBucketService.deleteBucket(ids);
        return result ? R.ok() : R.fail(ResponseMessage.OSS_DELETE_FAIL);
    }

}