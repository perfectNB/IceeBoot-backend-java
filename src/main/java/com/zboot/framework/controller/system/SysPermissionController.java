/**
 * 权限管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.controller.system;

import com.zboot.common.annotation.RequirePermission;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.core.domain.R;
import com.zboot.framework.dto.system.PermissionDTO;
import com.zboot.framework.dto.system.SysPermissionQueryDTO;
import com.zboot.framework.service.system.ISysPermissionService;
import com.zboot.framework.vo.system.SysPermissionVO;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Tag(name = "权限管理", description = "权限增删改查、状态管理等接口")
@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
public class SysPermissionController {


    private final ISysPermissionService permissionService;

    @Operation(summary = "分页查询权限列表", description = "根据条件分页查询权限列表")
    @GetMapping("/page")
    @RequirePermission(dynamic = true)
    public R<PageResult<SysPermissionVO>> getPermissionPage(SysPermissionQueryDTO query) {
        PageResult<SysPermissionVO> pageResult = permissionService.getPermissionPage(query);
        return R.ok(pageResult);
    }


    @Operation(summary = "获取权限树", description = "获取完整的权限树结构")
    @GetMapping("/tree")
    @RequirePermission(dynamic = true)
    public R<List<SysPermissionVO>> getPermissionTree() {
        List<SysPermissionVO> permissionTree = permissionService.getPermissionTree();
        return R.ok(permissionTree);
    }


    @Operation(summary = "获取权限详情", description = "根据权限ID获取权限详细信息")
    @GetMapping("/{id}")
    @RequirePermission(dynamic = true)
    public R<SysPermissionVO> getPermissionById(@PathVariable Long id) {
        SysPermissionVO permissionVO = permissionService.getPermissionById(id);
        return R.ok(permissionVO);
    }


    @Operation(summary = "新增权限", description = "创建新权限")
    @PostMapping
    @RequirePermission(dynamic = true)
    public R<Void> addPermission(@Validated @RequestBody PermissionDTO permissionDTO) {
        boolean success = permissionService.addPermission(permissionDTO);
        return success ? R.ok(ResponseMessage.PERMISSION_CREATE_SUCCESS) : R.fail(ResponseMessage.PERMISSION_CREATE_FAIL);
    }


    @Operation(summary = "更新权限", description = "更新权限信息")
    @PutMapping
    @RequirePermission(dynamic = true)
    public R<Void> updatePermission(@Validated @RequestBody PermissionDTO permissionDTO) {
        boolean success = permissionService.updatePermission(permissionDTO);
        return success ? R.ok(ResponseMessage.PERMISSION_UPDATE_SUCCESS) : R.fail(ResponseMessage.PERMISSION_UPDATE_FAIL);
    }


    @Operation(summary = "删除权限", description = "批量删除权限")
    @DeleteMapping
    @RequirePermission(dynamic = true)
    public R<Void> deletePermissions(@RequestBody List<Long> ids) {
        boolean success = permissionService.deletePermissions(ids);
        return success ? R.ok(ResponseMessage.PERMISSION_DELETE_SUCCESS) : R.fail(ResponseMessage.PERMISSION_DELETE_FAIL);
    }


    @Operation(summary = "修改权限状态", description = "启用或禁用权限")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        boolean success = permissionService.updateStatus(id, status);
        return success ? R.ok(ResponseMessage.PERMISSION_UPDATE_SUCCESS) : R.fail(ResponseMessage.PERMISSION_UPDATE_FAIL);
    }





    @Operation(summary = "获取子权限", description = "根据父级ID获取子权限列表")
    @GetMapping("/children/{parentId}")
    @RequirePermission(dynamic = true)
    public R<List<SysPermissionVO>> getChildrenByParentId(@PathVariable Long parentId) {
        List<SysPermissionVO> children = permissionService.getChildrenByParentId(parentId);
        return R.ok(children);
    }
}