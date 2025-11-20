/**
 * 数据权限切面
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
package com.iceeboot.common.aspect;

import com.iceeboot.common.annotation.DataPermission;
import com.iceeboot.common.exception.DataPermissionException;
import com.iceeboot.common.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * 数据权限切面
 * 在方法执行后检查返回值的数据权限
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
@Aspect
@Component
@Slf4j
public class DataPermissionAspect {

    /**
     * 环绕通知：拦截带@DataPermission注解的方法
     */
    @Around("@annotation(com.iceeboot.common.annotation.DataPermission)")
    public Object checkDataPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取注解
        DataPermission dataPermission = method.getAnnotation(DataPermission.class);
        if (dataPermission == null) {
            return joinPoint.proceed();
        }
        
        // 执行目标方法
        Object result = joinPoint.proceed();
        
        // 如果返回值为null，直接返回
        if (result == null) {
            return result;
        }
        
        // 获取当前登录用户ID
        Long currentUserId = TokenUtils.getCurrentUserId();
        if (currentUserId == null) {
            log.warn("数据权限检查 - 用户未登录，方法: {}", method.getName());
            throw new DataPermissionException("用户未登录");
        }
        
        // 如果返回值是集合类型，不处理（集合类型应该在SQL层面过滤）
        if (result instanceof Collection) {
            log.warn("数据权限检查 - 返回值是集合类型，建议在SQL层面过滤，方法: {}", method.getName());
            return result;
        }
        
        // 检查数据权限
        String fieldName = dataPermission.field();
        String errorMsg = dataPermission.errorMsg();
        boolean allowAdmin = dataPermission.allowAdmin();
        
        try {
            // 获取字段值
            Object ownerValue = getFieldValue(result, fieldName);
            
            if (ownerValue == null) {
                log.warn("数据权限检查 - 字段{}的值为null，方法: {}", fieldName, method.getName());
                // 字段为null，认为是公共资源，允许访问
                return result;
            }
            
            // 转换为Long类型
            Long ownerId = convertToLong(ownerValue);
            if (ownerId == null) {
                log.error("数据权限检查 - 字段{}的值无法转换为Long类型，值: {}, 方法: {}", 
                         fieldName, ownerValue, method.getName());
                throw new DataPermissionException("数据权限配置错误");
            }
            
            // 检查是否是资源所有者
            if (!currentUserId.equals(ownerId)) {
                // TODO: 如果allowAdmin=true，这里可以检查是否是管理员
                // 现在暂时不实现管理员跳过功能
                log.warn("数据权限检查失败 - 用户{}尝试访问用户{}的资源，方法: {}", 
                        currentUserId, ownerId, method.getName());
                throw new DataPermissionException(errorMsg);
            }
            
            log.debug("数据权限检查通过 - 用户{}，方法: {}", currentUserId, method.getName());
            
        } catch (DataPermissionException e) {
            throw e;
        } catch (Exception e) {
            log.error("数据权限检查异常 - 方法: {}, 错误: {}", method.getName(), e.getMessage(), e);
            throw new DataPermissionException("数据权限检查失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取字段值（支持嵌套字段，如：user.id）
     */
    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        
        // 支持嵌套字段（如：user.id）
        String[] fieldNames = fieldName.split("\\.");
        Object currentObj = obj;
        
        for (String name : fieldNames) {
            if (currentObj == null) {
                return null;
            }
            
            // 获取字段
            Field field = findField(currentObj.getClass(), name);
            if (field == null) {
                throw new NoSuchFieldException("字段不存在: " + name + " in " + currentObj.getClass().getName());
            }
            
            // 设置可访问
            field.setAccessible(true);
            
            // 获取字段值
            currentObj = field.get(currentObj);
        }
        
        return currentObj;
    }
    
    /**
     * 查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 继续查找父类
                currentClass = currentClass.getSuperclass();
            }
        }
        
        return null;
    }
    
    /**
     * 转换为Long类型
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Long) {
            return (Long) value;
        }
        
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        // 尝试toString后转换
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

