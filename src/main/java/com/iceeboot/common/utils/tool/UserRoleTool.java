package com.iceeboot.common.utils.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.service.system.ISysRoleService;
import com.iceeboot.framework.service.system.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author CodeIcee
 * @date 2025/1/27
 */
@Component
@Slf4j
public class UserRoleTool {

    private final ISysRoleService roleService;
    private final ISysUserService userService;

    public UserRoleTool(ISysRoleService roleService,ISysUserService userService) {

        this.roleService = roleService;
        this.userService = userService;
    }

    /**
     * true 返回结果不走大模型
     * false 走大模型
     */
    @Tool(description = "通过用户昵称或用户名查询用户拥有的角色", returnDirect = false)
    public String getUserRoles(String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return "请提供有效的用户昵称或用户名";
            }
            
            // 先通过昵称查询用户
             LambdaQueryWrapper<UserDO> nicknameWrapper = new LambdaQueryWrapper<>();
             nicknameWrapper.eq(UserDO::getNickname, name.trim());
             UserDO user = userService.getOne(nicknameWrapper);
             
             // 如果通过昵称没找到，再通过用户名查询
             if (user == null) {
                 user = userService.getUserByUsername(name.trim());
             }
            
            // 如果都没找到，返回用户不存在
            if (user == null) {
                return "用户 '" + name + "' 不存在";
            }
            
            // 从数据库查询用户角色
            List<RoleDO> roles = roleService.getRolesByUserId(user.getId());
            
            if (roles.isEmpty()) {
                return "用户 '" + name + "' 未分配任何角色";
            }
            
            // 提取角色名称
            List<String> roleNames = roles.stream()
                    .map(RoleDO::getName)
                    .collect(Collectors.toList());
            
            return "用户 '" + name + "' 拥有的角色: " + String.join(", ", roleNames);
        } catch (Exception e) {
            log.error("查询用户角色时发生错误: {}", e.getMessage(), e);
            return "查询用户角色时发生错误: " + e.getMessage();
        }
    }
}