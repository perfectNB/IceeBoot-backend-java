package com.iceeboot.framework.service.agent.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.agent.AgentSandboxTemplateDTO;
import com.iceeboot.framework.dto.update.agent.AgentSandboxTemplateUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentSandboxTemplateDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentSandboxTemplateMapper;
import com.iceeboot.framework.model.agent.Capability;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentSandboxTemplateVO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 智能体沙箱模板管理服务实现。控制器仅使用 DTO/VO，跨资源规则复用配置服务。 */
@Service
@RequiredArgsConstructor
public class AgentSandboxTemplateServiceImpl
        extends ServiceImpl<AgentSandboxTemplateMapper, AgentSandboxTemplateDO>
        implements IAgentSandboxTemplateService {
    private final IAgentConfigurationService configs;
    private final IAgentPersistenceService store;
    private final AgentApiConverter converter;

    @Override
    public List<AgentSandboxTemplateVO> getResourceList() {
        List<Capability> values = configs.capabilities(CapabilityKind.SANDBOX);
        if (values.isEmpty()) return List.of();
        Map<Long, AgentSandboxTemplateDO> rows =
                listByIds(values.stream().map(Capability::id).toList()).stream()
                        .collect(
                                Collectors.toMap(
                                        AgentSandboxTemplateDO::getId, Function.identity()));
        return values.stream().map(value -> toVO(value, rows.get(value.id()))).toList();
    }

    private Capability resource(long id) {
        Capability value = configs.capability(id);
        if (value.kind() != CapabilityKind.SANDBOX)
            throw new IceeBootException(ResponseMessage.Agent.RESOURCE_TYPE_MISMATCH);
        return value;
    }

    private AgentSandboxTemplateVO toVO(Capability value, AgentSandboxTemplateDO row) {
        if (row == null)
            throw new IceeBootException(ResponseMessage.Agent.RESOURCE_DETAIL_NOT_FOUND);
        AgentSandboxTemplateVO vo = converter.sandboxVO(value);
        vo.setCreateTime(row.getCreateTime());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }

    @Override
    public AgentSandboxTemplateVO getResourceById(long id) {
        return toVO(resource(id), getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSandboxTemplateVO addResource(AgentSandboxTemplateDTO dto) {
        Capability saved = configs.saveCapability(converter.capability(dto, null));
        return toVO(saved, getById(saved.id()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSandboxTemplateVO updateResource(AgentSandboxTemplateUpdateDTO dto) {
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
