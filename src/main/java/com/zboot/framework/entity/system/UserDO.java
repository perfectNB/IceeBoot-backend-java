/**
 * 系统用户实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
@Schema(description = "用户")
public class UserDO extends ZbootDO {

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "密码")
    @TableField("password")
    private String password;

    @Schema(description = "昵称")
    @TableField("nickname")
    private String nickname;

    @Schema(description = "头像")
    @TableField("avatar")
    private String avatar;

    @Schema(description = "邮箱")
    @TableField("email")
    private String email;

    @Schema(description = "手机号")
    @TableField("phone")
    private String phone;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "最后登录时间")
    @TableField("last_login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    @TableField("last_login_ip")
    private String lastLoginIp;

    @Schema(description = "微信openid")
    @TableField("wechat_openid")
    private String wechatOpenid;

    @Schema(description = "微信unionid")
    @TableField("wechat_unionid")
    private String wechatUnionid;

    @Schema(description = "角色列表")
    @TableField(exist = false)
    private List<String> roles;

    @Schema(description = "权限列表")
    @TableField(exist = false)
    private List<String> permissions;
}