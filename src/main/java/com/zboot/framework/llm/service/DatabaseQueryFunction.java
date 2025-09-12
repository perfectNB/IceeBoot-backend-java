/**
 * 数据库查询 Function Calling 实现
 * 基于 Spring AI Alibaba 的 Function Calling 功能
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.zboot.framework.llm.service;

import com.zboot.framework.llm.model.DatabaseQueryRequest;
import com.zboot.framework.llm.model.DatabaseQueryResponse;
import com.zboot.framework.llm.model.QueryResult;
import com.zboot.framework.llm.model.TableSchema;
import com.zboot.framework.llm.model.ColumnSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * 数据库查询函数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseQueryFunction implements Function<DatabaseQueryRequest, DatabaseQueryResponse> {

    private final DatabaseSchemaService schemaService;
    private final DatabaseQueryExecutor queryExecutor;

    @Override
    public DatabaseQueryResponse apply(DatabaseQueryRequest request) {
        try {
            request.setKeyword(null);
            log.info("执行数据库查询函数，查询意图: {}", request.getQueryIntent());
            
            // 1. 根据查询意图分析相关表
            List<TableSchema> relevantTables = 
                schemaService.searchRelevantTables(request.getTableName());
            
            if (relevantTables.isEmpty()) {
                return DatabaseQueryResponse.error("未找到与查询相关的数据表");
            }
            
            // 2. 生成 SQL 查询
            String sql = generateSqlFromIntent(request, relevantTables);
            
            // 3. 执行查询
            QueryResult queryResult = queryExecutor.executeQuery(sql);
            
            // 4. 返回结果
            if (queryResult.isSuccess()) {
                String resultData = queryResult.formatAsText();
                // 添加明确指示，要求AI模型显示所有数据
                String enhancedData = resultData + "\n\n【重要提示】：请完整显示以上所有查询结果，不要省略任何记录。用户需要查看完整的数据列表。";
                return DatabaseQueryResponse.success(
                    enhancedData,
                    sql,
                    queryResult.getTotalCount()
                );
            } else {
                return DatabaseQueryResponse.error(queryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("数据库查询函数执行失败", e);
            return DatabaseQueryResponse.error("查询执行失败: " + e.getMessage());
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
        return queryExecutor.generateSelectSql(
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
}