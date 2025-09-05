package com.zboot.framework.service.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.framework.dto.create.ai.AiAgentDTO;
import com.zboot.framework.dto.update.ai.AiAgentUpdateDTO;
import com.zboot.framework.entity.ai.AiAgentDO;
import com.zboot.framework.query.ai.AiAgentQuery;
import com.zboot.framework.vo.ai.AiAgentVO;

import java.util.List;

/**
 * AI代理服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface IAiAgentService extends IService<AiAgentDO> {

    /**
     * 分页查询AI代理列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AiAgentVO> getAgentPage(AiAgentQuery query);

    /**
     * 根据ID获取AI代理详情
     * 
     * @param id 代理ID
     * @return 代理详情
     */
    AiAgentVO getAgentById(Long id);

    /**
     * 新增AI代理
     * 
     * @param dto 代理信息
     * @return 是否成功
     */
    Boolean addAgent(AiAgentDTO dto);

    /**
     * 更新AI代理
     * 
     * @param dto 代理信息
     * @return 是否成功
     */
    Boolean updateAgent(AiAgentUpdateDTO dto);

    /**
     * 删除AI代理
     * 
     * @param ids 代理ID列表
     * @return 是否成功
     */
    Boolean deleteAgent(List<Long> ids);

    /**
     * 修改AI代理状态
     * 
     * @param id 代理ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateAgentStatus(Long id, String status);

    /**
     * 根据平台获取代理列表
     * 
     * @param platform 平台
     * @return 代理列表
     */
    List<AiAgentVO> getAgentsByPlatform(String platform);

    /**
     * 根据类型获取代理列表
     * 
     * @param type 类型
     * @return 代理列表
     */
    List<AiAgentVO> getAgentsByType(String type);
}