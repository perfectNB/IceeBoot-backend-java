package com.iceeboot.common.annotation;

import java.lang.annotation.*;


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

