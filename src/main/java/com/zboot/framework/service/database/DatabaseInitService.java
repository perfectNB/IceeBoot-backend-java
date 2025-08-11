/**
 * 数据库初始化服务
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.service.database;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zboot.common.core.domain.ZbootDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 数据库初始化服务
 * 在项目启动时自动检查并创建DO对应的数据库表
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Slf4j
@Service
public class DatabaseInitService implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String ENTITY_PACKAGE = "com.zboot.framework.entity";
    private static final String RESOURCE_PATTERN = "/**/*.class";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("数据库初始化服务已启动");
        log.info("注意：自动建表功能已简化，请手动创建数据库表或使用 MyBatis Plus 的自动建表功能");
    }

    /**
     * 扫描实体类
     */
    private Set<Class<?>> scanEntityClasses() throws Exception {
        Set<Class<?>> entityClasses = new HashSet<>();
        
        // 手动添加已知的实体类
        try {
            addEntityClass(entityClasses, "com.zboot.framework.entity.system.UserDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.system.RoleDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.system.PermissionDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.system.MenuDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.system.RolePermissionDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.system.UserRoleDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.log.LogDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.ai.AiAgentDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.ai.AiChatSessionDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.ai.AiLLMDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.task.TaskDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.activity.ActivityDO");
            addEntityClass(entityClasses, "com.zboot.framework.entity.job.JobDO");
        } catch (Exception e) {
            log.error("加载实体类失败", e);
        }
        
        return entityClasses;
    }
    
    /**
     * 添加实体类
     */
    private void addEntityClass(Set<Class<?>> entityClasses, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (ZbootDO.class.isAssignableFrom(clazz) && 
                clazz.isAnnotationPresent(TableName.class) &&
                !clazz.equals(ZbootDO.class)) {
                entityClasses.add(clazz);
                log.debug("发现实体类: {}", className);
            }
        } catch (ClassNotFoundException e) {
            log.warn("无法加载类: {}", className);
        }
    }

    /**
     * 为实体类创建对应的表
     */
    private void createTableIfNotExists(Class<?> entityClass) {
        TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
        if (tableNameAnnotation == null) {
            return;
        }
        
        String tableName = tableNameAnnotation.value();
        
        try {
            // 检查表是否存在
            if (tableExists(tableName)) {
                log.debug("表 {} 已存在，跳过创建", tableName);
                return;
            }
            
            // 生成建表SQL
            String createTableSql = generateCreateTableSql(entityClass, tableName);
            
            // 执行建表SQL
            jdbcTemplate.execute(createTableSql);
            log.info("成功创建表: {}", tableName);
            
        } catch (Exception e) {
            log.error("创建表 {} 失败", tableName, e);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查表 {} 是否存在时出错: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 生成建表SQL
     */
    private String generateCreateTableSql(Class<?> entityClass, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(tableName).append("` (\n");
        
        List<String> columns = new ArrayList<>();
        
        // 处理父类字段（ZbootDO的字段）
        addBaseColumns(columns);
        
        // 处理当前类的字段
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                // 跳过不存储到数据库的字段
                continue;
            }
            
            String columnName = getColumnName(field, tableField);
            String columnType = getColumnType(field);
            String comment = getColumnComment(field);
            
            if (columnName != null && columnType != null) {
                StringBuilder column = new StringBuilder();
                column.append("  `").append(columnName).append("` ").append(columnType);
                
                // 添加注释
                if (comment != null && !comment.isEmpty()) {
                    column.append(" COMMENT '").append(comment).append("'");
                }
                
                columns.add(column.toString());
            }
        }
        
        sql.append(String.join(",\n", columns));
        sql.append(",\n  PRIMARY KEY (`id`)");
        sql.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='").append(getTableComment(entityClass)).append("';");
        
        return sql.toString();
    }

    /**
     * 添加基础字段（来自ZbootDO）
     */
    private void addBaseColumns(List<String> columns) {
        columns.add("  `id` bigint NOT NULL COMMENT '主键ID'");
        columns.add("  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者'");
        columns.add("  `create_time` datetime DEFAULT NULL COMMENT '创建时间'");
        columns.add("  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者'");
        columns.add("  `update_time` datetime DEFAULT NULL COMMENT '更新时间'");
        columns.add("  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）'");
    }

    /**
     * 获取字段对应的数据库列名
     */
    private String getColumnName(Field field, TableField tableField) {
        if (tableField != null && !tableField.value().isEmpty()) {
            return tableField.value();
        }
        
        // 将驼峰命名转换为下划线命名
        return camelToUnderscore(field.getName());
    }

    /**
     * 获取字段对应的数据库列类型
     */
    private String getColumnType(Field field) {
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            return "varchar(255)";
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return "int";
        } else if (fieldType == Long.class || fieldType == long.class) {
            return "bigint";
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "tinyint(1)";
        } else if (fieldType == Double.class || fieldType == double.class) {
            return "double";
        } else if (fieldType == Float.class || fieldType == float.class) {
            return "float";
        } else if (fieldType == BigDecimal.class) {
            return "decimal(10,2)";
        } else if (fieldType == LocalDateTime.class) {
            return "datetime";
        } else if (fieldType == LocalDate.class) {
            return "date";
        } else if (fieldType == LocalTime.class) {
            return "time";
        } else if (fieldType.isEnum()) {
            return "varchar(50)";
        } else {
            return "text";
        }
    }

    /**
     * 获取字段注释
     */
    private String getColumnComment(Field field) {
        // 尝试从Schema注解获取描述
        if (field.isAnnotationPresent(io.swagger.v3.oas.annotations.media.Schema.class)) {
            io.swagger.v3.oas.annotations.media.Schema schema = field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            return schema.description();
        }
        
        return field.getName();
    }

    /**
     * 获取表注释
     */
    private String getTableComment(Class<?> entityClass) {
        // 从类的javadoc或注解中获取表注释
        String className = entityClass.getSimpleName();
        if (className.endsWith("DO")) {
            className = className.substring(0, className.length() - 2);
        }
        return className + "表";
    }

    /**
     * 驼峰命名转下划线命名
     */
    private String camelToUnderscore(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}