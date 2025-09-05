/**
 * 菜单管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.controller.system;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.create.system.MenuDTO;
import com.zboot.framework.dto.update.system.MenuUpdateDTO;
import com.zboot.framework.service.system.ISysMenuService;
import com.zboot.framework.vo.system.SysMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
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




    @Operation(summary = "查询用户菜单列表", description = "根据条件分页查询菜单列表")
    @GetMapping("/user")
    @RequirePermission(dynamic = true)
    public R<List<SysMenuVO>> getUserMenu() {
        List<SysMenuVO> userMenu = menuService.getUserMenu();
        return R.ok(userMenu);
    }


    @Operation(summary = "新增菜单", description = "创建新菜单")
    @PostMapping
//    @RequirePermission(dynamic = true)
    public R<Void> addMenu(@Validated @RequestBody MenuDTO menuDTO) {
        boolean success = menuService.addMenu(menuDTO);
        return success ? R.ok(ResponseMessage.MENU_CREATE_SUCCESS) : R.fail(ResponseMessage.MENU_CREATE_FAIL);
    }


    @Operation(summary = "更新菜单", description = "更新菜单信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updateMenu(@Validated @RequestBody MenuUpdateDTO menuDTO) {
        boolean success = menuService.updateMenu(menuDTO);
        return success ? R.ok(ResponseMessage.MENU_UPDATE_SUCCESS) : R.fail(ResponseMessage.MENU_UPDATE_FAIL);
    }

    @Operation(summary = "删除菜单", description = "批量删除菜单")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deleteMenus(@RequestBody List<Long> ids) {
        boolean success = menuService.deleteMenus(ids);
        return success ? R.ok(ResponseMessage.MENU_DELETE_SUCCESS) : R.fail(ResponseMessage.MENU_DELETE_FAIL);
    }


    @Operation(summary = "修改菜单状态", description = "启用或禁用菜单")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        boolean success = menuService.updateStatus(id, status);
        return success ? R.ok(ResponseMessage.MENU_UPDATE_SUCCESS) : R.fail(ResponseMessage.MENU_UPDATE_FAIL);
    }


    @Operation(summary = "检查菜单名称", description = "检查菜单名称是否已存在")
    @GetMapping("/check/name")
    public R<Boolean> checkName(@RequestParam String name, @RequestParam(required = false) Long excludeId) {
        boolean exists = menuService.existsName(name, excludeId);
        return R.ok(exists);
    }


    @Operation(summary = "获取子菜单", description = "根据父级ID获取子菜单列表")
    @GetMapping("/children/{parentId}")
    public R<List<SysMenuVO>> getChildrenByParentId(@PathVariable Long parentId) {
        List<SysMenuVO> children = menuService.getChildrenByParentId(parentId);
        return R.ok(children);
    }
}