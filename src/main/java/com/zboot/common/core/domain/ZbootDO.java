/*
 * @Description: 公共基础实体类
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot.common.core.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公共基础实体类
 * 
 * @author CodeIcee
 * @since 2025-08-11
 */
@Data
@Accessors(chain = true)
public class ZbootDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "创建者")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "删除标志")
    @TableLogic
    @TableField(value = "del_flag")
    @JsonIgnore
    private Integer delFlag;

    @Schema(description = "页码")
    @TableField(exist = false)
    @JsonIgnore
    private Integer pageNumber;

    @Schema(description = "每页数量")
    @TableField(exist = false)
    @JsonIgnore
    private Integer pageSize;

    @Schema(description = "搜索关键字")
    @TableField(exist = false)
    @JsonIgnore
    private String keyword;

    @Schema(description = "排序字段")
    @TableField(exist = false)
    @JsonIgnore
    private String orderBy;

    @Schema(description = "排序方向")
    @TableField(exist = false)
    @JsonIgnore
    private String orderDirection;
}