package com.zboot.framework.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.create.ai.AiAgentDTO;
import com.zboot.framework.dto.update.ai.AiAgentUpdateDTO;
import com.zboot.framework.entity.ai.AiAgentDO;
import com.zboot.framework.mapper.ai.AiAgentMapper;
import com.zboot.framework.query.ai.AiAgentQuery;
import com.zboot.framework.service.ai.IAiAgentService;
import com.zboot.framework.vo.ai.AiAgentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI代理服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl extends ServiceImpl<AiAgentMapper, AiAgentDO> implements IAiAgentService {

    @Override
    public PageResult<AiAgentVO> getAgentPage(AiAgentQuery query) {
        Page<AiAgentDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AiAgentDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(AiAgentDO::getName, query.getName());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(AiAgentDO::getType, query.getType());
        }
        if (StringUtils.hasText(query.getPlatform())) {
            wrapper.eq(AiAgentDO::getPlatform, query.getPlatform());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AiAgentDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(AiAgentDO::getCreateTime);
        
        IPage<AiAgentDO> result = this.page(page, wrapper);
        
        // 转换为VO
        List<AiAgentVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public AiAgentVO getAgentById(Long id) {
        AiAgentDO agent = this.getById(id);
        return agent != null ? convertToVO(agent) : null;
    }

    @Override
    public Boolean addAgent(AiAgentDTO dto) {
        AiAgentDO agent = new AiAgentDO();
        BeanUtils.copyProperties(dto, agent);
        return this.save(agent);
    }

    @Override
    public Boolean updateAgent(AiAgentUpdateDTO dto) {
        AiAgentDO agent = new AiAgentDO();
        BeanUtils.copyProperties(dto, agent);
        return this.updateById(agent);
    }

    @Override
    public Boolean deleteAgent(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public Boolean updateAgentStatus(Long id, String status) {
        AiAgentDO agent = new AiAgentDO();
        agent.setId(id);
        agent.setStatus(status);
        return this.updateById(agent);
    }

    @Override
    public List<AiAgentVO> getAgentsByPlatform(String platform) {
        LambdaQueryWrapper<AiAgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgentDO::getPlatform, platform)
               .eq(AiAgentDO::getStatus, SystemConstants.Status.ACTIVE)
               .orderByDesc(AiAgentDO::getCreateTime);
        
        List<AiAgentDO> agents = this.list(wrapper);
        return agents.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<AiAgentVO> getAgentsByType(String type) {
        LambdaQueryWrapper<AiAgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgentDO::getType, type)
               .eq(AiAgentDO::getStatus, SystemConstants.Status.ACTIVE)
               .orderByDesc(AiAgentDO::getCreateTime);
        
        List<AiAgentDO> agents = this.list(wrapper);
        return agents.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 转换为VO对象
     */
    private AiAgentVO convertToVO(AiAgentDO agent) {
        AiAgentVO vo = new AiAgentVO();
        BeanUtils.copyProperties(agent, vo);
        return vo;
    }
}