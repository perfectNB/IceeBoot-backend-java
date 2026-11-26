package com.iceeboot.framework.controller.workflow;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.update.workflow.ApprovalRequests.*;
import com.iceeboot.framework.service.workflow.ApprovalCaseService;
import com.iceeboot.framework.service.workflow.ApprovalDesignService;
import com.iceeboot.framework.vo.workflow.ApprovalViews.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/workflow/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalDesignService designs;
    private final ApprovalCaseService cases;

    @GetMapping("/drafts")
    @RequirePermission(dynamic = true)
    public R<List<Blueprint>> drafts() {
        return R.ok(designs.designs(false));
    }

    @GetMapping("/roles")
    @RequirePermission(dynamic = true)
    public R<List<Role>> roles() {
        return R.ok(designs.roles());
    }

    @PutMapping("/drafts")
    @RequirePermission(dynamic = true)
    public R<Void> save(@RequestBody Draft request) {
        designs.save(request);
        return R.ok();
    }

    @PostMapping("/drafts/{code}/publish")
    @RequirePermission(dynamic = true)
    public R<Void> publish(@PathVariable String code, @RequestBody Version request) {
        designs.publish(code, request.revision());
        return R.ok();
    }

    @GetMapping("/published")
    @RequirePermission(dynamic = true)
    public R<List<Blueprint>> published() {
        return R.ok(designs.designs(true));
    }

    @PostMapping("/cases")
    @RequirePermission(dynamic = true)
    public R<String> submit(@RequestBody Submit request) {
        return R.ok("申请已提交", cases.submit(request));
    }

    @GetMapping("/cases")
    @RequirePermission(dynamic = true)
    public R<List<Item>> list(@RequestParam String scope) {
        return R.ok(cases.list(scope));
    }

    @GetMapping("/cases/{id}")
    @RequirePermission(dynamic = true)
    public R<Detail> detail(@PathVariable String id) {
        return R.ok(cases.detail(id));
    }

    @PostMapping("/cases/{id}/review/{action}")
    @RequirePermission(dynamic = true)
    public R<Void> review(@PathVariable String id, @PathVariable String action, @RequestBody Change request) {
        cases.review(id, action, request);
        return R.ok();
    }

    @PostMapping("/cases/{id}/owner/{action}")
    @RequirePermission(dynamic = true)
    public R<Void> revise(@PathVariable String id, @PathVariable String action, @RequestBody Change request) {
        cases.revise(id, action, request);
        return R.ok();
    }
}
