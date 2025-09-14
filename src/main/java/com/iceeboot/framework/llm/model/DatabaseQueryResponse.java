/**
 * 数据库查询响应结果
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.iceeboot.framework.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 响应结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseQueryResponse {
    private boolean success;
    private String message;
    private String data;
    private String executedSql;
    private Integer totalCount;
    private String errorMessage;

    public static DatabaseQueryResponse success(String data, String executedSql, Integer totalCount) {
        DatabaseQueryResponse response = new DatabaseQueryResponse();
        response.success = true;
        response.message = "查询执行成功";
        response.data = data;
        response.executedSql = executedSql;
        response.totalCount = totalCount;
        return response;
    }

    public static DatabaseQueryResponse error(String errorMessage) {
        DatabaseQueryResponse response = new DatabaseQueryResponse();
        response.success = false;
        response.message = "查询执行失败";
        response.errorMessage = errorMessage;
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public String getExecutedSql() { return executedSql; }
    public void setExecutedSql(String executedSql) { this.executedSql = executedSql; }
    
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}