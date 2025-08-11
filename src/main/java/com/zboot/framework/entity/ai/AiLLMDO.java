/**
 * 大模型实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_llm")
public class AiLLMDO extends ZbootDO {

    @Schema(description = "模型名称")
    @TableField("name")
    private String name;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;
}