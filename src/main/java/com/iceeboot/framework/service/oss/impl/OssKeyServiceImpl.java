package com.iceeboot.framework.service.oss.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.factory.OSSFactory;
import com.iceeboot.common.service.oss.service.OSSService;
import com.iceeboot.framework.dto.create.oss.OssKeyDTO;
import com.iceeboot.framework.dto.update.oss.OssKeyUpdateDTO;
import com.iceeboot.framework.entity.oss.OssKeyDO;
import com.iceeboot.framework.mapper.oss.OssKeyMapper;
import com.iceeboot.framework.query.oss.OssKeyQuery;
import com.iceeboot.framework.service.oss.IOssKeyService;
import com.iceeboot.framework.vo.oss.OssKeyVO;
import com.iceeboot.framework.vo.oss.OssProviderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * OSS密钥服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssKeyServiceImpl extends ServiceImpl<OssKeyMapper, OssKeyDO> implements IOssKeyService {

    private final OSSFactory ossFactory;

    // 用于轮询获取密钥的计数器
    private final Map<String, AtomicInteger> providerCounters = new ConcurrentHashMap<>();

    @Override
    public PageResult<OssKeyVO> getOssKeyPage(OssKeyQuery query) {
        Page<OssKeyDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OssKeyDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(OssKeyDO::getRemark, query.getKeyword())
                   .or()
                   .like(OssKeyDO::getOssProvider, query.getKeyword());
        }
        if (StringUtils.hasText(query.getOssProvider())) {
            wrapper.eq(OssKeyDO::getOssProvider, query.getOssProvider());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(OssKeyDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(OssKeyDO::getCreateTime);
        
        IPage<OssKeyDO> result = this.page(page, wrapper);
        
        // 转换为VO
        List<OssKeyVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public OssKeyVO getOssKeyById(Long id) {
        OssKeyDO ossKey = this.getById(id);
        return ossKey != null ? convertToVO(ossKey) : null;
    }

    @Override
    public Boolean addOssKey(OssKeyDTO dto) {
        // 验证厂商是否支持
        if (!getSupportedProviders().contains(dto.getOssProvider())) {
            throw new IceeBootException("不支持的OSS厂商: " + dto.getOssProvider());
        }
        
        OssKeyDO ossKey = new OssKeyDO();
        BeanUtils.copyProperties(dto, ossKey);
        
        // 设置默认状态
        if (!StringUtils.hasText(ossKey.getStatus())) {
            ossKey.setStatus(SystemConstants.Status.ACTIVE);
        }
        
        // 设置默认端点
        if (!StringUtils.hasText(ossKey.getEndpoint())) {
            ossKey.setEndpoint(ossFactory.getDefaultEndpoint(dto.getOssProvider()));
        }
        
        return this.save(ossKey);
    }

    @Override
    public Boolean updateOssKey(OssKeyUpdateDTO dto) {
        OssKeyDO ossKey = new OssKeyDO();
        BeanUtils.copyProperties(dto, ossKey);
        return this.updateById(ossKey);
    }

    @Override
    public Boolean deleteOssKey(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Boolean updateOssKeyStatus(Long id, String status) {
        OssKeyDO ossKey = new OssKeyDO();
        ossKey.setId(id);
        ossKey.setStatus(status);
        return this.updateById(ossKey);
    }

    @Override
    public List<OssKeyVO> getActiveOssKeysByProvider(String ossProvider) {
        LambdaQueryWrapper<OssKeyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OssKeyDO::getOssProvider, ossProvider)
               .eq(OssKeyDO::getStatus, SystemConstants.Status.ACTIVE)
               .orderByDesc(OssKeyDO::getCreateTime);
        
        List<OssKeyDO> ossKeys = this.list(wrapper);
        return ossKeys.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedProviders() {
        // 从OSSFactory获取支持的厂商列表
        return new ArrayList<>(ossFactory.getSupportedProviders());
    }

    @Override
    public OssKeyVO getAvailableKeyByProvider(String ossProvider) {
        List<OssKeyVO> keys = getActiveOssKeysByProvider(ossProvider);
        if (keys.isEmpty()) {
            return null;
        }
        
        // 轮询获取密钥
        AtomicInteger counter = providerCounters.computeIfAbsent(ossProvider, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % keys.size();
        return keys.get(index);
    }

    @Override
    public Boolean validateOssKey(Long id) {
        OssKeyDO ossKey = this.getById(id);
        if (ossKey == null) {
            return false;
        }
        
        try {
            // 获取对应的OSS服务
            OSSService ossService = ossFactory.getService(ossKey.getOssProvider());
            if (ossService == null) {
                return false;
            }
            
            // 构建验证请求
            OSSRequest request = new OSSRequest()
                    .setAccessKeyId(ossKey.getAccessKeyId())
                    .setAccessKeySecret(ossKey.getAccessKeySecret())
                    .setEndpoint(ossKey.getEndpoint())
                    .setRegion(ossKey.getRegion());
            
            // 验证配置
            Boolean isValid = ossService.validateConfig(request).block();
            
            return Boolean.TRUE.equals(isValid);
        } catch (Exception e) {
            log.error("验证OSS密钥失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Integer batchImportKeys(List<OssKeyDTO> keys) {
        int successCount = 0;
        for (OssKeyDTO keyDTO : keys) {
            try {
                if (addOssKey(keyDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("导入OSS密钥失败: {}", e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Object> getKeyUsageStats(String ossProvider) {
        Map<String, Object> stats = new HashMap<>();
        
        LambdaQueryWrapper<OssKeyDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(ossProvider)) {
            wrapper.eq(OssKeyDO::getOssProvider, ossProvider);
        }
        
        // 总数统计
        long totalCount = this.count(wrapper);
        stats.put("totalCount", totalCount);
        
        // 激活状态统计
        wrapper.eq(OssKeyDO::getStatus, SystemConstants.Status.ACTIVE);
        long activeCount = this.count(wrapper);
        stats.put("activeCount", activeCount);
        
        // 禁用状态统计
        wrapper.clear();
        if (StringUtils.hasText(ossProvider)) {
            wrapper.eq(OssKeyDO::getOssProvider, ossProvider);
        }
        wrapper.eq(OssKeyDO::getStatus, SystemConstants.Status.INACTIVE);
        long inactiveCount = this.count(wrapper);
        stats.put("inactiveCount", inactiveCount);
        
        // 按厂商分组统计
        if (!StringUtils.hasText(ossProvider)) {
            Map<String, Long> providerStats = new HashMap<>();
            for (String provider : getSupportedProviders()) {
                LambdaQueryWrapper<OssKeyDO> providerWrapper = new LambdaQueryWrapper<>();
                providerWrapper.eq(OssKeyDO::getOssProvider, provider)
                              .eq(OssKeyDO::getStatus, SystemConstants.Status.ACTIVE);
                long count = this.count(providerWrapper);
                providerStats.put(provider, count);
            }
            stats.put("providerStats", providerStats);
        }
        
        return stats;
    }

    @Override
    public List<OssProviderVO> getAllProviderKeys() {
        List<OssProviderVO> result = new ArrayList<>();
        
        // 获取所有支持的厂商
        List<String> providers = getSupportedProviders();
        
        for (String provider : providers) {
            // 获取该厂商的所有密钥（包括激活和禁用的）
            LambdaQueryWrapper<OssKeyDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OssKeyDO::getOssProvider, provider)
                   .orderByDesc(OssKeyDO::getCreateTime);
            
            List<OssKeyDO> keys = this.list(wrapper);
            List<OssKeyVO> keyVOs = keys.stream().map(this::convertToVO).collect(Collectors.toList());
            
            // 统计激活的密钥数量
            long activeCount = keys.stream()
                    .filter(key -> SystemConstants.Status.ACTIVE.equals(key.getStatus()))
                    .count();
            
            OssProviderVO providerVO = new OssProviderVO();
            providerVO.setProviderName(provider);
            providerVO.setDisplayName(getProviderDisplayName(provider));
            providerVO.setKeys(keyVOs);
            providerVO.setKeyCount(keyVOs.size());
            providerVO.setActiveKeyCount((int) activeCount);
            providerVO.setDefaultEndpoint(ossFactory.getDefaultEndpoint(provider));
            providerVO.setSupportedFeatures(ossFactory.getService(provider).getSupportedOperations());
            
            result.add(providerVO);
        }
        
        return result;
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
     * 转换为VO对象
     */
    private OssKeyVO convertToVO(OssKeyDO ossKey) {
        OssKeyVO vo = new OssKeyVO();
        BeanUtils.copyProperties(ossKey, vo);
        
        // 脱敏处理密钥信息
//        if (StringUtils.hasText(vo.getAccessKeyId())) {
//            vo.setAccessKeyId(maskSensitiveInfo(vo.getAccessKeyId()));
//        }
//        if (StringUtils.hasText(vo.getAccessKeySecret())) {
//            vo.setAccessKeySecret(maskSensitiveInfo(vo.getAccessKeySecret()));
//        }
        
        return vo;
    }
    
    /**
     * 脱敏处理敏感信息
     */
    private String maskSensitiveInfo(String sensitive) {
        if (!StringUtils.hasText(sensitive) || sensitive.length() <= 8) {
            return "****";
        }
        return sensitive.substring(0, 4) + "****" + sensitive.substring(sensitive.length() - 4);
    }
}