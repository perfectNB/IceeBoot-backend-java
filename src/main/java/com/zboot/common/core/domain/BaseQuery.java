/**
 * 基础查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.common.core.domain;

import lombok.Data;

/**
 * 基础查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class BaseQuery {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方向
     */
    private String orderDirection = "desc";

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 删除标识
     */
    private Integer delFlag = 0;
}