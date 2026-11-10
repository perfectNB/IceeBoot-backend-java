package com.iceeboot.framework.controller.message;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ApplicationConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.message.NotificationDTO;
import com.iceeboot.framework.query.message.NotificationQuery;
import com.iceeboot.framework.service.message.INotificationService;
import com.iceeboot.framework.vo.message.NotificationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.iceeboot.framework.vo.message.NotificationApplicationVO;
import com.iceeboot.config.message.NotificationProperties;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final INotificationService notificationService;
    private final NotificationProperties notificationProperties;

    @GetMapping("/applications")
    @RequirePermission(dynamic = true)
    public R<List<NotificationApplicationVO>> applications() {
        return R.ok(Stream.concat(
                Stream.of(new NotificationApplicationVO(ApplicationConstants.ALL, "全平台")),
                notificationProperties.getApplicationCodes().stream()
                        .map(code -> new NotificationApplicationVO(code, code))).toList());
    }

    @GetMapping
    @RequirePermission(dynamic = true)
    public R<PageResult<NotificationVO>> page(NotificationQuery query) {
        return R.ok(notificationService.getAdminPage(query));
    }

    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Long> save(@Valid @RequestBody NotificationDTO dto) {
        return R.ok(notificationService.saveDraft(dto));
    }

    @PutMapping("/{id}/publish")
    @RequirePermission(dynamic = true)
    public R<Boolean> publish(@PathVariable Long id) {
        return R.ok(notificationService.publish(id));
    }

    @PutMapping("/{id}/withdraw")
    @RequirePermission(dynamic = true)
    public R<Boolean> withdraw(@PathVariable Long id) {
        return R.ok(notificationService.withdraw(id));
    }

    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.ok(notificationService.delete(ids));
    }
}
