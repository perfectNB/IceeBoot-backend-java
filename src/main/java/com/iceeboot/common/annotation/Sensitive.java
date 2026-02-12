package com.iceeboot.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.iceeboot.common.utils.SensitiveSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段脱敏注解
 * 用于VO层字段，序列化时自动脱敏
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /**
     * 前面保留位数
     */
    int prefixKeep() default 4;

    /**
     * 后面保留位数
     */
    int suffixKeep() default 4;
}
