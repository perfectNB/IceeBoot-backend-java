package com.iceeboot.framework.query.message;

import com.iceeboot.common.core.domain.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQuery extends BaseQuery {
    private String applicationCode;
    private String type;
    private String status;
}
