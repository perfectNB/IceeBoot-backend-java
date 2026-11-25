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
import org.springframework.transaction.annotation.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemoryChangeService {
    private final AgentMemoryMapper mapper;
    private final AgentMemoryJobMapper jobs;
    private final MemorySettingsService settings;
    private final MemoryProfileService profiles;
    private final AgentJsonCodec json;
    private final AgentSecrets secrets;

    List<Target> targets(MemoryJobDO job) {
        return json.list(secrets.decrypt(job.getPolicySnapshotCiphertext()), Target.class);
    }

    Source source(MemoryJobDO job) {
        return json.read(secrets.decrypt(job.getRequestCiphertext()), Source.class);
    }

    Map<String, String> knownValues(MemoryJobDO job, Target target) {
        var scope = target.rule().scope();
        var row = profiles.find(scope, job.getSourceAgentId(), job.getSourceUserId());
        if (!matches(row, target)) return Map.of();
        var values = new LinkedHashMap<>(profiles.document(row).values());
        values.keySet().retainAll(target.writable());
        if (scope == MemoryScope.USER)
            values.keySet().retainAll(settings.grant(job.getSourceUserId(), job.getSourceAgentId()).readFields());
        return values;
    }

    boolean current(MemoryJobDO job, Target target) {
        var rule = settings.rule(target.rule().scope(), job.getSourceAgentId());
        if (!rule.enabled() || rule.revision() != target.policyRevision()) return false;
        if (rule.scope() != MemoryScope.USER) return true;
        var grant = settings.grant(job.getSourceUserId(), job.getSourceAgentId());
        return grant.revision() == target.grantRevision() && grant.writeFields().containsAll(target.writable());
    }

    private boolean authorizedAtWrite(MemoryJobDO job, Target target) {
        var scope = target.rule().scope();
        Long policyRevision = mapper.lockPolicy(scope.name(), scope.policyAgent(job.getSourceAgentId()));
        if (!Objects.equals(policyRevision, target.policyRevision())) return false;
        if (scope == MemoryScope.USER) {
            Long grantRevision = mapper.lockGrant(job.getSourceUserId(), job.getSourceAgentId());
            if (!Objects.equals(grantRevision, target.grantRevision())) return false;
        }
        return current(job, target);
    }

    /** 在同一事务中检查租约、锁定画像、写入候选和标记任务完成。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void complete(long jobId, long epoch, Extraction result) {
        MemoryJobDO job = jobs.locked(jobId, epoch);
        if (job == null) return;
        Source source = source(job);
        for (Target target : targets(job)) {
            var row = mapper.lockProfile(target.profileId());
            if (!matches(row, target) || !authorizedAtWrite(job, target)) continue;
            Document doc = profiles.document(row);
            var values = new LinkedHashMap<String, String>();
            var evidence = new LinkedHashMap<String, Evidence>();
            for (Suggestion item : result.updates()) {
                if (!eligible(item, target, source, doc)) continue;
                values.put(item.field(), item.value());
                evidence.put(item.field(), new Evidence(source.messageId(), item.quote()));
            }
            if (values.isEmpty()) continue;
            var change = profiles.change(row, jobId, profiles.encrypt(values), profiles.encrypt(evidence));
            mapper.insertChange(change, "MODEL", "");
            if (!target.rule().reviewRequired()) {
                doc.values().putAll(values);
                doc.evidence().putAll(evidence);
                profiles.write(row, doc, false, change, "");
            }
        }
        jobs.finish(jobId, epoch, "SUCCEEDED", 0, "");
    }

    static boolean eligible(Suggestion item, Target target, Source source, Document doc) {
        if (item == null || item.scope() != target.rule().scope() || item.field() == null
                || !target.writable().contains(item.field()) || doc.forgotten().contains(item.field())) return false;
        FieldType type = target.rule().fields().get(item.field());
        if (type == null || !MemoryProfileService.validValue(type, item.value())) return false;
        if (item.quote() == null || item.quote().isBlank() || item.quote().length() > 400
                || !source.text().contains(item.quote())) return false;
        Evidence previous = doc.evidence().get(item.field());
        if (previous != null && previous.messageId() >= source.messageId()) return false;
        return !Objects.equals(doc.values().get(item.field()), item.value());
    }

    private boolean matches(MemoryProfileDO row, Target target) {
        return row != null && row.getRevision() == target.revision() && row.getForgetEpoch() == target.forgetEpoch()
                && "ACTIVE".equals(row.getStatus());
    }

    public List<MemoryView.Candidate> pending(MemoryScope scope, long agent, String user) {
        MemoryProfileDO row = profiles.find(scope, agent, user);
        if (row == null) return List.of();
        var result = new ArrayList<MemoryView.Candidate>();
        for (MemoryChangeDO change : mapper.pending(row.getId())) {
            var quotes = new LinkedHashMap<String, String>();
            json.map(secrets.decrypt(change.getEvidenceCiphertext()), Evidence.class)
                    .forEach((field, evidence) -> quotes.put(field, evidence.quote()));
            result.add(new MemoryView.Candidate(change.getId(),
                    json.map(secrets.decrypt(change.getPatchCiphertext()), String.class), quotes));
        }
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean review(MemoryScope scope, long agent, String user, long changeId, boolean approve) {
        return reviewOwned(scope, agent, user, user, changeId, approve);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean reviewUser(String user, String actor, long changeId, boolean approve) {
        return reviewOwned(MemoryScope.USER, 0, user, actor, changeId, approve);
    }

    private boolean reviewOwned(MemoryScope scope, long agent, String user, String actor, long changeId, boolean approve) {
        var owner = profiles.find(scope, agent, user);
        if (owner == null) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
        var row = mapper.lockProfile(owner.getId());
        var change = mapper.lockChange(row.getId(), changeId);
        if (change == null) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
        MemorySettingsService.checkRevision("PENDING".equals(change.getStatus()));
        if (approve) {
            var job = jobs.get(change.getJobId());
            for (Target target : targets(job)) {
                if (target.profileId() != row.getId() || !matches(row, target) || !authorizedAtWrite(job, target)) continue;
                Document doc = profiles.document(row);
                var patch = json.map(secrets.decrypt(change.getPatchCiphertext()), String.class);
                var evidence = json.map(secrets.decrypt(change.getEvidenceCiphertext()), Evidence.class);
                Source source = source(job);
                boolean valid = true;
                for (var entry : patch.entrySet()) {
                    var proof = evidence.get(entry.getKey());
                    if (proof == null || proof.messageId() != source.messageId() || !eligible(
                            new Suggestion(scope, entry.getKey(), entry.getValue(), proof.quote()), target, source, doc)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) continue;
                doc.values().putAll(patch);
                doc.evidence().putAll(evidence);
                profiles.write(row, doc, false, change, actor);
                return true;
            }
        }
        mapper.decide(changeId, approve ? "STALE" : "REJECTED", 0, actor, System.currentTimeMillis());
        return !approve;
    }
}
