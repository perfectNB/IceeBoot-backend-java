package com.iceeboot.framework.controller.message;

import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.query.message.NotificationQuery;
import com.iceeboot.framework.service.message.INotificationService;
import com.iceeboot.framework.vo.message.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    public R<PageResult<NotificationVO>> page(@RequestParam String applicationCode,
                                               NotificationQuery query) {
        return R.ok(notificationService.getUserPage(applicationCode, query));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount(@RequestParam String applicationCode) {
        return R.ok(notificationService.getUnreadCount(applicationCode));
    }

    @GetMapping("/important-unread")
    public R<NotificationVO> latestUnreadImportant(@RequestParam String applicationCode) {
        return R.ok(notificationService.getLatestUnreadImportant(applicationCode));
    }

    @PutMapping("/{id}/read")
    public R<Boolean> read(@PathVariable Long id, @RequestParam String applicationCode) {
        return R.ok(notificationService.markRead(id, applicationCode));
    }

    @PutMapping("/read-all")
    public R<Boolean> readAll(@RequestParam String applicationCode) {
        return R.ok(notificationService.markAllRead(applicationCode));
    }
}
