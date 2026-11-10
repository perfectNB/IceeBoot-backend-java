package com.iceeboot.framework.service.agent.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.agent.*;
import com.iceeboot.framework.dto.update.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.vo.agent.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/** 接口 DTO、响应 VO 与内部运行时模型的转换。 */
@Component
@RequiredArgsConstructor
public class AgentApiConverter {
    private final ObjectMapper json;

    public <T> T convert(Object value, Class<T> type) {
        try {
            return json.convertValue(value, type);
        } catch (IllegalArgumentException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(), ResponseMessage.Agent.API_CONVERSION_FAILED, e);
        }
    }

    public Capability capability(AgentSkillDTO request, Long id) {
        return new Capability(
                id,
                CapabilityKind.SKILL,
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.isEnabled(),
                new SkillConfig(request.getContent(), request.getResources(), request.getToolIds()),
                null,
                null,
                null);
    }

    public AgentSkillVO skillVO(Capability value) {
        AgentSkillVO result = convert(value.skill(), AgentSkillVO.class);
        result.setId(value.id());
        result.setCode(value.code());
        result.setName(value.name());
        result.setDescription(value.description());
        result.setEnabled(value.enabled());
        return result;
    }

    public Capability capability(AgentToolDTO request, Long id) {
        return new Capability(
                id,
                CapabilityKind.TOOL,
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.isEnabled(),
                null,
                new ToolConfig(
                        request.getKind(),
                        request.getUrl(),
                        request.getMethod(),
                        request.getHeaders(),
                        request.getInputSchema(),
                        request.getScript(),
                        request.isProjectApi(),
                        request.getPermissionCode()),
                null,
                null);
    }

    public AgentToolVO toolVO(Capability value) {
        AgentToolVO result = convert(value.tool(), AgentToolVO.class);
        result.setId(value.id());
        result.setCode(value.code());
        result.setName(value.name());
        result.setDescription(value.description());
        result.setEnabled(value.enabled());
        return result;
    }

    public Capability capability(AgentMcpDTO request, Long id) {
        return new Capability(
                id,
                CapabilityKind.MCP,
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.isEnabled(),
                null,
                null,
                new McpConfig(request.getUrl(), request.getHeaders(), request.getAllowedTools()),
                null);
    }

    public AgentMcpVO mcpVO(Capability value) {
        AgentMcpVO result = convert(value.mcp(), AgentMcpVO.class);
        result.setId(value.id());
        result.setCode(value.code());
        result.setName(value.name());
        result.setDescription(value.description());
        result.setEnabled(value.enabled());
        return result;
    }

    public Capability capability(AgentSandboxTemplateDTO request, Long id) {
        return new Capability(
                id,
                CapabilityKind.SANDBOX,
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.isEnabled(),
                null,
                null,
                null,
                new SandboxConfig(request.getTemplateId(), request.getTimeoutSeconds()));
    }

    public AgentSandboxTemplateVO sandboxVO(Capability value) {
        AgentSandboxTemplateVO result = convert(value.sandbox(), AgentSandboxTemplateVO.class);
        result.setId(value.id());
        result.setCode(value.code());
        result.setName(value.name());
        result.setDescription(value.description());
        result.setEnabled(value.enabled());
        return result;
    }

    public Capability capability(AgentKnowledgeDTO request, Long id) {
        return new Capability(
                id,
                CapabilityKind.KNOWLEDGE,
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.isEnabled(),
                null,
                new ToolConfig(
                        request.getKind(),
                        request.getUrl(),
                        request.getMethod(),
                        request.getHeaders(),
                        request.getInputSchema(),
                        request.getScript(),
                        request.isProjectApi(),
                        request.getPermissionCode()),
                null,
                null);
    }

    public AgentKnowledgeVO knowledgeVO(Capability value) {
        AgentKnowledgeVO result = convert(value.tool(), AgentKnowledgeVO.class);
        result.setId(value.id());
        result.setCode(value.code());
        result.setName(value.name());
        result.setDescription(value.description());
        result.setEnabled(value.enabled());
        return result;
    }
}
