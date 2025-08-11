/*
 * @Description: AI大模型密钥Mapper接口
 * @Date: 2025-01-27
 * @Author: CodeIcee
 */
package com.zboot.framework.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.ai.AiLLMKeyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI大模型密钥Mapper接口
 *
 * @author CodeIcee
 * @since 2025-01-27
 */
@Mapper
public interface AiLLMKeyMapper extends BaseMapper<AiLLMKeyDO> {

    /**
     * 根据大模型ID获取密钥列表
     *
     * @param llmId 大模型ID
     * @return 密钥列表
     */
    List<AiLLMKeyDO> selectByLlmId(@Param("llmId") Long llmId);

    /**
     * 根据状态获取密钥列表
     *
     * @param status 状态
     * @return 密钥列表
     */
    List<AiLLMKeyDO> selectByStatus(@Param("status") String status);

    /**
     * 根据大模型ID和状态获取密钥列表
     *
     * @param llmId 大模型ID
     * @param status 状态
     * @return 密钥列表
     */
    List<AiLLMKeyDO> selectByLlmIdAndStatus(@Param("llmId") Long llmId, @Param("status") String status);
}