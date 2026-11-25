package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.*;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.update.agent.MemoryRequest;
import com.iceeboot.framework.query.agent.MemoryUserQuery;
import com.iceeboot.framework.service.agent.IAgentUserProfileService;
import com.iceeboot.framework.vo.agent.MemoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "通用画像管理")
@RequestMapping("/v1/agent-console/memory/users")
public class AgentUserProfileController {
    private final IAgentUserProfileService profiles;

    private long actor() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null) throw new IceeBootException(403, ResponseMessage.Agent.PERMISSION_DENIED);
        return id;
    }

    @GetMapping
    @RequirePermission(dynamic = true)
    @Operation(summary = "分页查询画像用户")
    public R<PageResult<MemoryView.User>> users(MemoryUserQuery query) { return R.ok(profiles.users(actor(), query)); }

    @GetMapping("/{userId}/profile")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看指定用户通用画像")
    public R<MemoryView.Profile> profile(@PathVariable long userId) { return R.ok(profiles.profile(actor(), userId)); }

    @PutMapping("/{userId}/fields/{field}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "修改指定用户通用画像")
    public R<Void> edit(@PathVariable long userId, @PathVariable String field, @Valid @RequestBody MemoryRequest.FieldDTO request) {
        profiles.edit(actor(), userId, field, request.value(), request.revision());
        return R.ok();
    }

    @DeleteMapping("/{userId}/fields/{field}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "忘记指定用户通用画像字段")
    public R<Void> forget(@PathVariable long userId, @PathVariable String field, @RequestParam long revision) {
        profiles.forget(actor(), userId, field, revision);
        return R.ok();
    }

    @GetMapping("/{userId}/reviews")
    @RequirePermission(dynamic = true)
    @Operation(summary = "查看指定用户画像候选")
    public R<List<MemoryView.Candidate>> pending(@PathVariable long userId) { return R.ok(profiles.pending(actor(), userId)); }

    @PutMapping("/{userId}/reviews/{id}")
    @RequirePermission(dynamic = true)
    @Operation(summary = "审核指定用户画像候选")
    public R<Boolean> review(@PathVariable long userId, @PathVariable long id, @Valid @RequestBody MemoryRequest.ReviewDTO request) {
        return R.ok(profiles.review(actor(), userId, id, request.approve()));
    }
}
