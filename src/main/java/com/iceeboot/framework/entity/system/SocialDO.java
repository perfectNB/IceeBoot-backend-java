package com.iceeboot.framework.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户社交媒体关联表
 *
 * @author CodeIcee
 * @date 2025-11-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_social")
@Schema(description = "用户社交媒体关联表")
@Accessors(chain = true)
public class SocialDO extends IceeBootDO {

    @Schema(description = "社交账号唯一id")
    @TableField("openId")
    private String openId;

    @Schema(description = "社交账号平台 0Github 1QQ 2微信 3微博")
    @TableField("platform")
    private Integer platform;

    @Schema(description = "社交账号用户名")
    @TableField("username")
    private String username;

    @Schema(description = "头像")
    @TableField("avatar")
    private String avatar;

    @Schema(description = "绑定用户unionId")
    @TableField("unionId")
    private String unionId;

    @Schema(description = "是否绑定")
    @TableField("is_related")
    private Boolean isRelated;

    @Schema(description = "绑定用户名")
    @TableField("nickname")
    private String nickname;
}
