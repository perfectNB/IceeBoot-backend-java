package com.iceeboot.framework.controller.workflow;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.core.domain.PageResult;
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

    @GetMapping(value = "/drafts", params = {"!page", "!code"})
    @RequirePermission(dynamic = true)
    public R<List<Blueprint>> drafts() {
        return R.ok(designs.designs(false));
    }

    @GetMapping(value = "/published", params = "page")
    @RequirePermission(dynamic = true)
    public R<PageResult<TemplateRow>> publishedPage(@RequestParam int page, @RequestParam int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(designs.page(true, page, size, keyword));
    }

    @GetMapping(value = "/published", params = {"code", "!page"})
    @RequirePermission(dynamic = true)
    public R<Blueprint> publishedDetail(@RequestParam String code) {
        return R.ok(designs.detail(code, true));
    }

    @GetMapping(value = "/drafts", params = "page")
    @RequirePermission(dynamic = true)
    public R<PageResult<TemplateRow>> draftsPage(@RequestParam int page, @RequestParam int size) {
        return R.ok(designs.page(false, page, size));
    }

    @GetMapping(value = "/drafts", params = {"code", "!page"})
    @RequirePermission(dynamic = true)
    public R<Blueprint> draftsDetail(@RequestParam String code, @RequestParam(defaultValue="false") boolean released) {
        return R.ok(designs.managementDetail(code, released));
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

    @PutMapping("/drafts/{code}/intake")
    @RequirePermission(dynamic = true)
    public R<Void> intake(@PathVariable String code, @jakarta.validation.Valid @RequestBody Intake request) {
        designs.setStopped(code, request.revision(), request.stopped());
        return R.ok();
    }

    @GetMapping(value = "/published", params = {"!page", "!code"})
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
