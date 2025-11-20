/**
 * 数据权限注解
 * 用于验证用户是否有权访问指定资源
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
package com.iceeboot.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于方法级别的数据所有权验证
 * 
 * 使用场景：
 * 1. 单条数据查询：验证返回的实体是否属于当前用户
 * 2. 数据修改/删除：验证操作的实体是否属于当前用户
 * 
 * 使用示例：
 * <pre>
 * @DataPermission
 * public PayRecordVO getPayRecordById(Long id) {
 *     // 方法返回值会被自动检查createBy字段
 *     return payRecordService.getById(id);
 * }
 * 
 * @DataPermission(field = "ownerId")
 * public OrderVO getOrder(Long id) {
 *     // 检查返回值的ownerId字段
 *     return orderService.getById(id);
 * }
 * 
 * @DataPermission(errorMsg = "您无权访问此订单")
 * public OrderVO getOrder(Long id) {
 *     // 自定义错误消息
 *     return orderService.getById(id);
 * }
 * </pre>
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 需要检查的字段名
     * 支持嵌套字段（使用点号分隔，如：user.id）
     * 默认检查createBy字段
     * 
     * @return 字段名
     */
    String field() default "createBy";
    
    /**
     * 自定义错误消息
     * 默认："这不是您的资源"
     * 
     * @return 错误消息
     */
    String errorMsg() default "这不是您的资源";
    
    /**
     * 是否允许管理员跳过检查
     * 默认：true（管理员可以访问所有资源）
     * 
     * @return 是否允许管理员跳过
     */
    boolean allowAdmin() default true;
}

