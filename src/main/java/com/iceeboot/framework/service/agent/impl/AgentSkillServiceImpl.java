package com.iceeboot.framework.service.agent.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.agent.AgentSkillDTO;
import com.iceeboot.framework.dto.update.agent.AgentSkillUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentSkillDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentSkillMapper;
import com.iceeboot.framework.model.agent.Capability;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentSkillVO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 智能体技能管理服务实现。控制器仅使用 DTO/VO，跨资源规则复用配置服务。 */
@Service
@RequiredArgsConstructor
public class AgentSkillServiceImpl extends ServiceImpl<AgentSkillMapper, AgentSkillDO>
        implements IAgentSkillService {
    private final IAgentConfigurationService configs;
    private final IAgentPersistenceService store;
    private final AgentApiConverter converter;

    @Override
    public List<AgentSkillVO> getResourceList() {
        List<Capability> values = configs.capabilities(CapabilityKind.SKILL);
        if (values.isEmpty()) return List.of();
        Map<Long, AgentSkillDO> rows =
                listByIds(values.stream().map(Capability::id).toList()).stream()
                        .collect(Collectors.toMap(AgentSkillDO::getId, Function.identity()));
        return values.stream().map(value -> toVO(value, rows.get(value.id()))).toList();
    }

    private Capability resource(long id) {
        Capability value = configs.capability(id);
        if (value.kind() != CapabilityKind.SKILL)
            throw new IceeBootException(ResponseMessage.Agent.RESOURCE_TYPE_MISMATCH);
        return value;
    }

    private AgentSkillVO toVO(Capability value, AgentSkillDO row) {
        if (row == null)
            throw new IceeBootException(ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND);
        AgentSkillVO vo = converter.skillVO(value);
        vo.setCreateTime(row.getCreateTime());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }

    @Override
    public AgentSkillVO getResourceById(long id) {
        return toVO(resource(id), getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSkillVO addResource(AgentSkillDTO dto) {
        Capability saved = configs.saveCapability(converter.capability(dto, null));
        return toVO(saved, getById(saved.id()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSkillVO updateResource(AgentSkillUpdateDTO dto) {
        resource(dto.getId());
        Capability saved = configs.saveCapability(converter.capability(dto, dto.getId()));
        return toVO(saved, getById(saved.id()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(long id) {
        resource(id);
        store.deleteCapability(id);
    }
}
