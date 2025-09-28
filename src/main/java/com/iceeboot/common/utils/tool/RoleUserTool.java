package com.iceeboot.common.utils.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.service.system.ISysRoleService;
import com.iceeboot.framework.service.system.ISysUserRoleService;
import com.iceeboot.framework.service.system.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author CodeIcee
 * @date 2025-09-28
 */
@Component
@Slf4j
public class RoleUserTool {

    private final ISysRoleService roleService;
    private final ISysUserRoleService userRoleService;
    private final ISysUserService userService;
    
    public RoleUserTool(ISysRoleService roleService, 
                       ISysUserRoleService userRoleService,
                       ISysUserService userService) {
        this.roleService = roleService;
        this.userRoleService = userRoleService;
        this.userService = userService;
    }

    /**
     * true 返回结果不走大模型
     * false 走大模型
     */
    @Tool(description = "根据角色名称查询该角色下的用户", returnDirect = false)
    public String getRoleUsers(String roleName) {
        log.info("触发tool：根据角色名称查询该角色下的用户");
        try {
            // 根据角色名称查找角色
            LambdaQueryWrapper<RoleDO> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(RoleDO::getName, roleName);
            RoleDO role = roleService.getOne(roleWrapper);
            
            if (role == null) {
                return "角色: " + roleName + " 不存在";
            }
            
            // 根据角色ID查询用户ID列表
            List<Long> userIds = userRoleService.getUserIdsByRoleId(role.getId());
            
            if (userIds.isEmpty()) {
                return "角色: " + roleName + " 下未找到任何用户";
            }
            
            // 根据用户ID列表查询用户信息
            List<UserDO> users = userService.listByIds(userIds);
            
            // 格式化用户信息
            List<String> userInfos = users.stream()
                    .map(user -> user.getNickname() + "(ID:" + user.getId() + ", 用户名:" + user.getUsername() + ")")
                    .collect(Collectors.toList());
            
            return "角色: " + roleName + " 下的用户: " + String.join(", ", userInfos);
        } catch (Exception e) {
            log.error("查询角色用户时发生错误: ", e);
            return "查询角色用户时发生错误: " + e.getMessage();
        }
    }
}