/**
 * 数据库表结构信息
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.zboot.framework.ai.model;

import java.util.List;

/**
 * 表结构信息
 */
public class TableSchema {
    private String tableName;
    private String entityClass;
    private String comment;
    private List<ColumnSchema> columns;

    // Getters and Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getEntityClass() { return entityClass; }
    public void setEntityClass(String entityClass) { this.entityClass = entityClass; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public List<ColumnSchema> getColumns() { return columns; }
    public void setColumns(List<ColumnSchema> columns) { this.columns = columns; }
}