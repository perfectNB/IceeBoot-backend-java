package com.zboot.framework.service.oss.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import org.springframework.util.StringUtils;
import com.zboot.framework.dto.create.oss.OssBucketDTO;
import com.zboot.framework.dto.update.oss.OssBucketUpdateDTO;
import com.zboot.framework.entity.oss.OssBucketDO;
import com.zboot.framework.mapper.oss.OssBucketMapper;
import com.zboot.framework.query.oss.OssBucketQuery;
import com.zboot.framework.service.oss.IOssBucketService;
import com.zboot.framework.mapper.oss.OssFileMapper;
import com.zboot.framework.vo.oss.OssBucketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OSS存储桶服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssBucketServiceImpl extends ServiceImpl<OssBucketMapper, OssBucketDO> implements IOssBucketService {

    private final OssFileMapper ossFileMapper;

    @Override
    public IPage<OssBucketVO> getBucketPage(OssBucketQuery query) {
        Page<OssBucketDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OssBucketDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(OssBucketDO::getBucketName, query.getKeyword())
                   .or()
                   .like(OssBucketDO::getDescription, query.getKeyword());
        }
        if (StringUtils.hasText(query.getBucketName())) {
            wrapper.like(OssBucketDO::getBucketName, query.getBucketName());
        }
        if (StringUtils.hasText(query.getProvider())) {
            wrapper.eq(OssBucketDO::getProvider, query.getProvider());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(OssBucketDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(OssBucketDO::getCreateTime);
        
        IPage<OssBucketDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<OssBucketVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<OssBucketVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public OssBucketVO getBucketById(Long id) {
        OssBucketDO bucket = this.getById(id);
        return bucket != null ? convertToVO(bucket) : null;
    }

    @Override
    public Boolean addBucket(OssBucketDTO dto) {
        // 检查同供应商下是否有重名的桶
        OssBucketDO existingBucket = getBucketByProviderAndName(dto.getProvider(), dto.getBucketName());
        if (existingBucket != null) {
            throw new IllegalArgumentException("同供应商下已存在相同名称的桶: " + dto.getBucketName());
        }
        
        OssBucketDO bucket = new OssBucketDO();
        BeanUtils.copyProperties(dto, bucket);
        
        // 设置默认状态
        if (!StringUtils.hasText(bucket.getStatus())) {
            bucket.setStatus(SystemConstants.Status.ACTIVE);
        }
        
        return this.save(bucket);
    }

    @Override
    public Boolean updateBucket(OssBucketUpdateDTO dto) {
        // 检查桶是否存在文件
        OssBucketDO existingBucket = this.getById(dto.getId());
        if (existingBucket == null) {
            throw new IllegalArgumentException("桶不存在");
        }
        
        if (hasBucketFiles(existingBucket.getBucketName())) {
            throw new IllegalArgumentException("桶下存在文件，无法修改");
        }
        
        // 检查同供应商下是否有重名的桶（排除自己）
        OssBucketDO duplicateBucket = getBucketByProviderAndName(dto.getProvider(), dto.getBucketName());
        if (duplicateBucket != null && !duplicateBucket.getId().equals(dto.getId())) {
            throw new IllegalArgumentException("同供应商下已存在相同名称的桶: " + dto.getBucketName());
        }
        
        OssBucketDO bucket = new OssBucketDO();
        BeanUtils.copyProperties(dto, bucket);
        return this.updateById(bucket);
    }

    @Override
    public Boolean deleteBucket(List<Long> ids) {
        for (Long id : ids) {
            OssBucketDO bucket = this.getById(id);
            if (bucket != null && hasBucketFiles(bucket.getBucketName())) {
                throw new IllegalArgumentException("桶 " + bucket.getBucketName() + " 下存在文件，无法删除");
            }
        }
        return this.removeByIds(ids);
    }

    @Override
    public OssBucketDO getBucketByProviderAndName(String provider, String bucketName) {
        return baseMapper.selectByProviderAndBucketName(provider, bucketName);
    }

    @Override
    public Boolean hasBucketFiles(String bucketName) {
        Long fileCount = ossFileMapper.countByBucketName(bucketName);
        return fileCount != null && fileCount > 0;
    }

    /**
     * 转换为VO对象
     */
    private OssBucketVO convertToVO(OssBucketDO bucket) {
        OssBucketVO vo = new OssBucketVO();
        BeanUtils.copyProperties(bucket, vo);
        
        // 设置文件数量
        Long fileCount = ossFileMapper.countByBucketName(bucket.getBucketName());
        vo.setFileCount(fileCount != null ? fileCount : 0L);
        
        return vo;
    }
}