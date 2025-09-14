package com.iceeboot.framework.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI大模型密钥视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI大模型密钥视图对象")
public class AiLLMKeyVO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "大模型厂商")
    private String llmProvider;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "秘密密钥")
    private String secretKey;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}