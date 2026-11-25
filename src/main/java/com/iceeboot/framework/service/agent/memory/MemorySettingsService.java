package com.iceeboot.framework.service.agent.memory;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.update.agent.MemoryRequest;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.mapper.agent.AgentMemoryMapper;
import com.iceeboot.framework.model.agent.memory.MemoryData.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;
import com.iceeboot.framework.vo.agent.MemoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemorySettingsService {
    private final AgentMemoryMapper mapper;
    private final AgentJsonCodec json;
    private final IAgentPersistenceService store;

    public List<Rule> releaseRules(long agent) {
        return List.of(rule(MemoryScope.AGENT_USER, agent), rule(MemoryScope.AGENT, agent));
    }

    public Rule rule(MemoryScope scope, long agent) {
        long owner = scope.policyAgent(agent);
        MemoryPolicyDO row = mapper.policy(scope.name(), owner);
        if (row == null) return new Rule(scope, owner, 0, false, true, "", Map.of());
        return new Rule(scope, owner, row.getRevision(), row.isEnabled(), row.isReviewRequired(),
                row.getPrompt(), json.map(row.getProfileSchema(), FieldType.class));
    }

    @Transactional
    public void save(MemoryScope scope, long agent, MemoryRequest.PolicyDTO request) {
        long modelId = scope == MemoryScope.USER ? 0 : store.agent(agent).draft().modelConfigId();
        Rule before = rule(scope, agent);
        checkRevision(before.revision() == request.revision());
        // 允许增加字段；已有字段更名或改类型需要单独迁移，以免丢失用户保存的数据。
        if (!request.fields().entrySet().containsAll(before.fields().entrySet()))
            throw new IceeBootException(ResponseMessage.Agent.MEMORY_SCHEMA_CHANGE);
        MemoryPolicyDO row = new MemoryPolicyDO();
        row.setScope(scope.name());
        row.setModelConfigId(modelId);
        row.setAgentId(before.agentId());
        row.setRevision(before.revision());
        row.setEnabled(request.enabled());
        row.setReviewRequired(scope == MemoryScope.AGENT || request.reviewRequired());
        row.setPrompt(request.prompt());
        row.setProfileSchema(json.json(request.fields()));
        try {
            if (before.revision() == 0) mapper.insertPolicy(row);
            else checkRevision(mapper.updatePolicy(row) == 1);
        } catch (DuplicateKeyException conflict) {
            throw stale();
        }
    }

    public MemoryView.Grant grant(String user, long agent) {
        MemoryGrantDO row = mapper.grant(user, agent);
        var available = rule(MemoryScope.USER, agent).fields().keySet();
        if (row == null) return new MemoryView.Grant(0, Set.of(), Set.of(), available);
        return new MemoryView.Grant(row.getRevision(),
                row.isCanRead() ? Set.copyOf(json.list(row.getReadFields(), String.class)) : Set.of(),
                row.isCanProposeUpdate() ? Set.copyOf(json.list(row.getWriteFields(), String.class)) : Set.of(), available);
    }

    @Transactional
    public void share(String user, long agent, MemoryRequest.GrantDTO request) {
        store.agent(agent);
        var fields = rule(MemoryScope.USER, agent).fields().keySet();
        if (!fields.containsAll(request.readFields()) || !fields.containsAll(request.writeFields()))
            throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID);
        var row = new MemoryGrantDO();
        row.setReadFields(json.json(request.readFields()));
        row.setWriteFields(json.json(request.writeFields()));
        row.setCanRead(!request.readFields().isEmpty());
        row.setCanProposeUpdate(!request.writeFields().isEmpty());
        row.setRevision(request.revision());
        try {
            if (request.revision() == 0) mapper.insertGrant(user, agent, row);
            else checkRevision(mapper.updateGrant(user, agent, row) == 1);
        } catch (DuplicateKeyException conflict) {
            throw stale();
        }
    }

    static void checkRevision(boolean matches) {
        if (!matches) throw stale();
    }

    private static IceeBootException stale() {
        return new IceeBootException(409, ResponseMessage.Agent.MEMORY_CHANGED);
    }
}
