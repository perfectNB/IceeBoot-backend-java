/**
 * 用户视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.vo.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.zboot.framework.entity.system.RoleDO;

/**
 * 用户视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "角色对象列表")
    private List<RoleDO> roleList;

    @Schema(description = "用户菜单列表")
    private List<Map<String, Object>> menus;

    @Schema(description = "权限列表")
    private List<String> permissions;
}