/*
 * @Description: AI大模型Mapper接口
 * @Date: 2025-01-27
 * @Author: CodeIcee
 */
package com.zboot.framework.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.ai.AiLLMDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI大模型Mapper接口
 *
 * @author CodeIcee
 * @since 2025-01-27
 */
@Mapper
public interface AiLLMMapper extends BaseMapper<AiLLMDO> {

    /**
     * 根据状态获取模型列表
     *
     * @param status 状态
     * @return 模型列表
     */
    List<AiLLMDO> selectByStatus(@Param("status") String status);

    /**
     * 根据名称模糊查询
     *
     * @param name 模型名称
     * @return 模型列表
     */
    List<AiLLMDO> selectByNameLike(@Param("name") String name);
}