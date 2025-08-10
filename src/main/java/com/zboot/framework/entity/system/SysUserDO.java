/**
 * 系统用户实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zboot.common.core.domain.ZbootDO;
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
@TableName("sys_user")
public class SysUserDO extends ZbootDO {

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 状态（active-激活，inactive-禁用）
     */
    @TableField("status")
    private String status;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 微信openid
     */
    @TableField("wechat_openid")
    private String wechatOpenid;

    /**
     * 微信unionid
     */
    @TableField("wechat_unionid")
    private String wechatUnionid;

    /**
     * 角色列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<String> roles;

    /**
     * 权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<String> permissions;
}