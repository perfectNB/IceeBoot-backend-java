package com.zboot.framework.controller.oss;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.oss.OssBucketDTO;
import com.zboot.framework.dto.create.oss.OssFileUploadDTO;
import com.zboot.framework.dto.update.oss.OssBucketUpdateDTO;
import com.zboot.framework.query.oss.OssBucketQuery;
import com.zboot.framework.query.oss.OssFileQuery;
import com.zboot.framework.service.oss.IOssBucketService;
import com.zboot.framework.service.oss.IOssFileService;
import com.zboot.framework.vo.oss.OssBucketVO;
import com.zboot.framework.vo.oss.OssFileVO;
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

    // ==================== 存储桶管理接口 ====================

    @Operation(summary = "分页查询存储桶列表")
    @GetMapping("/buckets/page")
    @RequirePermission(dynamic = true)
    public R<IPage<OssBucketVO>> getBucketPage(OssBucketQuery query) {
        IPage<OssBucketVO> result = ossBucketService.getBucketPage(query);
        return R.ok(result);
    }

    @Operation(summary = "根据ID获取存储桶详情")
    @GetMapping("/buckets/{id}")
    @RequirePermission(dynamic = true)
    public R<OssBucketVO> getBucketById(
            @Parameter(description = "桶ID") @PathVariable("id") Long id) {
        OssBucketVO result = ossBucketService.getBucketById(id);
        return R.ok(result);
    }

    @Operation(summary = "新增存储桶")
    @PostMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<Void> addBucket(@Validated @RequestBody OssBucketDTO dto) {
        Boolean result = ossBucketService.addBucket(dto);
        return result ? R.ok() : R.fail("新增失败");
    }

    @Operation(summary = "更新存储桶")
    @PutMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<Void> updateBucket(@Validated @RequestBody OssBucketUpdateDTO dto) {
        Boolean result = ossBucketService.updateBucket(dto);
        return result ? R.ok() : R.fail("更新失败");
    }

    @Operation(summary = "删除存储桶")
    @DeleteMapping("/buckets")
    @RequirePermission(dynamic = true)
    public R<Void> deleteBucket(@RequestBody List<Long> ids) {
        Boolean result = ossBucketService.deleteBucket(ids);
        return result ? R.ok() : R.fail("删除失败");
    }

    // ==================== 文件管理接口 ====================

    @Operation(summary = "分页查询文件列表")
    @GetMapping("/page")
    @RequirePermission(dynamic = true)
    public R<IPage<OssFileVO>> getFilePage(OssFileQuery query) {
        IPage<OssFileVO> result = ossFileService.getFilePage(query);
        return R.ok(result);
    }

    @Operation(summary = "根据ID获取文件详情")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<OssFileVO> getFileById(
            @Parameter(description = "文件ID") @PathVariable("id") Long id) {
        OssFileVO result = ossFileService.getFileById(id);
        return R.ok(result);
    }

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    @RequirePermission(dynamic = true)
    public R<OssFileVO> uploadFile(@Validated @ModelAttribute OssFileUploadDTO dto) {
        OssFileVO result = ossFileService.uploadFile(dto);
        return R.ok(result);
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download/{id}")
    @RequirePermission(dynamic = true)
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件ID") @PathVariable("id") Long id) {
        try {
            Resource resource = ossFileService.downloadFile(id);
            OssFileVO fileInfo = ossFileService.getFileById(id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileInfo.getOriginalName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteFile(@RequestBody List<Long> ids) {
        Boolean result = ossFileService.deleteFile(ids);
        return result ? R.ok() : R.fail("删除失败");
    }
}