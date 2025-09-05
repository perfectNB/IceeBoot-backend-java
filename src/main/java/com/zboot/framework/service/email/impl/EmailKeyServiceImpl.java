package com.zboot.framework.service.email.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.email.dto.EmailRequest;
import com.zboot.common.service.email.factory.EmailFactory;
import com.zboot.common.service.email.service.EmailService;
import com.zboot.framework.dto.email.EmailKeyDTO;
import com.zboot.framework.entity.email.EmailKeyDO;
import com.zboot.framework.mapper.email.EmailKeyMapper;
import com.zboot.framework.query.email.EmailKeyQuery;
import com.zboot.framework.service.email.IEmailKeyService;
import com.zboot.framework.vo.email.EmailKeyVO;
import com.zboot.framework.vo.email.EmailProviderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Email密钥服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailKeyServiceImpl extends ServiceImpl<EmailKeyMapper, EmailKeyDO> implements IEmailKeyService {

    // 用于轮询获取密钥的计数器
    private final Map<String, AtomicInteger> providerCounters = new HashMap<>();

    @Override
    public IPage<EmailKeyVO> getEmailKeyPage(EmailKeyQuery query) {
        Page<EmailKeyDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<EmailKeyDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(EmailKeyDO::getRemark, query.getKeyword())
                   .or()
                   .like(EmailKeyDO::getEmailProvider, query.getKeyword())
                   .or()
                   .like(EmailKeyDO::getEmailAddress, query.getKeyword());
        }
        if (StringUtils.hasText(query.getEmailProvider())) {
            wrapper.eq(EmailKeyDO::getEmailProvider, query.getEmailProvider());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(EmailKeyDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(EmailKeyDO::getCreateTime);
        
        IPage<EmailKeyDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<EmailKeyVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<EmailKeyVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public EmailKeyVO getEmailKeyById(Long id) {
        EmailKeyDO emailKey = this.getById(id);
        return emailKey != null ? convertToVO(emailKey) : null;
    }

    @Override
    public Boolean addEmailKey(EmailKeyDTO dto) {
        // 验证厂商是否支持
        if (!getSupportedProviders().contains(dto.getEmailProvider())) {
            throw new IllegalArgumentException("不支持的Email厂商: " + dto.getEmailProvider());
        }
        
        EmailKeyDO emailKey = new EmailKeyDO();
        BeanUtils.copyProperties(dto, emailKey);
        
        // 设置默认状态
        if (!StringUtils.hasText(emailKey.getStatus())) {
            emailKey.setStatus(SystemConstants.Status.ACTIVE);
        }
        
        // 设置默认配置
        setDefaultConfig(emailKey, dto.getEmailProvider());
        
        return this.save(emailKey);
    }

    @Override
    public Boolean updateEmailKey(EmailKeyDTO dto) {
        EmailKeyDO emailKey = new EmailKeyDO();
        BeanUtils.copyProperties(dto, emailKey);
        return this.updateById(emailKey);
    }

    @Override
    public Boolean deleteEmailKey(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Boolean updateEmailKeyStatus(Long id, String status) {
        EmailKeyDO emailKey = new EmailKeyDO();
        emailKey.setId(id);
        emailKey.setStatus(status);
        return this.updateById(emailKey);
    }

    @Override
    public List<EmailKeyVO> getActiveEmailKeysByProvider(String emailProvider) {
        LambdaQueryWrapper<EmailKeyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailKeyDO::getEmailProvider, emailProvider)
               .eq(EmailKeyDO::getStatus, SystemConstants.Status.ACTIVE)
               .orderByDesc(EmailKeyDO::getCreateTime);
        
        List<EmailKeyDO> emailKeys = this.list(wrapper);
        return emailKeys.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedProviders() {
        // 从EmailFactory获取支持的厂商列表
        return Arrays.asList(EmailFactory.getSupportedProviders());
    }

    @Override
    public EmailKeyVO getAvailableKeyByProvider(String emailProvider) {
        List<EmailKeyVO> keys = getActiveEmailKeysByProvider(emailProvider);
        if (keys.isEmpty()) {
            return null;
        }
        
        // 轮询获取密钥
        AtomicInteger counter = providerCounters.computeIfAbsent(emailProvider, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % keys.size();
        return keys.get(index);
    }

    @Override
    public Boolean validateEmailKey(Long id) {
        EmailKeyDO emailKey = this.getById(id);
        if (emailKey == null) {
            return false;
        }
        
        try {
            // 获取对应的Email服务
            EmailService emailService = EmailFactory.getService(emailKey.getEmailProvider());
            if (emailService == null) {
                return false;
            }
            
            // 构建验证请求
            EmailRequest request = new EmailRequest()
                    .setFromEmail(emailKey.getEmailAddress())
                    .setUsername(emailKey.getEmailAddress())
                    .setPassword(emailKey.getEmailPassword())
                    .setSmtpHost(emailKey.getSmtpHost())
                    .setSmtpPort(emailKey.getSmtpPort())
                    .setEnableSsl(emailKey.getEnableSsl())
                    .setEnableTls(emailKey.getEnableTls())
                    .setEnableAuth(emailKey.getEnableAuth())
                    .setConnectionTimeout(emailKey.getConnectionTimeout())
                    .setReadTimeout(emailKey.getReadTimeout());
            
            // 验证配置
            Boolean isValid = emailService.validateConfig(request).block();
            
            return Boolean.TRUE.equals(isValid);
        } catch (Exception e) {
            log.error("验证Email密钥失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Integer batchImportKeys(List<EmailKeyDTO> keys) {
        int successCount = 0;
        for (EmailKeyDTO keyDTO : keys) {
            try {
                if (addEmailKey(keyDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("导入Email密钥失败: {}", e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Object> getKeyUsageStats(String emailProvider) {
        Map<String, Object> stats = new HashMap<>();
        
        LambdaQueryWrapper<EmailKeyDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(emailProvider)) {
            wrapper.eq(EmailKeyDO::getEmailProvider, emailProvider);
        }
        
        // 总数统计
        long totalCount = this.count(wrapper);
        stats.put("totalCount", totalCount);
        
        // 激活状态统计
        wrapper.eq(EmailKeyDO::getStatus, SystemConstants.Status.ACTIVE);
        long activeCount = this.count(wrapper);
        stats.put("activeCount", activeCount);
        
        // 禁用状态统计
        wrapper.clear();
        if (StringUtils.hasText(emailProvider)) {
            wrapper.eq(EmailKeyDO::getEmailProvider, emailProvider);
        }
        wrapper.eq(EmailKeyDO::getStatus, SystemConstants.Status.INACTIVE);
        long inactiveCount = this.count(wrapper);
        stats.put("inactiveCount", inactiveCount);
        
        // 按厂商分组统计
        if (!StringUtils.hasText(emailProvider)) {
            Map<String, Long> providerStats = new HashMap<>();
            for (String provider : getSupportedProviders()) {
                LambdaQueryWrapper<EmailKeyDO> providerWrapper = new LambdaQueryWrapper<>();
                providerWrapper.eq(EmailKeyDO::getEmailProvider, provider)
                              .eq(EmailKeyDO::getStatus, SystemConstants.Status.ACTIVE);
                long count = this.count(providerWrapper);
                providerStats.put(provider, count);
            }
            stats.put("providerStats", providerStats);
        }
        
        return stats;
    }

    @Override
    public List<EmailProviderVO> getAllProviderKeys() {
        List<EmailProviderVO> result = new ArrayList<>();
        
        // 获取所有支持的厂商
        List<String> providers = getSupportedProviders();
        
        for (String provider : providers) {
            // 获取该厂商的所有密钥（包括激活和禁用的）
            LambdaQueryWrapper<EmailKeyDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EmailKeyDO::getEmailProvider, provider)
                   .orderByDesc(EmailKeyDO::getCreateTime);
            
            List<EmailKeyDO> keys = this.list(wrapper);
            List<EmailKeyVO> keyVOs = keys.stream().map(this::convertToVO).collect(Collectors.toList());
            
            // 统计激活的密钥数量
            long activeCount = keys.stream()
                    .filter(key -> SystemConstants.Status.ACTIVE.equals(key.getStatus()))
                    .count();
            
            EmailProviderVO providerVO = new EmailProviderVO();
            providerVO.setProviderName(provider);
            providerVO.setDisplayName(getProviderDisplayName(provider));
            providerVO.setKeys(keyVOs);
            providerVO.setKeyCount(keyVOs.size());
            providerVO.setActiveKeyCount((int) activeCount);
            providerVO.setDefaultSmtpHost(getDefaultSmtpHost(provider));
            providerVO.setDefaultSmtpPort(getDefaultSmtpPort(provider));
            providerVO.setSupportedEmailTypes(Arrays.asList(EmailFactory.getSupportedEmailTypes(provider)));
            providerVO.setSupportedFeatures(Arrays.asList(EmailFactory.getSupportedFeatures(provider)));
            
            result.add(providerVO);
        }
        
        return result;
    }
    
    /**
     * 设置默认配置
     */
    private void setDefaultConfig(EmailKeyDO emailKey, String provider) {
        if (!StringUtils.hasText(emailKey.getSmtpHost())) {
            emailKey.setSmtpHost(getDefaultSmtpHost(provider));
        }
        if (emailKey.getSmtpPort() == null) {
            emailKey.setSmtpPort(getDefaultSmtpPort(provider));
        }
        if (emailKey.getEnableSsl() == null) {
            emailKey.setEnableSsl(getDefaultEnableSsl(provider));
        }
        if (emailKey.getEnableTls() == null) {
            emailKey.setEnableTls(getDefaultEnableTls(provider));
        }
        if (emailKey.getEnableAuth() == null) {
            emailKey.setEnableAuth(true);
        }
        if (emailKey.getConnectionTimeout() == null) {
            emailKey.setConnectionTimeout(30000);
        }
        if (emailKey.getReadTimeout() == null) {
            emailKey.setReadTimeout(60000);
        }
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
     * 获取默认SMTP主机
     */
    private String getDefaultSmtpHost(String provider) {
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
     * 获取默认SMTP端口
     */
    private Integer getDefaultSmtpPort(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq", "netease", "gmail", "outlook", "tencent" -> 587;
            case "aliyun" -> 25;
            default -> 587;
        };
    }
    
    /**
     * 获取默认SSL设置
     */
    private Boolean getDefaultEnableSsl(String provider) {
        return switch (provider.toLowerCase()) {
            case "gmail" -> true;
            default -> false;
        };
    }
    
    /**
     * 获取默认TLS设置
     */
    private Boolean getDefaultEnableTls(String provider) {
        return switch (provider.toLowerCase()) {
            case "qq", "netease", "outlook", "tencent" -> true;
            case "gmail", "aliyun" -> false;
            default -> true;
        };
    }

    /**
     * 转换为VO对象
     */
    private EmailKeyVO convertToVO(EmailKeyDO emailKey) {
        EmailKeyVO vo = new EmailKeyVO();
        BeanUtils.copyProperties(emailKey, vo);
        
        // 脱敏处理密码信息
        if (StringUtils.hasText(vo.getEmailPassword())) {
            vo.setEmailPassword(maskSensitiveInfo(vo.getEmailPassword()));
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