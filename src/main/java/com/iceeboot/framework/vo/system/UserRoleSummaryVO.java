package com.iceeboot.framework.vo.system;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 用户列表中的角色摘要，不加载角色菜单和权限树。 */
@Data
public class UserRoleSummaryVO {
    @JsonIgnore
    @Schema(hidden = true)
    private Long userId;

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色状态")
    private String status;

    @Schema(description = "排序")
    private Integer sort;
}
