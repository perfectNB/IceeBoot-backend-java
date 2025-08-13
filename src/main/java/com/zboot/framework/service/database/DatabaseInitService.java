/**
 * 数据库初始化服务
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.database;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zboot.common.core.domain.ZbootDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

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
 * @date 2025-08-11
 */
@Slf4j
@Service
public class DatabaseInitService implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ApplicationContext applicationContext;

    private static final String ENTITY_PACKAGE = "com.zboot.framework.entity";
    private static final String RESOURCE_PATTERN = "classpath*:com/zboot/**/*.class";
    
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("数据库初始化服务已启动");
        log.info("开始检查并创建数据库表...");
        
        try {
            // 扫描并创建实体类对应的表
            Set<Class<?>> entityClasses = scanEntityClasses();
            for (Class<?> entityClass : entityClasses) {
                processTable(entityClass);
            }
            log.info("数据库表检查完成，共处理 {} 个实体类", entityClasses.size());
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }

    /**
     * 扫描实体类
     */
    private Set<Class<?>> scanEntityClasses() throws Exception {
        Set<Class<?>> entityClasses = new HashSet<>();
        
        try {
            Resource[] resources = resourcePatternResolver.getResources(RESOURCE_PATTERN);
            
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    String className = metadataReader.getClassMetadata().getClassName();
                    
                    try {
                        Class<?> clazz = Class.forName(className);
                        
                        // 检查是否有@TableName注解且继承自ZbootDO
                        if (clazz.isAnnotationPresent(TableName.class) && 
                            ZbootDO.class.isAssignableFrom(clazz) &&
                            !clazz.equals(ZbootDO.class)) {
                            entityClasses.add(clazz);
                            log.debug("发现实体类: {}", className);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        log.warn("无法加载类: {}, 错误: {}", className, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描实体类失败", e);
        }
        
        return entityClasses;
    }
    
    /**
     * 处理表（创建表或增量更新字段）
     */
    private void processTable(Class<?> entityClass) {
        TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
        if (tableNameAnnotation == null) {
            return;
        }
        
        String tableName = tableNameAnnotation.value();
        
        try {
            // 检查表是否存在
            if (tableExists(tableName)) {
                log.debug("表 {} 已存在，检查字段", tableName);
                // 表存在，检查并添加缺失的字段
                addMissingColumns(entityClass, tableName);
            } else {
                // 表不存在，创建表
                String createTableSql = generateCreateTableSql(entityClass, tableName);
                jdbcTemplate.execute(createTableSql);
                log.info("成功创建表: {}", tableName);
            }
            
        } catch (Exception e) {
            log.error("处理表 {} 失败", tableName, e);
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
     * 添加缺失的字段
     */
    private void addMissingColumns(Class<?> entityClass, String tableName) {
        try {
            // 获取表中现有的字段
            Set<String> existingColumns = getExistingColumns(tableName);
            
            // 获取实体类中定义的所有字段（包括父类字段）
            Map<String, String> entityColumns = getAllEntityColumns(entityClass);
            
            // 找出缺失的字段并添加
            for (Map.Entry<String, String> entry : entityColumns.entrySet()) {
                String columnName = entry.getKey();
                String columnDefinition = entry.getValue();
                
                if (!existingColumns.contains(columnName.toLowerCase())) {
                    String addColumnSql = String.format("ALTER TABLE `%s` ADD COLUMN %s", tableName, columnDefinition);
                    jdbcTemplate.execute(addColumnSql);
                    log.info("为表 {} 添加字段: {}", tableName, columnName);
                }
            }
            
        } catch (Exception e) {
            log.error("为表 {} 添加缺失字段失败", tableName, e);
        }
    }
    
    /**
     * 获取表中现有的字段名
     */
    private Set<String> getExistingColumns(String tableName) {
        Set<String> columns = new HashSet<>();
        try {
            String sql = "SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?";
            List<String> columnNames = jdbcTemplate.queryForList(sql, String.class, tableName);
            for (String columnName : columnNames) {
                columns.add(columnName.toLowerCase());
            }
        } catch (Exception e) {
            log.error("获取表 {} 的字段信息失败", tableName, e);
        }
        return columns;
    }
    
    /**
     * 获取实体类中所有字段（包括父类字段）
     */
    private Map<String, String> getAllEntityColumns(Class<?> entityClass) {
        Map<String, String> columns = new LinkedHashMap<>();
        
        // 添加基础字段（来自ZbootDO）
        addBaseColumnsToMap(columns);
        
        // 添加当前类的字段
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
                StringBuilder columnDef = new StringBuilder();
                columnDef.append("`").append(columnName).append("` ").append(columnType);
                
                // 添加注释
                if (comment != null && !comment.isEmpty()) {
                    columnDef.append(" COMMENT '").append(comment).append("'");
                }
                
                columns.put(columnName, columnDef.toString());
            }
        }
        
        return columns;
    }
    
    /**
     * 添加基础字段到Map中
     */
    private void addBaseColumnsToMap(Map<String, String> columns) {
        columns.put("id", "`id` bigint NOT NULL COMMENT '主键ID'");
        columns.put("create_by", "`create_by` varchar(64) DEFAULT NULL COMMENT '创建者'");
        columns.put("create_time", "`create_time` datetime DEFAULT NULL COMMENT '创建时间'");
        columns.put("update_by", "`update_by` varchar(64) DEFAULT NULL COMMENT '更新者'");
        columns.put("update_time", "`update_time` datetime DEFAULT NULL COMMENT '更新时间'");
        columns.put("del_flag", "`del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）'");
    }

    /**
     * 生成建表SQL
     */
    private String generateCreateTableSql(Class<?> entityClass, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(tableName).append("` (\n");
        
        // 获取所有字段定义
        Map<String, String> entityColumns = getAllEntityColumns(entityClass);
        List<String> columnDefinitions = new ArrayList<>();
        
        for (String columnDefinition : entityColumns.values()) {
            columnDefinitions.add("  " + columnDefinition);
        }
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append(",\n  PRIMARY KEY (`id`)");
        sql.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='").append(getTableComment(entityClass)).append("';");
        
        return sql.toString();
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