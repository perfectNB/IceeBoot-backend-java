/**
 * 权限验证注解
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.common.annotation;

import java.lang.annotation.*;

/**
 * 权限验证注解
 * 用于标记需要权限验证的接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限编码
     * 支持多个权限，满足其中一个即可
     * 
     * @return 权限编码数组
     */
    String[] value() default {};
    
    /**
     * 权限编码（单个）
     * 与value()二选一使用
     * 
     * @return 权限编码
     */
    String code() default "";
    
    /**
     * 权限描述
     * 
     * @return 权限描述
     */
    String description() default "";
    
    /**
     * 是否需要所有权限都满足
     * true: 需要满足所有权限（AND）
     * false: 满足任意一个权限即可（OR）
     * 
     * @return 是否需要所有权限
     */
    boolean requireAll() default false;
    
    /**
     * 是否启用动态权限查询
     * true: 从Redis中根据URL动态查询权限编码
     * false: 使用注解中指定的权限编码
     * 
     * @return 是否启用动态权限查询
     */
    boolean dynamic() default false;
}