/**
 * 数据库初始化服务
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.database;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

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
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
public class DatabaseInitService implements ApplicationRunner {

    private static final int DECIMAL_PRECISION = 18;
    private final JdbcTemplate jdbcTemplate;


    private static final String ENTITY_PACKAGE = "com.iceeboot.framework.entity";
    private static final String RESOURCE_PATTERN = "classpath*:com/iceeboot/**/*.class";
    
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
                        
                        // 检查是否有@TableName注解且继承自iceebootDO
                        if (clazz.isAnnotationPresent(TableName.class) && 
                            IceeBootDO.class.isAssignableFrom(clazz) &&
                            !clazz.equals(IceeBootDO.class)) {
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
                log.info("表 {} 已存在，检查字段", tableName);
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
            Map<String, ColumnMetadata> existingColumns = getExistingColumns(tableName);
            
            // 获取实体类中定义的所有字段（包括父类字段）
            Map<String, String> entityColumns = getAllEntityColumns(entityClass);
            
            // 找出缺失的字段并添加
            for (Map.Entry<String, String> entry : entityColumns.entrySet()) {
                String columnName = entry.getKey();
                String columnDefinition = entry.getValue();
                
                ColumnMetadata existingColumn = existingColumns.get(columnName.toLowerCase());
                if (existingColumn == null) {
                    // 兼容已有自然/复合主键表，不能再补一个没有索引的 AUTO_INCREMENT id。
                    if ("id".equals(columnName) && hasPrimaryKey(tableName)) continue;
                    String addColumnSql = String.format("ALTER TABLE `%s` ADD COLUMN %s", tableName, columnDefinition);
                    jdbcTemplate.execute(addColumnSql);
                    log.info("为表 {} 添加字段: {}", tableName, columnName);
                }
            }

            alignExplicitDecimalColumns(entityClass, tableName, existingColumns);
            
        } catch (Exception e) {
            log.error("为表 {} 添加缺失字段失败", tableName, e);
        }
    }
    
    private boolean hasPrimaryKey(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=? AND index_name='PRIMARY'", Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * 获取表中现有的字段名
     */
    private Map<String, ColumnMetadata> getExistingColumns(String tableName) {
        Map<String, ColumnMetadata> columns = new HashMap<>();
        try {
            String sql = """
                    SELECT COLUMN_NAME, COLUMN_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE,
                           IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = ?
                    """;
            List<ColumnMetadata> columnMetadata = jdbcTemplate.query(sql, (rs, rowNum) -> new ColumnMetadata(
                    rs.getString("COLUMN_NAME"),
                    rs.getString("COLUMN_TYPE"),
                    toInteger(rs.getObject("NUMERIC_PRECISION")),
                    toInteger(rs.getObject("NUMERIC_SCALE")),
                    "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                    rs.getObject("COLUMN_DEFAULT"),
                    rs.getString("COLUMN_COMMENT")
            ), tableName);
            for (ColumnMetadata metadata : columnMetadata) {
                columns.put(metadata.name().toLowerCase(), metadata);
            }
        } catch (Exception e) {
            log.error("获取表 {} 的字段信息失败", tableName, e);
        }
        return columns;
    }

    /**
     * 只对显式声明 numericScale 的 BigDecimal 字段校准精度，避免影响充值、支付等两位小数字段。
     */
    private void alignExplicitDecimalColumns(Class<?> entityClass,
                                             String tableName,
                                             Map<String, ColumnMetadata> existingColumns) {
        for (Field field : entityClass.getDeclaredFields()) {
            Integer numericScale = getExplicitNumericScale(field);
            if (numericScale == null) {
                continue;
            }

            TableField tableField = field.getAnnotation(TableField.class);
            String columnName = getColumnName(field, tableField);
            ColumnMetadata metadata = existingColumns.get(columnName.toLowerCase());
            if (metadata == null
                    || (Objects.equals(metadata.numericPrecision(), DECIMAL_PRECISION)
                    && Objects.equals(metadata.numericScale(), numericScale))) {
                continue;
            }

            String alterColumnSql = buildDecimalColumnAlterSql(tableName, columnName, numericScale, metadata);
            jdbcTemplate.execute(alterColumnSql);
            log.info("已调整表 {} 字段 {} 精度为 DECIMAL({},{})",
                    tableName, columnName, DECIMAL_PRECISION, numericScale);
        }
    }

    private String buildDecimalColumnAlterSql(String tableName,
                                              String columnName,
                                              int numericScale,
                                              ColumnMetadata metadata) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ")
                .append(quoteIdentifier(tableName))
                .append(" MODIFY COLUMN ")
                .append(quoteIdentifier(columnName))
                .append(" DECIMAL(")
                .append(DECIMAL_PRECISION)
                .append(",")
                .append(numericScale)
                .append(")");

        if (metadata.columnType() != null
                && metadata.columnType().toLowerCase().contains("unsigned")) {
            sql.append(" UNSIGNED");
        }
        sql.append(metadata.nullable() ? " NULL" : " NOT NULL");
        if (metadata.defaultValue() != null) {
            sql.append(" DEFAULT '")
                    .append(escapeSqlLiteral(String.valueOf(metadata.defaultValue())))
                    .append("'");
        } else if (metadata.nullable()) {
            sql.append(" DEFAULT NULL");
        }
        if (metadata.comment() != null && !metadata.comment().isEmpty()) {
            sql.append(" COMMENT '")
                    .append(escapeSqlLiteral(metadata.comment()))
                    .append("'");
        }
        return sql.toString();
    }

    private Integer getExplicitNumericScale(Field field) {
        if (field.getType() != BigDecimal.class) {
            return null;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField == null || tableField.numericScale().isBlank()) {
            return null;
        }

        int numericScale;
        try {
            numericScale = Integer.parseInt(tableField.numericScale());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("字段 " + field.getName() + " 的 numericScale 必须是整数", e);
        }
        if (numericScale < 0 || numericScale > DECIMAL_PRECISION) {
            throw new IllegalArgumentException(
                    "字段 " + field.getName() + " 的 numericScale 必须在 0 到 " + DECIMAL_PRECISION + " 之间");
        }
        return numericScale;
    }

    private static Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }
    
    /**
     * 获取实体类中所有字段（包括父类字段）
     */
    private Map<String, String> getAllEntityColumns(Class<?> entityClass) {
        Map<String, String> columns = new LinkedHashMap<>();
        
        // 添加基础字段（来自iceebootDO）
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
        columns.put("id", "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'");
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
        sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
        
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
    String getColumnType(Field field) {
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && tableField.jdbcType() == org.apache.ibatis.type.JdbcType.LONGVARCHAR) {
                return "longtext";
            }
            return "content".equalsIgnoreCase(field.getName()) ? "text" : "varchar(255)";
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
            Integer numericScale = getExplicitNumericScale(field);
            return numericScale == null
                    ? "decimal(10,2)"
                    : "decimal(" + DECIMAL_PRECISION + "," + numericScale + ")";
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

    private record ColumnMetadata(String name,
                                  String columnType,
                                  Integer numericPrecision,
                                  Integer numericScale,
                                  boolean nullable,
                                  Object defaultValue,
                                  String comment) {
    }
}
