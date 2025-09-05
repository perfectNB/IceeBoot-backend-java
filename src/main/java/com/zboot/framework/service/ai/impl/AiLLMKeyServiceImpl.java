package com.zboot.framework.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.llm.factory.LLMFactory;
import com.zboot.common.service.llm.service.LLMService;
import com.zboot.framework.dto.create.ai.AiLLMKeyDTO;
import com.zboot.framework.dto.update.ai.AiLLMKeyUpdateDTO;
import com.zboot.framework.entity.ai.AiLLMKeyDO;
import com.zboot.framework.mapper.ai.AiLLMKeyMapper;
import com.zboot.framework.query.ai.AiLLMKeyQuery;
import com.zboot.framework.service.ai.IAiLLMKeyService;
import com.zboot.framework.vo.ai.AiLLMKeyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.zboot.framework.vo.ai.AiLLMProviderVO;

/**
 * AI大模型密钥服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiLLMKeyServiceImpl extends ServiceImpl<AiLLMKeyMapper, AiLLMKeyDO> implements IAiLLMKeyService {

    private final LLMFactory llmFactory;
    
    // 用于轮询获取密钥的计数器
    private final Map<String, AtomicInteger> providerCounters = new HashMap<>();

    @Override
    public IPage<AiLLMKeyVO> getLLMKeyPage(AiLLMKeyQuery query) {
        Page<AiLLMKeyDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AiLLMKeyDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AiLLMKeyDO::getRemark, query.getKeyword())
                   .or()
                   .like(AiLLMKeyDO::getLlmProvider, query.getKeyword());
        }
        if (StringUtils.hasText(query.getLlmProvider())) {
            wrapper.eq(AiLLMKeyDO::getLlmProvider, query.getLlmProvider());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AiLLMKeyDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(AiLLMKeyDO::getCreateTime);
        
        IPage<AiLLMKeyDO> result = this.page(page, wrapper);
        
        // 转换为VO
        Page<AiLLMKeyVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AiLLMKeyVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }

    @Override
    public AiLLMKeyVO getLLMKeyById(Long id) {
        AiLLMKeyDO llmKey = this.getById(id);
        return llmKey != null ? convertToVO(llmKey) : null;
    }

    @Override
    public Boolean addLLMKey(AiLLMKeyDTO dto) {
        // 验证厂商是否支持
        if (!getSupportedProviders().contains(dto.getLlmProvider())) {
            throw new IllegalArgumentException("不支持的大模型厂商: " + dto.getLlmProvider());
        }
        
        AiLLMKeyDO llmKey = new AiLLMKeyDO();
        BeanUtils.copyProperties(dto, llmKey);
        
        // 设置默认状态
        if (!StringUtils.hasText(llmKey.getStatus())) {
            llmKey.setStatus(SystemConstants.Status.ACTIVE);
        }
        
        return this.save(llmKey);
    }

    @Override
    public Boolean updateLLMKey(AiLLMKeyUpdateDTO dto) {
        AiLLMKeyDO llmKey = new AiLLMKeyDO();
        BeanUtils.copyProperties(dto, llmKey);
        return this.updateById(llmKey);
    }

    @Override
    public Boolean deleteLLMKey(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Boolean updateLLMKeyStatus(Long id, String status) {
        AiLLMKeyDO llmKey = new AiLLMKeyDO();
        llmKey.setId(id);
        llmKey.setStatus(status);
        return this.updateById(llmKey);
    }

    @Override
    public List<AiLLMKeyVO> getActiveLLMKeysByProvider(String llmProvider) {
        LambdaQueryWrapper<AiLLMKeyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiLLMKeyDO::getLlmProvider, llmProvider)
               .eq(AiLLMKeyDO::getStatus, SystemConstants.Status.ACTIVE)
               .orderByDesc(AiLLMKeyDO::getCreateTime);
        
        List<AiLLMKeyDO> llmKeys = this.list(wrapper);
        return llmKeys.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedProviders() {
        // 从SystemConstants.LLMProvider获取支持的厂商列表
        return Arrays.asList(
            SystemConstants.LLMProvider.CHATGPT,
            SystemConstants.LLMProvider.GROK,
            SystemConstants.LLMProvider.CLAUDE,
            SystemConstants.LLMProvider.GEMINI,
            SystemConstants.LLMProvider.WENXIN,
            SystemConstants.LLMProvider.MOONSHOT,
            SystemConstants.LLMProvider.QWEN,
            SystemConstants.LLMProvider.DEEPSEEK,
            SystemConstants.LLMProvider.ZHIPU,
            SystemConstants.LLMProvider.BAICHUAN,
            SystemConstants.LLMProvider.SPARK
        );
    }

    @Override
    public AiLLMKeyVO getAvailableKeyByProvider(String llmProvider) {
        List<AiLLMKeyVO> keys = getActiveLLMKeysByProvider(llmProvider);
        if (keys.isEmpty()) {
            return null;
        }
        
        // 轮询获取密钥
        AtomicInteger counter = providerCounters.computeIfAbsent(llmProvider, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % keys.size();
        return keys.get(index);
    }

    @Override
    public Boolean validateLLMKey(Long id) {
        AiLLMKeyDO llmKey = this.getById(id);
        if (llmKey == null) {
            return false;
        }
        
        try {
            // 获取对应的LLM服务
            LLMService llmService = llmFactory.getService(llmKey.getLlmProvider());
            if (llmService == null) {
                return false;
            }
            
            // 验证API密钥
            String apiKey = llmKey.getApiKey();
            Boolean isValid = llmService.validateApiKey(apiKey, null).block();
            
            return Boolean.TRUE.equals(isValid);
        } catch (Exception e) {
            log.error("验证密钥失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Integer batchImportKeys(List<AiLLMKeyDTO> keys) {
        int successCount = 0;
        for (AiLLMKeyDTO keyDTO : keys) {
            try {
                if (addLLMKey(keyDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("导入密钥失败: {}", e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Object> getKeyUsageStats(String llmProvider) {
        Map<String, Object> stats = new HashMap<>();
        
        LambdaQueryWrapper<AiLLMKeyDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(llmProvider)) {
            wrapper.eq(AiLLMKeyDO::getLlmProvider, llmProvider);
        }
        
        // 总数统计
        long totalCount = this.count(wrapper);
        stats.put("totalCount", totalCount);
        
        // 激活状态统计
        wrapper.eq(AiLLMKeyDO::getStatus, SystemConstants.Status.ACTIVE);
        long activeCount = this.count(wrapper);
        stats.put("activeCount", activeCount);
        
        // 禁用状态统计
        wrapper.clear();
        if (StringUtils.hasText(llmProvider)) {
            wrapper.eq(AiLLMKeyDO::getLlmProvider, llmProvider);
        }
        wrapper.eq(AiLLMKeyDO::getStatus, SystemConstants.Status.INACTIVE);
        long inactiveCount = this.count(wrapper);
        stats.put("inactiveCount", inactiveCount);
        
        // 按厂商分组统计
        if (!StringUtils.hasText(llmProvider)) {
            Map<String, Long> providerStats = new HashMap<>();
            for (String provider : getSupportedProviders()) {
                LambdaQueryWrapper<AiLLMKeyDO> providerWrapper = new LambdaQueryWrapper<>();
                providerWrapper.eq(AiLLMKeyDO::getLlmProvider, provider)
                              .eq(AiLLMKeyDO::getStatus, SystemConstants.Status.ACTIVE);
                long count = this.count(providerWrapper);
                providerStats.put(provider, count);
            }
            stats.put("providerStats", providerStats);
        }
        
        return stats;
    }

    @Override
    public List<AiLLMProviderVO> getAllProviderKeys() {
        List<AiLLMProviderVO> result = new ArrayList<>();
        
        // 获取所有支持的厂商
        List<String> providers = getSupportedProviders();
        
        for (String provider : providers) {
            // 获取该厂商的所有密钥（包括激活和禁用的）
            LambdaQueryWrapper<AiLLMKeyDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiLLMKeyDO::getLlmProvider, provider)
                   .orderByDesc(AiLLMKeyDO::getCreateTime);
            
            List<AiLLMKeyDO> keys = this.list(wrapper);
            List<AiLLMKeyVO> keyVOs = keys.stream().map(this::convertToVO).collect(Collectors.toList());

            // 统计激活的密钥数量
            long activeCount = keys.stream()
                    .filter(key -> SystemConstants.Status.ACTIVE.equals(key.getStatus()))
                    .count();
            
            AiLLMProviderVO providerVO = new AiLLMProviderVO();
            providerVO.setProviderName(provider);
            providerVO.setDisplayName(getProviderDisplayName(provider));
            providerVO.setKeys(keyVOs);
            providerVO.setKeyCount(keyVOs.size());
            providerVO.setActiveKeyCount((int) activeCount);
            
            result.add(providerVO);
        }
        
        return result;
    }
    
    /**
     * 获取厂商显示名称
     */
    private String getProviderDisplayName(String provider) {
        return switch (provider.toLowerCase()) {
            case "chatgpt" -> "ChatGPT";
            case "grok" -> "Grok";
            case "claude" -> "Claude";
            case "gemini" -> "Gemini";
            case "wenxin" -> "文心一言";
            case "moonshot" -> "月之暗面 (Kimi)";
            case "qwen" -> "通义千问";
            case "deepseek" -> "DeepSeek";
            case "zhipu" -> "智谱AI";
            case "baichuan" -> "百川智能";
            case "spark" -> "讯飞星火";
            default -> provider;
        };
    }

    /**
     * 转换为VO对象
     */
    private AiLLMKeyVO convertToVO(AiLLMKeyDO llmKey) {
        AiLLMKeyVO vo = new AiLLMKeyVO();
        BeanUtils.copyProperties(llmKey, vo);
        
        // 脱敏处理密钥信息

        if (StringUtils.hasText(vo.getApiKey())) {
            vo.setApiKey(maskSensitiveInfo(vo.getApiKey()));
        }
        if (StringUtils.hasText(vo.getSecretKey())) {
            vo.setSecretKey(maskSensitiveInfo(vo.getSecretKey()));
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