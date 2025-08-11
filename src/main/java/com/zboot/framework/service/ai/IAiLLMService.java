package com.zboot.framework.service.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.ai.AiLLMDTO;
import com.zboot.framework.entity.ai.AiLLMDO;
import com.zboot.framework.query.ai.AiLLMQuery;
import com.zboot.framework.vo.ai.AiLLMVO;

import java.util.List;

/**
 * AI大模型服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface IAiLLMService extends IService<AiLLMDO> {

    /**
     * 分页查询AI大模型列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AiLLMVO> getLLMPage(AiLLMQuery query);

    /**
     * 根据ID获取AI大模型详情
     * 
     * @param id 模型ID
     * @return 模型详情
     */
    AiLLMVO getLLMById(Long id);

    /**
     * 新增AI大模型
     * 
     * @param dto 模型信息
     * @return 是否成功
     */
    Boolean addLLM(AiLLMDTO dto);

    /**
     * 更新AI大模型
     * 
     * @param dto 模型信息
     * @return 是否成功
     */
    Boolean updateLLM(AiLLMDTO dto);

    /**
     * 删除AI大模型
     * 
     * @param ids 模型ID列表
     * @return 是否成功
     */
    Boolean deleteLLM(List<Long> ids);

    /**
     * 修改AI大模型状态
     * 
     * @param id 模型ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateLLMStatus(Long id, String status);

    /**
     * 获取所有激活的模型列表
     *
     * @return 激活的模型列表
     */
    List<AiLLMVO> getActiveLLMList();
}