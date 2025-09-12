/**
 * 数据库字段结构信息
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.zboot.framework.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段结构信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnSchema {
    private String columnName;
    private String columnType;
    private String comment;
    private boolean isPrimaryKey;
    private boolean nullable;




}