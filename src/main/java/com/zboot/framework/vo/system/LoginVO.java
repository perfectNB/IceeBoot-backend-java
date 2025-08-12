/**
 * 登录响应视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "用户信息")
    private SysUserVO user;

    @Schema(description = "过期时间（秒）")
    private Long expires;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";
}