package com.zboot.framework.service.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.ai.AiLLMKeyDTO;
import com.zboot.framework.entity.ai.AiLLMKeyDO;
import com.zboot.framework.query.ai.AiLLMKeyQuery;
import com.zboot.framework.vo.ai.AiLLMKeyVO;
import com.zboot.framework.vo.ai.AiLLMProviderVO;

import java.util.List;
import java.util.Map;

/**
 * AI大模型密钥服务接口
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
public interface IAiLLMKeyService extends IService<AiLLMKeyDO> {

    /**
     * 分页查询AI大模型密钥列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AiLLMKeyVO> getLLMKeyPage(AiLLMKeyQuery query);

    /**
     * 根据ID获取AI大模型密钥详情
     * 
     * @param id 密钥ID
     * @return 密钥详情
     */
    AiLLMKeyVO getLLMKeyById(Long id);

    /**
     * 新增AI大模型密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean addLLMKey(AiLLMKeyDTO dto);

    /**
     * 更新AI大模型密钥
     * 
     * @param dto 密钥信息
     * @return 是否成功
     */
    Boolean updateLLMKey(AiLLMKeyDTO dto);

    /**
     * 删除AI大模型密钥
     * 
     * @param ids 密钥ID列表
     * @return 是否成功
     */
    Boolean deleteLLMKey(List<Long> ids);

    /**
     * 修改AI大模型密钥状态
     * 
     * @param id 密钥ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateLLMKeyStatus(Long id, String status);

    /**
     * 根据厂商获取激活的密钥列表
     * 
     * @param llmProvider 大模型厂商
     * @return 密钥列表
     */
    List<AiLLMKeyVO> getActiveLLMKeysByProvider(String llmProvider);

    /**
     * 获取所有支持的大模型厂商列表
     * 
     * @return 厂商列表
     */
    List<String> getSupportedProviders();

    /**
     * 根据厂商获取可用的密钥（轮询获取）
     * 
     * @param llmProvider 大模型厂商
     * @return 可用密钥
     */
    AiLLMKeyVO getAvailableKeyByProvider(String llmProvider);

    /**
     * 验证密钥是否有效
     * 
     * @param id 密钥ID
     * @return 是否有效
     */
    Boolean validateLLMKey(Long id);

    /**
     * 批量导入密钥
     * 
     * @param keys 密钥列表
     * @return 导入成功数量
     */
    Integer batchImportKeys(List<AiLLMKeyDTO> keys);

    /**
     * 获取密钥使用统计
     * 
     * @param llmProvider 大模型厂商
     * @return 统计信息
     */
    Map<String, Object> getKeyUsageStats(String llmProvider);

    /**
     * 获取所有厂商的密钥列表（按厂商分组）
     * 
     * @return 厂商密钥列表
     */
    List<AiLLMProviderVO> getAllProviderKeys();
}