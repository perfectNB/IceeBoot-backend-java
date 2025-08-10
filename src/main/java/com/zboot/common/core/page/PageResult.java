/*
 * @Description: 分页结果类
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot.common.core.page;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果类
 * 
 * @author CodeIcee
 * @since 2025-08-11
 */
@Data
@Accessors(chain = true)
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private long page;

    /**
     * 每页大小
     */
    private long size;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否为第一页
     */
    private boolean isFirst;

    /**
     * 是否为最后一页
     */
    private boolean isLast;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long page, long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (total + size - 1) / size;
        this.hasPrevious = page > 1;
        this.hasNext = page < pages;
        this.isFirst = page == 1;
        this.isLast = page == pages;
    }

    /**
     * 创建分页结果
     *
     * @param list 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 创建空分页结果
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(List.of(), 0, page, size);
    }
}