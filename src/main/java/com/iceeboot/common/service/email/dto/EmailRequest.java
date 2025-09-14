/**
 * Email请求DTO
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.email.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Email请求DTO
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Accessors(chain = true)
public class EmailRequest {
    
    /**
     * 发件人邮箱
     */
    private String fromEmail;
    
    /**
     * 发件人姓名
     */
    private String fromName;
    
    /**
     * 收件人邮箱
     */
    private String toEmail;
    
    /**
     * 收件人姓名
     */
    private String toName;
    
    /**
     * 批量收件人邮箱列表
     */
    private List<String> toEmails;
    
    /**
     * 抄送邮箱列表
     */
    private List<String> ccEmails;
    
    /**
     * 密送邮箱列表
     */
    private List<String> bccEmails;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件内容（纯文本）
     */
    private String textContent;
    
    /**
     * 邮件内容（HTML格式）
     */
    private String htmlContent;
    
    /**
     * 邮件模板ID
     */
    private String templateId;
    
    /**
     * 模板参数
     */
    private Map<String, Object> templateParams;
    
    /**
     * 附件列表
     */
    private List<EmailAttachment> attachments;
    
    /**
     * SMTP服务器地址
     */
    private String smtpHost;
    
    /**
     * SMTP端口
     */
    private Integer smtpPort;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码或授权码
     */
    private String password;
    
    /**
     * 是否启用SSL
     */
    private Boolean enableSsl;
    
    /**
     * 是否启用TLS
     */
    private Boolean enableTls;
    
    /**
     * 是否启用认证
     */
    private Boolean enableAuth;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout;
    
    /**
     * 邮件优先级（1-高，3-普通，5-低）
     */
    private Integer priority;
    
    /**
     * 是否需要回执
     */
    private Boolean needReceipt;
    
    /**
     * 回复邮箱
     */
    private String replyTo;
    
    /**
     * 邮件编码
     */
    private String charset;
    
    /**
     * 自定义邮件头
     */
    private Map<String, String> customHeaders;
    
    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 邮件类型（text/html/template）
     */
    private String emailType;
    
    /**
     * 是否异步发送
     */
    private Boolean async;
    
    /**
     * 邮件附件内部类
     */
    @Data
    @Accessors(chain = true)
    public static class EmailAttachment {
        /**
         * 附件名称
         */
        private String fileName;
        
        /**
         * 附件内容（字节数组）
         */
        private byte[] content;
        
        /**
         * 附件文件路径
         */
        private String filePath;
        
        /**
         * 附件MIME类型
         */
        private String mimeType;
        
        /**
         * 附件大小
         */
        private Long fileSize;
        
        /**
         * 是否内嵌附件
         */
        private Boolean inline;
        
        /**
         * 内嵌附件ID（用于HTML中引用）
         */
        private String contentId;
    }
}