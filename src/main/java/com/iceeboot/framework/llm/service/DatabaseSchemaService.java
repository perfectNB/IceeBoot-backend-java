/**
 * 数据库表结构分析服务
 * 用于动态获取数据库表结构信息，支持 Function Calling 的 Schema 注入
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.iceeboot.framework.llm.service;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.framework.llm.model.TableSchema;
import com.iceeboot.framework.llm.model.ColumnSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据库表结构分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private static final String ENTITY_PACKAGE = "com.iceeboot.framework.entity";
    private static final String RESOURCE_PATTERN = "classpath*:com/iceeboot/framework/entity/**/*DO.class";
    
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
    
    // 缓存表结构信息
    private Map<String, TableSchema> tableSchemaCache = new HashMap<>();
    private boolean cacheInitialized = false;

    /**
     * 获取所有数据库表的结构信息
     */
    public Map<String, TableSchema> getAllTableSchemas() {
        if (!cacheInitialized) {
            initializeTableSchemas();
        }
        return new HashMap<>(tableSchemaCache);
    }

    /**
     * 根据表名获取表结构
     */
    public TableSchema getTableSchema(String tableName) {
        if (!cacheInitialized) {
            initializeTableSchemas();
        }
        return tableSchemaCache.get(tableName);
    }

    /**
     * 根据关键词搜索相关表
     */
    public List<TableSchema> searchRelevantTables(String keyword) {
        if (!cacheInitialized) {
            initializeTableSchemas();
        }
        
        List<TableSchema> relevantTables = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (TableSchema schema : tableSchemaCache.values()) {
            // 检查表名、注释、字段名是否包含关键词
            if (isTableRelevant(schema, lowerKeyword)) {
                relevantTables.add(schema);
            }
        }
        
        return relevantTables;
    }

    /**
     * 初始化表结构缓存
     */
    private void initializeTableSchemas() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(RESOURCE_PATTERN);
            
            for (Resource resource : resources) {
                try {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    String className = metadataReader.getClassMetadata().getClassName();
                    
                    if (className.contains(ENTITY_PACKAGE) && className.endsWith("DO")) {
                        Class<?> entityClass = Class.forName(className);
                        TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
                        
                        if (tableNameAnnotation != null) {
                            String tableName = tableNameAnnotation.value();
                            TableSchema schema = buildTableSchema(entityClass, tableName);
                            tableSchemaCache.put(tableName, schema);
                            log.info("加载表结构: {} -> {}", tableName, className);
                        } else {
                            log.warn("实体类 {} 没有 @TableName 注解", className);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析实体类失败: {}", resource.getFilename(), e);
                }
            }
            
            cacheInitialized = true;
            log.info("数据库表结构缓存初始化完成，共加载 {} 个表", tableSchemaCache.size());
            
        } catch (Exception e) {
            log.error("初始化表结构缓存失败", e);
        }
    }

    /**
     * 构建表结构信息
     */
    private TableSchema buildTableSchema(Class<?> entityClass, String tableName) {
        TableSchema schema = new TableSchema();
        schema.setTableName(tableName);
        schema.setEntityClass(entityClass.getSimpleName());
        
        // 获取表注释
        Schema schemaAnnotation = entityClass.getAnnotation(Schema.class);
        if (schemaAnnotation != null) {
            schema.setComment(schemaAnnotation.description());
        }
        
        // 获取字段信息
        List<ColumnSchema> columns = new ArrayList<>();
        
        // 添加基础字段（来自iceebootDO）
        addBaseColumns(columns);
        
        // 添加实体类字段
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue; // 跳过不存储到数据库的字段
            }
            
            ColumnSchema column = buildColumnSchema(field, tableField);
            if (column != null) {
                columns.add(column);
            }
        }
        
        schema.setColumns(columns);
        return schema;
    }

    /**
     * 添加基础字段（来自iceebootDO）
     */
    private void addBaseColumns(List<ColumnSchema> columns) {
        columns.add(new ColumnSchema("id", "bigint", "主键ID", true, false));
        columns.add(new ColumnSchema("create_time", "datetime", "创建时间", false, false));
        columns.add(new ColumnSchema("update_time", "datetime", "更新时间", false, false));
        columns.add(new ColumnSchema("create_by", "varchar(64)", "创建人", false, true));
        columns.add(new ColumnSchema("update_by", "varchar(64)", "更新人", false, true));
        columns.add(new ColumnSchema("del_flag", "tinyint", "删除标志", false, false));
    }

    /**
     * 构建字段结构信息
     */
    private ColumnSchema buildColumnSchema(Field field, TableField tableField) {
        String columnName = getColumnName(field, tableField);
        if (columnName == null) {
            return null;
        }
        
        String columnType = getColumnType(field);
        String comment = getColumnComment(field);
        boolean isPrimaryKey = "id".equals(columnName);
        boolean nullable = !isPrimaryKey; // 主键不可为空，其他字段默认可为空
        
        return new ColumnSchema(columnName, columnType, comment, isPrimaryKey, nullable);
    }

    /**
     * 获取字段名
     */
    private String getColumnName(Field field, TableField tableField) {
        if (tableField != null && !tableField.value().isEmpty()) {
            return tableField.value();
        }
        return camelToSnake(field.getName());
    }

    /**
     * 获取字段类型
     */
    private String getColumnType(Field field) {
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            return "varchar(255)";
        } else if (fieldType == Long.class || fieldType == long.class) {
            return "bigint";
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return "int";
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "tinyint";
        } else if (fieldType == LocalDateTime.class) {
            return "datetime";
        } else if (fieldType == Double.class || fieldType == double.class) {
            return "double";
        } else if (fieldType == Float.class || fieldType == float.class) {
            return "float";
        } else {
            return "text";
        }
    }

    /**
     * 获取字段注释
     */
    private String getColumnComment(Field field) {
        Schema schema = field.getAnnotation(Schema.class);
        if (schema != null) {
            return schema.description();
        }
        return field.getName();
    }

    /**
     * 驼峰转下划线
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 判断表是否与关键词相关
     */
    private boolean isTableRelevant(TableSchema schema, String keyword) {
        // 检查表名
        if (schema.getTableName().toLowerCase().equals(keyword)) {
            return true;
        }
        
        // 检查表注释
        if (schema.getComment() != null && schema.getComment().toLowerCase().equals(keyword)) {
            return true;
        }
        
//        // 检查字段名和注释
//        for (ColumnSchema column : schema.getColumns()) {
//            if (column.getColumnName().toLowerCase().contains(keyword) ||
//                (column.getComment() != null && column.getComment().toLowerCase().contains(keyword))) {
//                return true;
//            }
//        }
//
        return false;
    }
}