package com.iceeboot.common.utils.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.service.system.ISysRoleService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.system.SysUserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author CodeIcee
 * @date 2025-09-28
 */

@Component
@Slf4j
public class UserMenuAndPermissionTool {
    private final ISysUserService userService;

    public UserMenuAndPermissionTool(ISysUserService userService) {

        this.userService = userService;
    }

    /**
     * true 返回结果不走大模型
     * false 走大模型
     */
    @Tool(description = "通过用户昵称或用户名查询用户拥有的菜单和权限", returnDirect = true)
    public String getUserMenuAndPermission(String name) {
        log.info("触发tool:通过用户昵称或用户名查询用户拥有的菜单和权限");
        try {
            if (name == null || name.trim().isEmpty()) {
                return "请提供有效的用户昵称或用户名";
            }

            // 先通过昵称查询用户
            LambdaQueryWrapper<UserDO> nicknameWrapper = new LambdaQueryWrapper<>();
            nicknameWrapper.eq(UserDO::getNickname, name.trim());
            UserDO user = userService.getOne(nicknameWrapper);
            if (user == null) {
                // 如果通过昵称没找到，再通过用户名查询
                LambdaQueryWrapper<UserDO> usernameWrapper = new LambdaQueryWrapper<>();
                usernameWrapper.eq(UserDO::getUsername, name.trim());
                user = userService.getOne(usernameWrapper);
                if (user == null) {
                    return "未找到用户: " + name;
                }
            }
            SysUserVO sysUserVO = userService.getUserById(user.getId());


            return "用户 '" + name + "' 拥有的菜单和权限为: " + sysUserVO.toString();
        } catch (Exception e) {
            log.error("查询用户角色时发生错误: {}", e.getMessage(), e);
            if (e.getMessage().contains("Expected one result (or null)")){
                return "昵称为"+name+ "的用户存在2个或者以上，请您用用户名来查询，保证唯一性";
            }
            return "查询用户角色时发生错误: " + e.getMessage();
        }
    }
}
