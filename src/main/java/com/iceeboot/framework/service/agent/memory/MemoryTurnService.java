package com.iceeboot.framework.service.agent.memory;

import com.iceeboot.framework.model.agent.PublishedAgent;

import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.memory.MemoryData.*;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemoryTurnService {
    private static final int MAX_CONTEXT_CHARS = 6000;
    private final MemoryProfileService profiles;
    private final MemorySettingsService settings;
    private final AgentMemoryJobMapper jobs;
    private final AgentConversationMapper messages;
    private final AgentJsonCodec json;

    /** 每个根对话轮次只读一次。读权限与提议写入权限分别过滤。 */
    @Transactional
    public Turn prepare(PublishedAgent release, String user) {
        long agent = release.agent().id();
        var visible = new LinkedHashMap<MemoryScope, Map<String, String>>();
        var targets = new ArrayList<Target>();
        for (MemoryScope scope : MemoryScope.values()) {
            Rule live = settings.rule(scope, agent);
            if (!live.enabled()) continue;
            Rule rule = live;
            if (scope != MemoryScope.USER) {
                rule = release.memoryRules().stream().filter(item -> item.scope() == scope).findFirst().orElse(null);
                if (rule == null || !rule.enabled()) continue;
            }
            Set<String> readable = rule.fields().keySet();
            Set<String> writable = new LinkedHashSet<>(readable);
            long grantRevision = 0;
            if (scope == MemoryScope.USER) {
                var grant = settings.grant(user, agent);
                readable = grant.readFields();
                writable.retainAll(grant.writeFields());
                grantRevision = grant.revision();
            }
            if (readable.isEmpty() && writable.isEmpty()) continue;
            var row = profiles.ensure(scope, agent, user);
            if (!"ACTIVE".equals(row.getStatus())) continue;
            Document doc = profiles.document(row);
            var values = new LinkedHashMap<>(doc.values());
            values.keySet().retainAll(readable);
            values.keySet().retainAll(rule.fields().keySet());
            values.keySet().removeAll(doc.forgotten());
            if (!values.isEmpty()) {
                // 优先保留专属偏好，其次为通用偏好，最后才是公共经验。
                var limited = new LinkedHashMap<String, String>();
                visible.put(scope, limited);
                for (var entry : values.entrySet()) {
                    limited.put(entry.getKey(), entry.getValue());
                    if (json.json(visible).length() > MAX_CONTEXT_CHARS) {
                        limited.remove(entry.getKey());
                        break;
                    }
                }
                if (limited.isEmpty()) visible.remove(scope);
            }
            writable.removeAll(doc.forgotten());
            if (!writable.isEmpty()) targets.add(new Target(rule, row.getId(), row.getRevision(),
                    row.getForgetEpoch(), writable, grantRevision, live.revision()));
        }
        return new Turn(visible.isEmpty() ? "" : json.json(visible), targets);
    }

    public void enqueue(AgentSessionDO session, String run, Turn turn) {
        if (turn.targets().isEmpty() || jobs.exists(run) != 0) return;
        var rows = messages.turn(session.getAgentId(), session.getUserId(), session.getSessionId(), run);
        for (AgentMessageDO message : rows) {
            if (!"user".equals(message.getRole())) continue;
            var job = new MemoryJobDO();
            job.setSourceRunId(run);
            job.setSourceAgentId(session.getAgentId());
            job.setSourceUserId(session.getUserId());
            job.setSourceSessionId(session.getSessionId());
            job.setSourceReleaseId(session.getReleaseId());
            job.setPolicySnapshotCiphertext(profiles.encrypt(turn.targets()));
            job.setRequestCiphertext(profiles.encrypt(new Source(message.getId(), message.getContent())));
            jobs.insert(job, "MODEL_TURN", "PENDING");
            return;
        }
    }
}
