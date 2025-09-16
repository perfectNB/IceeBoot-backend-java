/**
 * 数据库查询 Function Calling 实现
 * 基于 Spring AI Alibaba 的 Function Calling 功能
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.iceeboot.framework.llm.service;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.framework.llm.query.DatabaseQueryRequest;
import com.iceeboot.framework.llm.model.LLMQueryResponse;
import com.iceeboot.framework.llm.model.LLMQueryResult;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 数据库查询函数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseQueryFunction implements Function<DatabaseQueryRequest, LLMQueryResponse> {

    private final JdbcTemplate jdbcTemplate;
    
    // DatabaseSchemaService 相关字段
    private static final String ENTITY_PACKAGE = "com.iceeboot.framework.entity";
    private static final String RESOURCE_PATTERN = "classpath*:com/iceeboot/framework/entity/**/*DO.class";
    
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
    
    // 缓存表结构信息
    private Map<String, TableSchema> tableSchemaCache = new HashMap<>();
    private boolean cacheInitialized = false;
    
    // DatabaseQueryExecutor 相关字段
    // SQL 注入防护：只允许 SELECT 语句
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "^\\s*SELECT\\s+.*", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 危险关键词黑名单
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE ", "TRUNCATE",
        "EXEC", "EXECUTE", "UNION", "--", "/*", "*/", ";"
    );
    
    // 查询结果限制
    private static final int MAX_RESULT_SIZE = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 30;

    @Override
    public LLMQueryResponse apply(DatabaseQueryRequest request) {
        try {
            request.setKeyword(null);
            log.info("执行数据库查询函数，查询意图: {}", request.getQueryIntent());
            
            // 1. 根据查询意图分析相关表
            List<TableSchema> relevantTables = 
                searchRelevantTables(request.getTableName());
            
            if (relevantTables.isEmpty()) {
                return LLMQueryResponse.error("未找到与查询相关的数据表");
            }
            
            // 2. 生成 SQL 查询
            String sql = generateSqlFromIntent(request, relevantTables);
            
            // 3. 执行查询
            LLMQueryResult LLMQueryResult = executeQuery(sql);
            
            // 4. 返回结果
            if (LLMQueryResult.isSuccess()) {
                String resultData = LLMQueryResult.formatAsText();
                // 添加明确指示，要求AI模型显示所有数据
                String enhancedData = resultData + "\n\n【重要提示】：请完整显示以上所有查询结果，不要省略任何记录。用户需要查看完整的数据列表。";
                return LLMQueryResponse.success(
                    enhancedData,
                    sql,
                    LLMQueryResult.getTotalCount()
                );
            } else {
                return LLMQueryResponse.error(LLMQueryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("数据库查询函数执行失败", e);
            return LLMQueryResponse.error("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 根据查询意图生成 SQL
     */
    private String generateSqlFromIntent(DatabaseQueryRequest request, List<TableSchema> relevantTables) {
        // 选择最相关的表（简单实现：选择第一个匹配的表）
        TableSchema primaryTable = relevantTables.get(0);
        
        // 构建查询条件
        Map<String, Object> conditions = new HashMap<>();
        
        // 解析查询意图中的条件
        parseQueryConditions(request, primaryTable, conditions);
        
        // 生成 SQL
        return generateSelectSql(
            primaryTable.getTableName(),
            conditions,
            request.getLimit()
        );
    }

    /**
     * 解析查询条件
     */
    private void parseQueryConditions(DatabaseQueryRequest request, TableSchema table, Map<String, Object> conditions) {
        String intent = request.getQueryIntent().toLowerCase();
        
        // 时间相关查询
        if (intent.contains("最近") || intent.contains("近期")) {
            if (intent.contains("天")) {
                // 提取天数
                String[] parts = intent.split("最近|近期");
                if (parts.length > 1) {
                    String daysPart = parts[1].replaceAll("[^0-9]", "");
                    if (!daysPart.isEmpty()) {
                        int days = Integer.parseInt(daysPart);
                        conditions.put("create_time", "DATE_SUB(NOW(), INTERVAL " + days + " DAY)");
                    }
                }
            }
        }
        
        // 状态相关查询
        if (intent.contains("激活") || intent.contains("启用")) {
            conditions.put("status", "active");
        } else if (intent.contains("禁用") || intent.contains("停用")) {
            conditions.put("status", "inactive");
        }
        
        // 用户相关查询
        if (intent.contains("用户") && intent.contains("注册")) {
            // 用户注册查询，已通过表选择处理
        }
        
        // 名称模糊查询
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            // 查找包含 name 或 title 字段的列
            for (ColumnSchema column : table.getColumns()) {
                String columnName = column.getColumnName();
                if (columnName.contains("name") || columnName.contains("title")) {
                    conditions.put(columnName, request.getKeyword());
                    break;
                }
            }
        }
    }
    
    // ==================== DatabaseQueryExecutor 方法 ====================
    
    /**
     * 执行安全的数据库查询
     * 
     * @param sql SQL 查询语句
     * @return 查询结果
     */
    public LLMQueryResult executeQuery(String sql) {
        try {
            // 1. 安全检查
            if (!isSafeQuery(sql)) {
                return LLMQueryResult.error("不安全的查询语句，只允许 SELECT 操作");
            }
            
            // 2. 添加查询限制
            String safeSql = addQueryLimit(sql);
            
            log.info("执行数据库查询: {}", safeSql);
            
            // 3. 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(safeSql);
            
            // 4. 检查结果大小
            if (results.size() > MAX_RESULT_SIZE) {
                results = results.subList(0, MAX_RESULT_SIZE);
                log.warn("查询结果超过限制，已截取前 {} 条", MAX_RESULT_SIZE);
            }
            
            return LLMQueryResult.success(results, results.size());
            
        } catch (Exception e) {
            log.error("数据库查询执行失败: {}", e.getMessage(), e);
            return LLMQueryResult.error("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 生成查询 SQL
     * 
     * @param tableName 表名
     * @param conditions 查询条件
     * @param limit 限制条数
     * @return SQL 语句
     */
    public String generateSelectSql(String tableName, Map<String, Object> conditions, Integer limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        
        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            List<String> whereConditions = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String column = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    whereConditions.add(column + " LIKE '%" + value + "%'");
                } else if (value instanceof Number) {
                    whereConditions.add(column + " = " + value);
                } else {
                    whereConditions.add(column + " = '" + value + "'");
                }
            }
            
            sql.append(String.join(" AND ", whereConditions));
        }
        
        // 添加排序和限制
        sql.append(" ORDER BY create_time DESC");
        
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(Math.min(limit, MAX_RESULT_SIZE));
        } else {
            sql.append(" LIMIT ").append(MAX_RESULT_SIZE);
        }
        
        return sql.toString();
    }

    /**
     * 检查查询是否安全
     */
    private boolean isSafeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String upperSql = sql.toUpperCase();
        
        // 1. 检查是否为 SELECT 语句
        if (!SELECT_PATTERN.matcher(sql).matches()) {
            log.warn("非 SELECT 查询被拒绝: {}", sql);
            return false;
        }
        
        // 2. 检查危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)&& !upperSql.contains("CREATE_TIME")) {
                log.warn("包含危险关键词 '{}' 的查询被拒绝: {}", keyword, sql);
                return false;
            }
        }
        
//        // 3. 检查是否包含子查询或联合查询（简单检查）
//        if (upperSql.contains("(") && upperSql.contains("SELECT")) {
//            log.warn("包含子查询的复杂查询被拒绝: {}", sql);
//            return false;
//        }
        
        return true;
    }

    /**
     * 为查询添加限制
     */
    private String addQueryLimit(String sql) {
        String trimmedSql = sql.trim();
        
        // 如果已经有 LIMIT，检查是否超过最大限制
        if (trimmedSql.toUpperCase().contains("LIMIT")) {
            return trimmedSql;
        }
        
        // 添加默认限制
        return trimmedSql + " LIMIT " + MAX_RESULT_SIZE;
    }
    
    // ==================== DatabaseSchemaService 方法 ====================
    
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
        
        return false;
    }
}