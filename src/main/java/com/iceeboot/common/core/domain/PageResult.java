/**
 * 分页结果对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.common.core.domain;

import lombok.Data;

import java.util.List;

/**
 * 分页结果对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class PageResult<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页数量
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 构造函数
     */
    public PageResult() {
    }

    /**
     * 构造函数
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页数量
     */
    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = (total + size - 1) / size;
        this.hasPrevious = current > 1;
        this.hasNext = current < pages;
    }

    /**
     * 创建空的分页结果
     *
     * @param <T> 泛型类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }

    /**
     * 创建分页结果
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页数量
     * @param <T>     泛型类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        return new PageResult<>(records, total, current, size);
    }
}