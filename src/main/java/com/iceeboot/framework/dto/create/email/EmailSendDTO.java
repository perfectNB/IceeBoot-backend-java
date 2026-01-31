package com.iceeboot.framework.dto.create.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "邮件发送请求")
public class EmailSendDTO {
    @Schema(description = "密钥ID")
    private Long keyId;

    @Schema(description = "Email厂商")
    private String provider;

    @Schema(description = "收件人邮箱")
    private String toEmail;

    @Schema(description = "收件人邮箱列表")
    private List<String> toEmails;

    @Schema(description = "抄送邮箱列表")
    private List<String> ccEmails;

    @Schema(description = "密送邮箱列表")
    private List<String> bccEmails;

    @Schema(description = "邮件主题")
    private String subject;

    @Schema(description = "纯文本内容")
    private String textContent;

    @Schema(description = "HTML内容")
    private String htmlContent;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "模板参数")
    private Map<String, Object> templateParams;

    @Schema(description = "发件人名称")
    private String fromName;

    @Schema(description = "邮件类型")
    private String emailType;

    @Schema(description = "邮件优先级")
    private Integer priority;

    @Schema(description = "是否需要回执")
    private Boolean needReceipt;

    @Schema(description = "回复邮箱")
    private String replyTo;

    @Schema(description = "邮件编码")
    private String charset;

    @Schema(description = "自定义邮件头")
    private Map<String, String> customHeaders;

    @Schema(description = "是否异步发送")
    private Boolean async;

    @Schema(description = "附件列表")
    private List<EmailAttachmentDTO> attachments;
}
