package com.iceeboot.framework.service.agent.memory;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.memory.MemoryData.*;
import com.iceeboot.framework.service.agent.security.AgentSecrets;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;
import com.iceeboot.framework.vo.agent.MemoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemoryProfileService {
    private final AgentMemoryMapper profiles;
    private final AgentMemoryJobMapper jobs;
    private final MemorySettingsService settings;
    private final AgentJsonCodec json;
    private final AgentSecrets secrets;

    MemoryProfileDO find(MemoryScope scope, long agent, String user) {
        return profiles.profile(scope.name(), scope.policyAgent(agent), scope.owner(user));
    }

    MemoryProfileDO ensure(MemoryScope scope, long agent, String user) {
        MemoryProfileDO row = find(scope, agent, user);
        if (row != null) return row;
        profiles.ensureProfile(scope.name(), scope.policyAgent(agent), scope.owner(user), encrypt(new Document()));
        return profiles.current(scope.name(), scope.policyAgent(agent), scope.owner(user));
    }

    Document document(MemoryProfileDO row) {
        return json.read(secrets.decrypt(row.getProfileCiphertext()), Document.class);
    }

    String encrypt(Object value) {
        return secrets.encrypt(json.json(value));
    }

    public MemoryView.Profile view(MemoryScope scope, long agent, String user) {
        var row = find(scope, agent, user);
        if (row == null) return new MemoryView.Profile(0, Map.of(), settings.rule(scope, agent).fields(), Set.of());
        Document data = document(row);
        return new MemoryView.Profile(row.getRevision(), data.values(), settings.rule(scope, agent).fields(), data.forgotten());
    }

    /** 由用户操作触发。null 表示永久忽略该字段，只有用户手动填写才能恢复。 */
    @Transactional
    public void edit(MemoryScope scope, long agent, String user, String field, String value, long revision) {
        editOwned(scope, agent, user, user, field, value, revision, scope == MemoryScope.AGENT);
    }

    /** 管理员修改指定用户，画像归属与操作人分别记录。 */
    @Transactional
    public void editUser(String owner, String actor, String field, String value, long revision) {
        editOwned(MemoryScope.USER, 0, owner, actor, field, value, revision, true);
    }

    private void editOwned(MemoryScope scope, long agent, String user, String actor, String field, String value, long revision, boolean administrative) {
        Rule policy = settings.rule(scope, agent);
        FieldType type = policy.fields().get(field);
        if (type == null || (value != null && !validValue(type, value)))
            throw new IceeBootException(ResponseMessage.Agent.MEMORY_VALUE_INVALID);
        MemoryProfileDO before = find(scope, agent, user);
        MemorySettingsService.checkRevision((before == null ? 0 : before.getRevision()) == revision);
        MemoryProfileDO row = ensure(scope, agent, user);
        if (before == null) MemorySettingsService.checkRevision(row.getRevision() == 1);
        Document data = document(row);
        if (value == null) {
            data.values().remove(field);
            data.evidence().remove(field);
            data.forgotten().add(field);
        } else {
            data.values().put(field, value);
            data.evidence().put(field, new Evidence(0, "手动设置"));
            data.forgotten().remove(field);
        }
        MemoryJobDO job = new MemoryJobDO();
        job.setSourceRunId(UUID.randomUUID().toString());
        job.setSourceAgentId(scope.policyAgent(agent));
        job.setSourceUserId(user);
        job.setSourceSessionId("");
        job.setSourceReleaseId("");
        job.setRequestCiphertext(encrypt(List.of(field)));
        job.setPolicySnapshotCiphertext(encrypt(policy));
        jobs.insert(job, administrative ? "ADMIN_EDIT" : "USER_EDIT", "SUCCEEDED");
        // 用空对象表示删除，审计里不复制被删除的内容。
        var patch = value == null ? Map.of("forget", field) : Map.of(field, value);
        var change = change(row, job.getId(), encrypt(patch), encrypt(List.of(field)));
        profiles.insertChange(change, administrative ? "ADMIN" : "USER", actor);
        write(row, data, value == null, change, actor);
    }

    static boolean validValue(FieldType type, String value) {
        if (value == null || value.isBlank() || value.length() > 240) return false;
        return switch (type) {
            case TEXT -> true;
            case BOOLEAN -> value.equals("true") || value.equals("false");
            case NUMBER -> value.matches("-?(0|[1-9][0-9]{0,15})([.][0-9]{1,8})?");
        };
    }

    MemoryChangeDO change(MemoryProfileDO row, long jobId, String patch, String evidence) {
        var change = new MemoryChangeDO();
        change.setProfileId(row.getId());
        change.setJobId(jobId);
        change.setBaseRevision(row.getRevision());
        change.setBaseForgetEpoch(row.getForgetEpoch());
        change.setPatchCiphertext(patch);
        change.setEvidenceCiphertext(evidence);
        change.setStatus("PENDING");
        return change;
    }

    void write(MemoryProfileDO row, Document data, boolean forgot, MemoryChangeDO change, String actor) {
        MemorySettingsService.checkRevision(profiles.replace(row.getId(), row.getRevision(), encrypt(data), forgot ? 1 : 0) == 1);
        profiles.decide(change.getId(), "APPLIED", row.getRevision() + 1, actor, System.currentTimeMillis());
        profiles.expirePending(row.getId(), row.getRevision());
    }
}
