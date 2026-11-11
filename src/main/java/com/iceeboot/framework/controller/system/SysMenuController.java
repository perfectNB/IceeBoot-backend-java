package com.iceeboot.framework.controller.system;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.dto.create.system.MenuDTO;
import com.iceeboot.framework.dto.update.system.MenuUpdateDTO;
import com.iceeboot.framework.service.system.ISysMenuService;
import com.iceeboot.framework.vo.system.SysMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "菜单管理", description = "菜单增删改查、状态管理等接口")
@RestController
@RequestMapping("/v1/menus")
@RequiredArgsConstructor
public class SysMenuController {


    private final ISysMenuService menuService;


    @Operation(summary = "获取菜单树", description = "获取完整的菜单树结构")
    @GetMapping("/tree")
    @RequirePermission(dynamic = true)
    public R<List<SysMenuVO>> getMenuTree() {
        List<SysMenuVO> menuTree = menuService.getMenuTree();
        return R.ok(menuTree);
    }


    // 当前账号自助接口由全局登录拦截器保护，服务层仅返回当前用户获授权的菜单。
    @Operation(summary = "查询当前用户菜单", description = "登录后获取当前用户角色获授权的菜单，无需菜单管理权限")
    @GetMapping("/user")
    public R<List<SysMenuVO>> getUserMenu() {
        List<SysMenuVO> userMenu = menuService.getUserMenu();
        return R.ok(userMenu);
    }


    @Operation(summary = "新增菜单", description = "创建新菜单")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addMenu(@Validated @RequestBody MenuDTO menuDTO) {
        boolean success = menuService.addMenu(menuDTO);
        return success ? R.ok(ResponseMessage.Menu.MENU_CREATE_SUCCESS) : R.fail(ResponseMessage.Menu.MENU_CREATE_FAIL);
    }


    @Operation(summary = "更新菜单", description = "更新菜单信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateMenu(@Validated @RequestBody MenuUpdateDTO menuDTO) {
        boolean success = menuService.updateMenu(menuDTO);
        return success ? R.ok(ResponseMessage.Menu.MENU_UPDATE_SUCCESS) : R.fail(ResponseMessage.Menu.MENU_UPDATE_FAIL);
    }

    @Operation(summary = "删除菜单", description = "批量删除菜单")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteMenus(@RequestBody List<Long> ids) {
        boolean success = menuService.deleteMenus(ids);
        return success ? R.ok(ResponseMessage.Menu.MENU_DELETE_SUCCESS) : R.fail(ResponseMessage.Menu.MENU_DELETE_FAIL);
    }


    @Operation(summary = "修改菜单状态", description = "启用或禁用菜单")
    @PutMapping("/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Void> updateStatus(@PathVariable("id") Long id, @RequestParam String status) {
        boolean success = menuService.updateStatus(id, status);
        return success ? R.ok(ResponseMessage.Menu.MENU_UPDATE_SUCCESS) : R.fail(ResponseMessage.Menu.MENU_UPDATE_FAIL);
    }


    @Operation(summary = "检查菜单名称", description = "检查菜单名称是否已存在")
    @GetMapping("/check/name")
    @RequirePermission(dynamic = true)
    public R<Boolean> checkName(@RequestParam String name, @RequestParam(required = false) Long excludeId) {
        boolean exists = menuService.existsName(name, excludeId);
        return R.ok(exists);
    }


    @Operation(summary = "获取子菜单", description = "根据父级ID获取子菜单列表")
    @GetMapping("/children/{parentId}")
    @RequirePermission(dynamic = true)
    public R<List<SysMenuVO>> getChildrenByParentId(@PathVariable("parentId") Long parentId) {
        List<SysMenuVO> children = menuService.getChildrenByParentId(parentId);
        return R.ok(children);
    }
}
