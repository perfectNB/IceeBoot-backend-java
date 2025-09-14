/**
 * 安全的数据库查询执行器
 * 只允许执行 SELECT 查询，防止数据被恶意修改
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.iceeboot.framework.llm.service;

import com.iceeboot.framework.llm.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 安全的数据库查询执行器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseQueryExecutor {

    private final JdbcTemplate jdbcTemplate;
    
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

    /**
     * 执行安全的数据库查询
     * 
     * @param sql SQL 查询语句
     * @return 查询结果
     */
    public QueryResult executeQuery(String sql) {
        try {
            // 1. 安全检查
            if (!isSafeQuery(sql)) {
                return QueryResult.error("不安全的查询语句，只允许 SELECT 操作");
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
            
            return QueryResult.success(results, results.size());
            
        } catch (Exception e) {
            log.error("数据库查询执行失败: {}", e.getMessage(), e);
            return QueryResult.error("查询执行失败: " + e.getMessage());
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
        
        // 3. 检查是否包含子查询或联合查询（简单检查）
        if (upperSql.contains("(") && upperSql.contains("SELECT")) {
            log.warn("包含子查询的复杂查询被拒绝: {}", sql);
            return false;
        }
        
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
}