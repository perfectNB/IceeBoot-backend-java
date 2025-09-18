
package com.iceeboot.framework.llm.query;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * 日志查询请求参数
 *
 * @author CodeIcee
 * @date 2025-09-16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonClassDescription("日志查询请求参数")
@Data
public class LogQueryRequest {
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("用户的原始查询意图，直接复制用户输入的内容")
    private String queryIntent;
    
    @JsonProperty
    @JsonPropertyDescription("日志级别过滤，如：ERROR、WARN、INFO、DEBUG")
    private String level;
    
    @JsonProperty
    @JsonPropertyDescription("日志来源模块，如：用户管理、权限管理、系统监控等")
    private String module;
    
    @JsonProperty
    @JsonPropertyDescription("查询关键词，用于模糊匹配日志内容")
    private String keyword;
    
    @JsonProperty
    @JsonPropertyDescription("时间范围，如：今天、昨天、最近3天、最近一周等")
    private String timeRange;
    
    @JsonProperty
    @JsonPropertyDescription("查询结果限制数量。从用户输入中提取数字，如'前20条'->20，'前10个'->10，默认为100")
    private Integer limit = 100;
    
    @JsonProperty
    @JsonPropertyDescription("操作用户ID或用户名，用于查询特定用户的操作日志")
    private String userId;
}