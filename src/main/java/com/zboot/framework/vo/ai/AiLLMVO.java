package com.zboot.framework.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI大模型视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI大模型视图对象")
public class AiLLMVO {

    @Schema(description = "模型ID")
    private Long id;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "密钥列表")
    private List<AiLLMKeyVO> keys;
}