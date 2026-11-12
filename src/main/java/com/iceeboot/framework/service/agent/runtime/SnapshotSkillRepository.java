package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.model.agent.Capability;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.*;

import java.util.*;

/** 只读发布快照视图，脚本资源由 Harness 按需投影至所选沙箱。 */
public final class SnapshotSkillRepository implements AgentSkillRepository {
    private static final int MAX_CATALOG_DESCRIPTION = 320;
    private final Map<String, AgentSkill> skills = new LinkedHashMap<>();

    public SnapshotSkillRepository(List<Capability> definitions) {
        for (var c : definitions) {
            var s = c.skill();
            AgentSkill skill =
                    AgentSkill.builder()
                            .name(c.code())
                            .description(catalogDescription(c))
                            .skillContent(s.content())
                            .resources(s.resources() == null ? Map.of() : s.resources())
                            .source("iceeboot-release")
                            .build();
            skills.put(c.code(), skill);
        }
    }

    private static String catalogDescription(Capability capability) {
        String value = capability.description() == null || capability.description().isBlank() ? capability.name() : capability.description();
        int end = value.offsetByCodePoints(0, Math.min(value.codePointCount(0,value.length()), MAX_CATALOG_DESCRIPTION));
        return end == value.length() ? value : value.substring(0,end) + "…（加载技能后查看完整说明）";
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
