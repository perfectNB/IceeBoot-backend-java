package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.model.agent.Capability;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.*;

import java.util.*;

/** 只读发布快照视图，脚本资源由 Harness 按需投影至所选沙箱。 */
public final class SnapshotSkillRepository implements AgentSkillRepository {
    private final Map<String, AgentSkill> skills = new LinkedHashMap<>();

    public SnapshotSkillRepository(List<Capability> definitions) {
        for (var c : definitions) {
            var s = c.skill();
            AgentSkill skill =
                    AgentSkill.builder()
                            .name(c.code())
                            .description(c.description() == null || c.description().isBlank() ? c.name() : c.description())
                            .skillContent(s.content())
                            .resources(s.resources() == null ? Map.of() : s.resources())
                            .source("iceeboot-release")
                            .build();
            skills.put(c.code(), skill);
        }
    }

    @Override
    public AgentSkill getSkill(String name) {
        return skills.get(name);
    }

    @Override
    public List<String> getAllSkillNames() {
        return List.copyOf(skills.keySet());
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        return List.copyOf(skills.values());
    }

    @Override
    public boolean skillExists(String name) {
        return skills.containsKey(name);
    }

    @Override
    public boolean save(List<AgentSkill> values, boolean overwrite) {
        throw new IceeBootException(ResponseMessage.Agent.RELEASE_RESOURCE_READ_ONLY);
    }

    @Override
    public boolean delete(String name) {
        throw new IceeBootException(ResponseMessage.Agent.RELEASE_RESOURCE_READ_ONLY);
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return new AgentSkillRepositoryInfo("snapshot", "iceeboot-release", false);
    }

    @Override
    public String getSource() {
        return "iceeboot-release";
    }

    @Override
    public void setWriteable(boolean value) {
        if (value) throw new IceeBootException(ResponseMessage.Agent.RELEASE_RESOURCE_READ_ONLY);
    }

    @Override
    public boolean isWriteable() {
        return false;
    }
}
