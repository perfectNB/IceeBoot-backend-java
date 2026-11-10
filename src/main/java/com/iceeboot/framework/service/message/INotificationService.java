package com.iceeboot.framework.service.message;

import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.message.NotificationDTO;
import com.iceeboot.framework.query.message.NotificationQuery;
import com.iceeboot.framework.vo.message.NotificationVO;

import java.util.List;

public interface INotificationService {
    PageResult<NotificationVO> getAdminPage(NotificationQuery query);

    Long saveDraft(NotificationDTO dto);

    boolean publish(Long id);

    boolean withdraw(Long id);

    boolean delete(List<Long> ids);

    PageResult<NotificationVO> getUserPage(String applicationCode, NotificationQuery query);

    long getUnreadCount(String applicationCode);

    NotificationVO getLatestUnreadImportant(String applicationCode);

    boolean markRead(Long id, String applicationCode);

    boolean markAllRead(String applicationCode);


}
