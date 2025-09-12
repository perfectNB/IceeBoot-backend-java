/**
 * 数据库查询结果封装类
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.zboot.framework.ai.model;

import java.util.*;

/**
 * 查询结果封装类
 */
public class QueryResult {
    private boolean success;
    private String errorMessage;
    private List<Map<String, Object>> data;
    private int totalCount;
    private long executionTime;

    private QueryResult() {
        this.executionTime = System.currentTimeMillis();
    }

    public static QueryResult success(List<Map<String, Object>> data, int totalCount) {
        QueryResult result = new QueryResult();
        result.success = true;
        result.data = data;
        result.totalCount = totalCount;
        return result;
    }

    public static QueryResult error(String errorMessage) {
        QueryResult result = new QueryResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.data = new ArrayList<>();
        result.totalCount = 0;
        return result;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public List<Map<String, Object>> getData() { return data; }
    public int getTotalCount() { return totalCount; }
    public long getExecutionTime() { return executionTime; }
    
    /**
     * 格式化查询结果为可读文本
     */
    public String formatAsText() {
        if (!success) {
            return "查询失败: " + errorMessage;
        }
        
        if (data.isEmpty()) {
            return "查询结果为空";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("查询成功，共找到 ").append(totalCount).append(" 条记录:\n\n");
        
        // 获取列名
        Set<String> columns = data.get(0).keySet();
        
        // 格式化每一行数据 - 显示所有记录
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            result.append("第 ").append(i + 1).append(" 条记录:\n");
            
            for (String column : columns) {
                Object value = row.get(column);
                String displayValue = (value == null) ? "[空]" : value.toString();
                result.append("  ").append(column).append(": ").append(displayValue).append("\n");
            }
            result.append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * 格式化查询结果为 JSON 格式
     */
    public String formatAsJson() {
        if (!success) {
            return "{\"error\": \"" + errorMessage + "\"}";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{\"success\": true, \"totalCount\": ").append(totalCount).append(", \"data\": [");
        
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(mapToJson(data.get(i)));
        }
        
        json.append("]}");
        return json.toString();
    }
    
    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(", ");
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            
            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else {
                json.append(value.toString());
            }
        }
        
        json.append("}");
        return json.toString();
    }
}