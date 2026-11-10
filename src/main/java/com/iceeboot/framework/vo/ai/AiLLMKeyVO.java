package com.iceeboot.framework.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iceeboot.common.annotation.Sensitive;
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

    @Schema(description = "密钥名称")
    private String name;

    @Schema(description = "大模型厂商")
    private String llmProvider;

    @Schema(description = "自定义请求地址")
    private String url;

    @Schema(description = "备注")
    private String remark;

    @Sensitive(prefixKeep = 4, suffixKeep = 4)
    @Schema(description = "秘密密钥")
    private String secretKey;

    @Sensitive(prefixKeep = 4, suffixKeep = 4)
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
