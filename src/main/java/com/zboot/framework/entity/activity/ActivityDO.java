/**
 * 活动实体类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.entity.activity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动实体类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
@Schema(description = "活动信息")
public class ActivityDO extends ZbootDO {

    /**
     * 活动名称
     */
    @Schema(description = "活动名称")
    @TableField("activity_name")
    private String activityName;

    /**
     * 活动描述
     */
    @Schema(description = "活动描述")
    @TableField("description")
    private String description;

    /**
     * 活动类型
     */
    @Schema(description = "活动类型")
    @TableField("activity_type")
    private String activityType;

    /**
     * 活动状态（0-未开始，1-进行中，2-已结束，3-已取消）
     */
    @Schema(description = "活动状态")
    @TableField("status")
    private Integer status;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 活动地点
     */
    @Schema(description = "活动地点")
    @TableField("location")
    private String location;

    /**
     * 参与人数限制
     */
    @Schema(description = "参与人数限制")
    @TableField("participant_limit")
    private Integer participantLimit;

    /**
     * 当前参与人数
     */
    @Schema(description = "当前参与人数")
    @TableField("current_participants")
    private Integer currentParticipants;

    /**
     * 活动负责人
     */
    @Schema(description = "活动负责人")
    @TableField("organizer")
    private String organizer;

    /**
     * 联系方式
     */
    @Schema(description = "联系方式")
    @TableField("contact_info")
    private String contactInfo;

    /**
     * 活动费用
     */
    @Schema(description = "活动费用")
    @TableField("fee")
    private java.math.BigDecimal fee;

    /**
     * 报名截止时间
     */
    @Schema(description = "报名截止时间")
    @TableField("registration_deadline")
    private LocalDateTime registrationDeadline;

    /**
     * 活动标签
     */
    @Schema(description = "活动标签")
    @TableField("tags")
    private String tags;

    /**
     * 活动图片URL
     */
    @Schema(description = "活动图片URL")
    @TableField("image_url")
    private String imageUrl;

    /**
     * 是否需要审核（0-不需要，1-需要）
     */
    @Schema(description = "是否需要审核")
    @TableField("require_approval")
    private Integer requireApproval;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息")
    @TableField("remarks")
    private String remarks;
}