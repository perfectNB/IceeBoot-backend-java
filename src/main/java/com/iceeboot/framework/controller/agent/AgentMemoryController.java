package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.update.agent.MemoryRequest;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.memory.*;
import com.iceeboot.framework.vo.agent.MemoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "长期记忆")
@RequestMapping("/v1/agent-console/memory")
public class AgentMemoryController {
    private final MemorySettingsService settings;
    private final MemoryProfileService profiles;
    private final MemoryChangeService changes;
    private final IAgentPersistenceService store;

    private String owner() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
        return id.toString();
    }

    private void personal(MemoryScope scope) {
        if (scope == MemoryScope.AGENT) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
    }

    @GetMapping("/agents")
    @RequirePermission(dynamic = true)
    @Operation(summary = "智能体选项")
    public R<List<MemoryView.Agent>> agents() {
        return R.ok(store.agents().stream().map(view -> new MemoryView.Agent(view.draft().id(), view.draft().name())).toList());
    }

    @GetMapping("/policies/{scope}/{agent}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看画像策略")
    public R<MemoryView.Policy> policy(@PathVariable MemoryScope scope, @PathVariable long agent) {
        if (scope == MemoryScope.USER) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
        return policyView(scope, agent);
    }

    private R<MemoryView.Policy> policyView(MemoryScope scope, long agent) {
        var rule = settings.rule(scope, agent);
        return R.ok(new MemoryView.Policy(rule.revision(), rule.enabled(), rule.reviewRequired(), rule.prompt(), rule.fields()));
    }

    @PutMapping("/policies/{scope}/{agent}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "配置画像策略")
    public R<Void> policy(@PathVariable MemoryScope scope, @PathVariable long agent, @Valid @RequestBody MemoryRequest.PolicyDTO request) {
        if (scope == MemoryScope.USER) throw new IceeBootException(ResponseMessage.Agent.PERMISSION_DENIED);
        settings.save(scope, agent, request);
        return R.ok();
    }

    @GetMapping("/platform-policy")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看平台通用画像策略")
    public R<MemoryView.Policy> platformPolicy() {
        return policyView(MemoryScope.USER, 0);
    }

    @PutMapping("/platform-policy")
    @RequirePermission(dynamic = true)
    @Operation(summary = "配置平台通用画像策略")
    public R<Void> platformPolicy(@Valid @RequestBody MemoryRequest.PolicyDTO request) {
        settings.save(MemoryScope.USER, 0, request);
        return R.ok();
    }

    @GetMapping("/profiles/{scope}/{agent}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看记忆")
    public R<MemoryView.Profile> profile(@PathVariable MemoryScope scope, @PathVariable long agent) {
        return R.ok(profiles.view(scope, agent, owner()));
    }

    @PutMapping("/profiles/{scope}/{agent}/fields/{field}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "修改我的记忆")
    public R<Void> edit(@PathVariable MemoryScope scope, @PathVariable long agent, @PathVariable String field, @Valid @RequestBody MemoryRequest.FieldDTO request) {
        personal(scope);
        profiles.edit(scope, agent, owner(), field, request.value(), request.revision());
        return R.ok();
    }

    @DeleteMapping("/profiles/{scope}/{agent}/fields/{field}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "忘记我的记忆")
    public R<Void> forget(@PathVariable MemoryScope scope, @PathVariable long agent, @PathVariable String field, @RequestParam long revision) {
        personal(scope);
        profiles.edit(scope, agent, owner(), field, null, revision);
        return R.ok();
    }

    @PutMapping("/shared/{agent}/fields/{field}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "修改智能体公共经验")
    public R<Void> editShared(@PathVariable long agent, @PathVariable String field, @Valid @RequestBody MemoryRequest.FieldDTO request) {
        profiles.edit(MemoryScope.AGENT, agent, owner(), field, request.value(), request.revision());
        return R.ok();
    }

    @DeleteMapping("/shared/{agent}/fields/{field}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "删除智能体公共经验")
    public R<Void> forgetShared(@PathVariable long agent, @PathVariable String field, @RequestParam long revision) {
        profiles.edit(MemoryScope.AGENT, agent, owner(), field, null, revision);
        return R.ok();
    }

    @GetMapping("/grants/{agent}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看我的共享授权")
    public R<MemoryView.Grant> grant(@PathVariable long agent) {
        return R.ok(settings.grant(owner(), agent));
    }

    @PutMapping("/grants/{agent}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "设置我的共享授权")
    public R<Void> share(@PathVariable long agent, @Valid @RequestBody MemoryRequest.GrantDTO request) {
        settings.share(owner(), agent, request);
        return R.ok();
    }

    @GetMapping("/reviews/{scope}/{agent}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看我的待确认记忆")
    public R<List<MemoryView.Candidate>> pending(@PathVariable MemoryScope scope, @PathVariable long agent) {
        personal(scope);
        return R.ok(changes.pending(scope, agent, owner()));
    }

    @PutMapping("/reviews/{scope}/{agent}/{id}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "确认我的记忆")
    public R<Boolean> review(@PathVariable MemoryScope scope, @PathVariable long agent, @PathVariable long id, @RequestBody MemoryRequest.ReviewDTO request) {
        personal(scope);
        return R.ok(changes.review(scope, agent, owner(), id, request.approve()));
    }

    @GetMapping("/shared/{agent}/reviews")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看公共经验候选")
    public R<List<MemoryView.Candidate>> pendingShared(@PathVariable long agent) {
        return R.ok(changes.pending(MemoryScope.AGENT, agent, owner()));
    }

    @PutMapping("/shared/{agent}/reviews/{id}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "审核公共经验")
    public R<Boolean> reviewShared(@PathVariable long agent, @PathVariable long id, @RequestBody MemoryRequest.ReviewDTO request) {
        return R.ok(changes.review(MemoryScope.AGENT, agent, owner(), id, request.approve()));
    }
}
