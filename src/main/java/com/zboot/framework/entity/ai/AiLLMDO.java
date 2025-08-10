/**
 * 大模型实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 大模型实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_llm")
public class AiLLMDO extends ZbootDO {

    /**
     * 模型名称
     */
    @TableField("name")
    private String name;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 状态（active-激活，inactive-禁用）
     */
    @TableField("status")
    private String status;

    /**
     * 密钥列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<AiLLMKeyDO> keys;
}