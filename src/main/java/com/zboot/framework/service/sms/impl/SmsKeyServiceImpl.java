package com.zboot.framework.service.sms.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.sms.dto.SMSRequest;
import com.zboot.common.service.sms.factory.SMSFactory;
import com.zboot.common.service.sms.service.SMSService;
import com.zboot.framework.dto.sms.SmsKeyDTO;
import com.zboot.framework.entity.sms.SmsKeyDO;
import com.zboot.framework.mapper.sms.SmsKeyMapper;
import com.zboot.framework.query.sms.SmsKeyQuery;
import com.zboot.framework.service.sms.ISmsKeyService;
import com.zboot.framework.vo.sms.SmsKeyVO;
import com.zboot.framework.vo.sms.SmsProviderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * SMS密钥服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsKeyServiceImpl extends ServiceImpl<SmsKeyMapper, SmsKeyDO> implements ISmsKeyService {

    // 用于轮询获取密钥的计数器
    private final Map<String, AtomicInteger> providerCounters = new HashMap<>();

    @Override
    public IPage<SmsKeyVO> getSmsKeyPage(SmsKeyQuery query) {
        Page<SmsKeyDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SmsKeyDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(SmsKeyDO::getRemark, query.getKeyword())
                   .or()
                   .like(SmsKeyDO::getSmsProvider, query.getKeyword());
        }
        if (StringUtils.hasText(query.getSmsProvider())) {
            wrapper.eq(SmsKeyDO::getSmsProvider, query.getSmsProvider());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SmsKeyDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(SmsKeyDO::getCreateTime);
        
        IPage<SmsKeyDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<SmsKeyVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SmsKeyVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public SmsKeyVO getSmsKeyById(Long id) {
        SmsKeyDO smsKey = this.getById(id);
        return smsKey != null ? convertToVO(smsKey) : null;
    }

    @Override
    public Boolean addSmsKey(SmsKeyDTO dto) {
        // 验证厂商是否支持
        if (!getSupportedProviders().contains(dto.getSmsProvider())) {
            throw new IllegalArgumentException("不支持的SMS厂商: " + dto.getSmsProvider());
        }
        
        SmsKeyDO smsKey = new SmsKeyDO();
        BeanUtils.copyProperties(dto, smsKey);
        
        // 设置默认状态
        if (!StringUtils.hasText(smsKey.getStatus())) {
            smsKey.setStatus(SystemConstants.Status.ACTIVE);
        }
        
        // 设置默认端点
        if (!StringUtils.hasText(smsKey.getEndpoint())) {
            smsKey.setEndpoint(SMSFactory.getDefaultEndpoint(dto.getSmsProvider()));
        }
        
        return this.save(smsKey);
    }

    @Override
    public Boolean updateSmsKey(SmsKeyDTO dto) {
        SmsKeyDO smsKey = new SmsKeyDO();
        BeanUtils.copyProperties(dto, smsKey);
        return this.updateById(smsKey);
    }

    @Override
    public Boolean deleteSmsKey(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Boolean updateSmsKeyStatus(Long id, String status) {
        SmsKeyDO smsKey = new SmsKeyDO();
        smsKey.setId(id);
        smsKey.setStatus(status);
        return this.updateById(smsKey);
    }

    @Override
    public List<SmsKeyVO> getActiveSmsKeysByProvider(String smsProvider) {
        LambdaQueryWrapper<SmsKeyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsKeyDO::getSmsProvider, smsProvider)
               .eq(SmsKeyDO::getStatus, SystemConstants.Status.ACTIVE)
               .orderByDesc(SmsKeyDO::getCreateTime);
        
        List<SmsKeyDO> smsKeys = this.list(wrapper);
        return smsKeys.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedProviders() {
        // 从SMSFactory获取支持的厂商列表
        return Arrays.asList(SMSFactory.getSupportedProviders());
    }

    @Override
    public SmsKeyVO getAvailableKeyByProvider(String smsProvider) {
        List<SmsKeyVO> keys = getActiveSmsKeysByProvider(smsProvider);
        if (keys.isEmpty()) {
            return null;
        }
        
        // 轮询获取密钥
        AtomicInteger counter = providerCounters.computeIfAbsent(smsProvider, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % keys.size();
        return keys.get(index);
    }

    @Override
    public Boolean validateSmsKey(Long id) {
        SmsKeyDO smsKey = this.getById(id);
        if (smsKey == null) {
            return false;
        }
        
        try {
            // 获取对应的SMS服务
            SMSService smsService = SMSFactory.getService(smsKey.getSmsProvider());
            if (smsService == null) {
                return false;
            }
            
            // 构建验证请求
            SMSRequest request = new SMSRequest()
                    .setAccessKeyId(smsKey.getAccessKeyId())
                    .setAccessKeySecret(smsKey.getAccessKeySecret())
                    .setEndpoint(smsKey.getEndpoint())
                    .setRegion(smsKey.getRegion())
                    .setSignName(smsKey.getDefaultSignName())
                    .setTemplateId(smsKey.getDefaultTemplateId());
            
            // 验证配置
            Boolean isValid = smsService.validateConfig(request).block();
            
            return Boolean.TRUE.equals(isValid);
        } catch (Exception e) {
            log.error("验证SMS密钥失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Integer batchImportKeys(List<SmsKeyDTO> keys) {
        int successCount = 0;
        for (SmsKeyDTO keyDTO : keys) {
            try {
                if (addSmsKey(keyDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("导入SMS密钥失败: {}", e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Object> getKeyUsageStats(String smsProvider) {
        Map<String, Object> stats = new HashMap<>();
        
        LambdaQueryWrapper<SmsKeyDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(smsProvider)) {
            wrapper.eq(SmsKeyDO::getSmsProvider, smsProvider);
        }
        
        // 总数统计
        long totalCount = this.count(wrapper);
        stats.put("totalCount", totalCount);
        
        // 激活状态统计
        wrapper.eq(SmsKeyDO::getStatus, SystemConstants.Status.ACTIVE);
        long activeCount = this.count(wrapper);
        stats.put("activeCount", activeCount);
        
        // 禁用状态统计
        wrapper.clear();
        if (StringUtils.hasText(smsProvider)) {
            wrapper.eq(SmsKeyDO::getSmsProvider, smsProvider);
        }
        wrapper.eq(SmsKeyDO::getStatus, SystemConstants.Status.INACTIVE);
        long inactiveCount = this.count(wrapper);
        stats.put("inactiveCount", inactiveCount);
        
        // 按厂商分组统计
        if (!StringUtils.hasText(smsProvider)) {
            Map<String, Long> providerStats = new HashMap<>();
            for (String provider : getSupportedProviders()) {
                LambdaQueryWrapper<SmsKeyDO> providerWrapper = new LambdaQueryWrapper<>();
                providerWrapper.eq(SmsKeyDO::getSmsProvider, provider)
                              .eq(SmsKeyDO::getStatus, SystemConstants.Status.ACTIVE);
                long count = this.count(providerWrapper);
                providerStats.put(provider, count);
            }
            stats.put("providerStats", providerStats);
        }
        
        return stats;
    }

    @Override
    public List<SmsProviderVO> getAllProviderKeys() {
        List<SmsProviderVO> result = new ArrayList<>();
        
        // 获取所有支持的厂商
        List<String> providers = getSupportedProviders();
        
        for (String provider : providers) {
            // 获取该厂商的所有密钥（包括激活和禁用的）
            LambdaQueryWrapper<SmsKeyDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SmsKeyDO::getSmsProvider, provider)
                   .orderByDesc(SmsKeyDO::getCreateTime);
            
            List<SmsKeyDO> keys = this.list(wrapper);
            List<SmsKeyVO> keyVOs = keys.stream().map(this::convertToVO).collect(Collectors.toList());
            
            // 统计激活的密钥数量
            long activeCount = keys.stream()
                    .filter(key -> SystemConstants.Status.ACTIVE.equals(key.getStatus()))
                    .count();
            
            SmsProviderVO providerVO = new SmsProviderVO();
            providerVO.setProviderName(provider);
            providerVO.setDisplayName(getProviderDisplayName(provider));
            providerVO.setKeys(keyVOs);
            providerVO.setKeyCount(keyVOs.size());
            providerVO.setActiveKeyCount((int) activeCount);
            providerVO.setDefaultEndpoint(SMSFactory.getDefaultEndpoint(provider));
            providerVO.setSupportedSmsTypes(Arrays.asList(SMSFactory.getSupportedSMSTypes(provider)));
            providerVO.setSupportedFeatures(Arrays.asList(SMSFactory.getSupportedFeatures(provider)));
            
            result.add(providerVO);
        }
        
        return result;
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
     * 转换为VO对象
     */
    private SmsKeyVO convertToVO(SmsKeyDO smsKey) {
        SmsKeyVO vo = new SmsKeyVO();
        BeanUtils.copyProperties(smsKey, vo);
        
        // 脱敏处理密钥信息
        if (StringUtils.hasText(vo.getAccessKeyId())) {
            vo.setAccessKeyId(maskSensitiveInfo(vo.getAccessKeyId()));
        }
        if (StringUtils.hasText(vo.getAccessKeySecret())) {
            vo.setAccessKeySecret(maskSensitiveInfo(vo.getAccessKeySecret()));
        }
        
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