/**
 * Sa-Token权限校验接口实现
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.config.security;

import cn.dev33.satoken.stp.StpInterface;
import com.iceeboot.framework.service.system.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token权限校验接口实现
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Component
public class SaTokenPermissionImpl implements StpInterface {

    @Autowired
    private ISysUserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 根据用户ID获取权限列表
        Long userId = Long.valueOf(loginId.toString());
        return userService.getUserPermissions(userId);
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 根据用户ID获取角色列表
        Long userId = Long.valueOf(loginId.toString());
        return userService.getUserRoles(userId);
    }
}