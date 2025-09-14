/**
 * 数据库查询请求参数
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.iceeboot.framework.llm.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * 请求参数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonClassDescription("数据库查询请求参数")
@Data
public class DatabaseQueryRequest {
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("用户的原始查询意图，直接复制用户输入的内容")
    private String queryIntent;
    
    @JsonProperty(required = true)
    @JsonPropertyDescription("查询的数据库表名，必须填写。根据查询内容判断：用户相关->t_user，角色相关->t_role，任务相关->t_task，日志相关->t_log，活动相关->t_activity，文件相关->t_oss_file，AI聊天相关->t_ai_chat_session")
    private String tableName;

    @JsonProperty(required = true)
    @JsonPropertyDescription("查询对象的中文名称，必须填写。从用户输入中提取，如：用户、角色、任务、日志、活动、文件等")
    private String name;
    
    @JsonProperty
    @JsonPropertyDescription("查询关键词，用于模糊匹配。如果用户提到具体的名称或条件，请提取出来")
    private String keyword;
    
    @JsonProperty
    @JsonPropertyDescription("查询结果限制数量。从用户输入中提取数字，如'前20名'->20，'前10个'->10，默认为50")
    private Integer limit = 50;


}