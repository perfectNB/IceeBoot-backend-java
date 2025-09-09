package com.zboot.framework.service.oss.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.oss.dto.OSSRequest;
import com.zboot.common.service.oss.dto.OSSResponse;
import com.zboot.common.service.oss.factory.OSSFactory;
import com.zboot.common.service.oss.service.OSSService;
import com.zboot.framework.dto.create.oss.OssFileUploadDTO;
import com.zboot.framework.entity.oss.OssBucketDO;
import com.zboot.framework.entity.oss.OssFileDO;
import com.zboot.framework.mapper.oss.OssFileMapper;
import com.zboot.framework.query.oss.OssFileQuery;
import com.zboot.framework.service.oss.IOssBucketService;
import com.zboot.framework.service.oss.IOssFileService;
import com.zboot.framework.service.oss.IOssKeyService;
import com.zboot.framework.vo.oss.OssFileVO;
import com.zboot.framework.vo.oss.OssKeyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OSS文件服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssFileServiceImpl extends ServiceImpl<OssFileMapper, OssFileDO> implements IOssFileService {

    private final IOssBucketService ossBucketService;
    private final IOssKeyService ossKeyService;

    @Override
    public IPage<OssFileVO> getFilePage(OssFileQuery query) {
        Page<OssFileDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OssFileDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(OssFileDO::getFileName, query.getKeyword())
                   .or()
                   .like(OssFileDO::getOriginalName, query.getKeyword());
        }
        if (StringUtils.hasText(query.getFileName())) {
            wrapper.like(OssFileDO::getFileName, query.getFileName());
        }
        if (StringUtils.hasText(query.getOriginalName())) {
            wrapper.like(OssFileDO::getOriginalName, query.getOriginalName());
        }
        if (StringUtils.hasText(query.getFileType())) {
            wrapper.eq(OssFileDO::getFileType, query.getFileType());
        }
        if (StringUtils.hasText(query.getProvider())) {
            wrapper.eq(OssFileDO::getProvider, query.getProvider());
        }
        if (StringUtils.hasText(query.getBucketName())) {
            wrapper.eq(OssFileDO::getBucketName, query.getBucketName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(OssFileDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(OssFileDO::getCreateTime);
        
        IPage<OssFileDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<OssFileVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<OssFileVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public OssFileVO getFileById(Long id) {
        OssFileDO file = this.getById(id);
        return file != null ? convertToVO(file) : null;
    }

    @Override
    public OssFileVO uploadFile(OssFileUploadDTO dto) {
        try {
            MultipartFile file = dto.getFile();
            
            // 获取密钥信息
            OssKeyVO ossKey = ossKeyService.getOssKeyById(dto.getKeyId());
            if (ossKey == null) {
                throw new IllegalArgumentException("密钥不存在");
            }
            
            // 验证供应商是否匹配
            if (!dto.getProvider().equals(ossKey.getOssProvider())) {
                throw new IllegalArgumentException("供应商与密钥不匹配");
            }
            
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateFileName(dto.getPathPrefix(), fileExtension);
            
            // 计算文件MD5
            String fileMd5 = calculateMD5(file.getBytes());
            
            // 检查是否已存在相同MD5的文件
            OssFileDO existingFile = baseMapper.selectByFileMd5(fileMd5);
            if (existingFile != null) {
                log.info("文件已存在，返回已有文件信息: {}", existingFile.getFileName());
                return convertToVO(existingFile);
            }
            
            // 获取OSS服务
            OSSService ossService = OSSFactory.getService(dto.getProvider());
            
            // 构建OSS请求
            OSSRequest ossRequest = new OSSRequest()
                    .setAccessKeyId(ossKey.getAccessKeyId())
                    .setAccessKeySecret(ossKey.getAccessKeySecret())
                    .setBucketName(dto.getBucketName())
                    .setObjectKey(fileName)
                    .setContent(file.getBytes())
                    .setContentType(file.getContentType());
            
            // 上传文件
            OSSResponse ossResponse = ossService.uploadFile(ossRequest).block();
            if (!Boolean.TRUE.equals(ossResponse.getSuccess())) {
                throw new RuntimeException("文件上传失败: " + ossResponse.getErrorMessage());
            }
            
            // 保存文件信息到数据库
            OssFileDO ossFile = new OssFileDO();
            ossFile.setFileName(fileName);
            ossFile.setOriginalName(originalFilename);
            ossFile.setFileSize(file.getSize());
            ossFile.setFileUrl(ossResponse.getFileUrl());
            ossFile.setFileType(file.getContentType());
            ossFile.setProvider(dto.getProvider());
            ossFile.setBucketName(dto.getBucketName());
            ossFile.setFileMd5(fileMd5);
            ossFile.setStatus(SystemConstants.Status.ACTIVE);
            
            this.save(ossFile);
            
            return convertToVO(ossFile);
            
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Resource downloadFile(Long id) {
        OssFileDO file = this.getById(id);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        
        // 获取桶信息
        OssBucketDO bucket = ossBucketService.getBucketByProviderAndName(file.getProvider(), file.getBucketName());
        if (bucket == null) {
            throw new IllegalArgumentException("桶不存在");
        }
        
        // 获取密钥信息
        OssKeyVO ossKey = ossKeyService.getOssKeyById(bucket.getKeyId());
        if (ossKey == null) {
            throw new IllegalArgumentException("密钥不存在");
        }
        
        // 获取OSS服务
        OSSService ossService = OSSFactory.getService(file.getProvider());
        
        // 构建OSS请求
        OSSRequest ossRequest = new OSSRequest()
                .setAccessKeyId(ossKey.getAccessKeyId())
                .setAccessKeySecret(ossKey.getAccessKeySecret())
                .setBucketName(file.getBucketName())
                .setObjectKey(file.getFileName());
        
        // 下载文件
        OSSResponse ossResponse = ossService.downloadFile(ossRequest).block();
        if (!Boolean.TRUE.equals(ossResponse.getSuccess())) {
            throw new RuntimeException("文件下载失败: " + ossResponse.getErrorMessage());
        }
        
        return new ByteArrayResource(ossResponse.getContent());
    }

    @Override
    public Boolean deleteFile(List<Long> ids) {
        for (Long id : ids) {
            OssFileDO file = this.getById(id);
            if (file == null) {
                continue;
            }
            
            try {
                // 获取桶信息
                OssBucketDO bucket = ossBucketService.getBucketByProviderAndName(file.getProvider(), file.getBucketName());
                if (bucket != null) {
                    // 获取密钥信息
                    OssKeyVO ossKey = ossKeyService.getOssKeyById(bucket.getKeyId());
                    if (ossKey != null) {
                        // 获取OSS服务
                        OSSService ossService = OSSFactory.getService(file.getProvider());
                        
                        // 构建OSS请求
                        OSSRequest ossRequest = new OSSRequest()
                                .setAccessKeyId(ossKey.getAccessKeyId())
                                .setAccessKeySecret(ossKey.getAccessKeySecret())
                                .setBucketName(file.getBucketName())
                                .setObjectKey(file.getFileName());
                        
                        // 删除OSS文件
                        OSSResponse ossResponse = ossService.deleteFile(ossRequest).block();
                        if (!Boolean.TRUE.equals(ossResponse.getSuccess())) {
                            log.warn("OSS文件删除失败: {}", ossResponse.getErrorMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("删除OSS文件异常: {}", e.getMessage(), e);
            }
        }
        
        // 删除数据库记录
        return this.removeByIds(ids);
    }

    @Override
    public Long countFilesByBucketName(String bucketName) {
        return baseMapper.countByBucketName(bucketName);
    }

    @Override
    public OssFileDO getFileByNameAndBucket(String fileName, String bucketName) {
        return baseMapper.selectByFileNameAndBucketName(fileName, bucketName);
    }

    /**
     * 转换为VO对象
     */
    private OssFileVO convertToVO(OssFileDO file) {
        OssFileVO vo = new OssFileVO();
        BeanUtils.copyProperties(file, vo);
        
        // 格式化文件大小
        vo.setFileSizeFormatted(formatFileSize(file.getFileSize()));
        
        return vo;
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String pathPrefix, String fileExtension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        StringBuilder fileName = new StringBuilder();
        if (StringUtils.hasText(pathPrefix)) {
            fileName.append(pathPrefix);
            if (!pathPrefix.endsWith("/")) {
                fileName.append("/");
            }
        }
        fileName.append(timestamp).append("/").append(uuid);
        if (StringUtils.hasText(fileExtension)) {
            fileName.append(".").append(fileExtension);
        }
        
        return fileName.toString();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * 计算文件MD5
     */
    private String calculateMD5(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long fileSize) {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}