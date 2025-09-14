/**
 * Email服务抽象接口
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.email.service;

import com.iceeboot.common.service.email.dto.EmailRequest;
import com.iceeboot.common.service.email.dto.EmailResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Email服务抽象接口
 * 定义了邮件发送的基本操作：单发、群发、模板邮件等
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
public interface EmailService {
    
    /**
     * 获取邮件服务提供商名称
     * @return 提供商名称
     */
    String getProvider();
    
    /**
     * 发送单封邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendEmail(EmailRequest request);
    
    /**
     * 发送纯文本邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendTextEmail(EmailRequest request);
    
    /**
     * 发送HTML邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendHtmlEmail(EmailRequest request);
    
    /**
     * 发送模板邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendTemplateEmail(EmailRequest request);
    
    /**
     * 批量发送邮件
     * @param request 邮件请求（包含多个收件人）
     * @return 发送响应
     */
    Mono<EmailResponse> sendBatchEmail(EmailRequest request);
    
    /**
     * 发送带附件的邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendEmailWithAttachments(EmailRequest request);
    
    /**
     * 发送通知邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendNotificationEmail(EmailRequest request);
    
    /**
     * 发送营销邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendMarketingEmail(EmailRequest request);
    
    /**
     * 发送系统邮件
     * @param request 邮件请求
     * @return 发送响应
     */
    Mono<EmailResponse> sendSystemEmail(EmailRequest request);
    
    /**
     * 查询邮件发送状态
     * @param messageId 消息ID
     * @param request 请求参数（包含认证信息）
     * @return 查询响应
     */
    Mono<EmailResponse> queryEmailStatus(String messageId, EmailRequest request);
    
    /**
     * 验证邮箱地址格式
     * @param email 邮箱地址
     * @return 是否有效
     */
    default boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * 验证配置
     * @param request 配置请求
     * @return 验证结果
     */
    Mono<Boolean> validateConfig(EmailRequest request);
    
    /**
     * 服务是否可用
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 获取支持的邮件类型
     * @return 支持的邮件类型列表
     */
    default List<String> getSupportedEmailTypes() {
        return List.of("text", "html", "template", "notification", "marketing", "system");
    }
    
    /**
     * 获取支持的功能列表
     * @return 支持的功能
     */
    default List<String> getSupportedFeatures() {
        return List.of("single", "batch", "template", "attachment", "html", "text", "status_query");
    }
    
    /**
     * 获取默认配置
     * @return 默认配置
     */
    default EmailRequest getDefaultConfig() {
        return new EmailRequest()
                .setConnectionTimeout(30000)
                .setReadTimeout(60000)
                .setEnableAuth(true)
                .setEnableSsl(true)
                .setCharset("UTF-8")
                .setPriority(3)
                .setAsync(false);
    }
    
    /**
     * 检查邮件内容长度
     * @param content 邮件内容
     * @return 是否在允许范围内
     */
    default boolean isValidContent(String content) {
        if (content == null) {
            return true; // 允许空内容
        }
        // 一般邮件内容限制在10MB以内
        return content.length() <= 10 * 1024 * 1024;
    }
    
    /**
     * 检查附件大小
     * @param attachments 附件列表
     * @return 是否在允许范围内
     */
    default boolean isValidAttachments(List<EmailRequest.EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return true;
        }
        
        long totalSize = 0;
        for (EmailRequest.EmailAttachment attachment : attachments) {
            if (attachment.getContent() != null) {
                totalSize += attachment.getContent().length;
            } else if (attachment.getFileSize() != null) {
                totalSize += attachment.getFileSize();
            }
        }
        
        // 总附件大小限制在25MB以内
        return totalSize <= 25 * 1024 * 1024;
    }
    
    /**
     * 获取邮件大小（字节）
     * @param request 邮件请求
     * @return 邮件大小
     */
    default long getEmailSize(EmailRequest request) {
        long size = 0;
        
        // 计算内容大小
        if (request.getTextContent() != null) {
            size += request.getTextContent().getBytes().length;
        }
        if (request.getHtmlContent() != null) {
            size += request.getHtmlContent().getBytes().length;
        }
        
        // 计算附件大小
        if (request.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : request.getAttachments()) {
                if (attachment.getContent() != null) {
                    size += attachment.getContent().length;
                } else if (attachment.getFileSize() != null) {
                    size += attachment.getFileSize();
                }
            }
        }
        
        return size;
    }
    
    /**
     * 格式化邮件大小
     * @param sizeInBytes 字节大小
     * @return 格式化后的大小字符串
     */
    default String formatEmailSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));
        }
    }
}