/**
 * 大模型消息DTO
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 大模型消息DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class LLMMessage {
    
    /**
     * 消息角色：system、user、assistant
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息名称（可选）
     */
    private String name;
    
    /**
     * 构造函数
     */
    public LLMMessage() {}
    
    /**
     * 构造函数
     * @param role 角色
     * @param content 内容
     */
    public LLMMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    /**
     * 创建系统消息
     * @param content 内容
     * @return LLMMessage
     */
    public static LLMMessage system(String content) {
        return new LLMMessage("system", content);
    }
    
    /**
     * 创建用户消息
     * @param content 内容
     * @return LLMMessage
     */
    public static LLMMessage user(String content) {
        return new LLMMessage("user", content);
    }
    
    /**
     * 创建助手消息
     * @param content 内容
     * @return LLMMessage
     */
    public static LLMMessage assistant(String content) {
        return new LLMMessage("assistant", content);
    }
}