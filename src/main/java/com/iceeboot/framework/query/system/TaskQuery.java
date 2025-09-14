package com.iceeboot.framework.query.system;

import com.iceeboot.common.core.domain.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQuery extends BaseQuery {

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务状态
     */
    private String status;
}