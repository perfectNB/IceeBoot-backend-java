/*
 * @Description: 公共基础实体类
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot.common.core.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    @TableField(value = "del_flag")
    @JsonIgnore
    private Integer delFlag;

    /**
     * 页码（查询参数，不存储到数据库）
     */
    @TableField(exist = false)
    @JsonIgnore
    private Integer pageNumber;

    /**
     * 页大小（查询参数，不存储到数据库）
     */
    @TableField(exist = false)
    @JsonIgnore
    private Integer pageSize;

    /**
     * 搜索关键字（查询参数，不存储到数据库）
     */
    @TableField(exist = false)
    @JsonIgnore
    private String keyword;

    /**
     * 排序字段（查询参数，不存储到数据库）
     */
    @TableField(exist = false)
    @JsonIgnore
    private String orderBy;

    /**
     * 排序方向（查询参数，不存储到数据库）
     */
    @TableField(exist = false)
    @JsonIgnore
    private String orderDirection;
}