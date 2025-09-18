
package com.iceeboot.framework.llm.service;

import com.iceeboot.framework.llm.query.LogQueryRequest;
import com.iceeboot.framework.llm.model.LLMQueryResponse;
import com.iceeboot.framework.llm.model.LLMQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 日志查询 Function Calling 实现
 * 基于 Spring AI Alibaba 的 Function Calling 功能
 *
 * @author CodeIcee
 * @date 2025-09-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogQueryFunction implements Function<LogQueryRequest, LLMQueryResponse> {

    private final JdbcTemplate jdbcTemplate;
    
    // SQL 注入防护：只允许 SELECT 语句
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "^\\s*SELECT\\s+.*", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 危险关键词黑名单
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE ", "TRUNCATE",
        "EXEC", "EXECUTE", "UNION", "--", "/*", "*/", ";"
    );
    
    // 查询结果限制
    private static final int MAX_RESULT_SIZE = 1000;
    
    // 日志表名
    private static final String LOG_TABLE = "t_log";

    @Override
    public LLMQueryResponse apply(LogQueryRequest request) {
        try {
            log.info("执行日志查询函数，查询意图: {}", request.getQueryIntent());
            
            // 1. 提供表结构和查询指导信息
            String tableInfo = getLogTableInfo();
            String queryGuidance = getQueryGuidance(request);
            
            // 2. 生成优化的SQL查询（使用中文字段别名）
            String sql = generateOptimizedSql(request);
            
            // 3. 执行查询
            LLMQueryResult queryResult = executeQuery(sql);
            
            // 4. 格式化返回结果
            if (queryResult.isSuccess()) {
                String formattedResult = formatResultForDisplay(queryResult, request.getLimit());
                
                String response = tableInfo + "\n\n" + queryGuidance + "\n\n" + formattedResult +
                    "\n\n📊 查询统计：共找到 " + queryResult.getTotalCount() + " 条日志记录" +
                    "\n🔍 执行SQL：" + sql +
                    "\n\n💡 提示：如需查看更多详细信息，请使用更具体的筛选条件。";
                    
                return LLMQueryResponse.success(
                    response,
                    sql,
                    queryResult.getTotalCount()
                );
            } else {
                return LLMQueryResponse.error(queryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("日志查询函数执行失败", e);
            return LLMQueryResponse.error("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 生成优化的SQL查询（使用中文字段别名）
     */
    private String generateOptimizedSql(LogQueryRequest request) {
        // 构建基础查询，选择需要的字段并使用中文别名
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("id as '日志ID', ");
        sql.append("level as '日志级别', ");
        sql.append("module as '所属模块', ");
        sql.append("message as '日志消息', ");
        sql.append("class_name as '类名', ");
        sql.append("summary as '接口描述', ");
        sql.append("path as '请求路径', ");
        sql.append("DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') as '创建时间' ");
        sql.append("FROM ").append(LOG_TABLE);
        
        List<String> whereConditions = new ArrayList<>();
        
        // 智能解析查询条件
        parseSmartQueryConditions(request, whereConditions);
        
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereConditions));
        }
        
        // 添加排序和限制
        sql.append(" ORDER BY create_time DESC");
        
        int limit = request.getLimit() != null ? request.getLimit() : 20; // 默认20条，提升速度
        sql.append(" LIMIT ").append(limit);
        
        return sql.toString();
    }
    
    /**
     * 生成日志查询 SQL（保留原方法作为备用）
     */
    private String generateLogQuerySql(LogQueryRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(LOG_TABLE);
        
        List<String> whereConditions = new ArrayList<>();
        
        // 解析查询条件
        parseLogQueryConditions(request, whereConditions);
        
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereConditions));
        }
        
        // 添加排序和限制
        sql.append(" ORDER BY create_time DESC");
        
        // 严格按照用户要求的数量返回，不做额外限制
        int limit = request.getLimit() != null ? request.getLimit() : 100;
        sql.append(" LIMIT ").append(limit);
        
        return sql.toString();
    }

    /**
     * 智能解析查询条件
     */
    private void parseSmartQueryConditions(LogQueryRequest request, List<String> whereConditions) {
        String intent = request.getQueryIntent().toLowerCase();
        
        // 1. 日志级别过滤
        if (request.getLevel() != null && !request.getLevel().trim().isEmpty()) {
            whereConditions.add("level = '" + request.getLevel().toUpperCase() + "'");
        } else {
            // 从查询意图中提取日志级别
            if (intent.contains("错误") || intent.contains("error")) {
                whereConditions.add("level = 'ERROR'");
            } else if (intent.contains("警告") || intent.contains("warn")) {
                whereConditions.add("level = 'WARN'");
            } else if (intent.contains("信息") || intent.contains("info")) {
                whereConditions.add("level = 'INFO'");
            } else if (intent.contains("调试") || intent.contains("debug")) {
                whereConditions.add("level = 'DEBUG'");
            }
        }
        
        // 2. 模块过滤
        if (request.getModule() != null && !request.getModule().trim().isEmpty()) {
            whereConditions.add("module LIKE '%" + request.getModule() + "%'");
        } else {
            // 从查询意图中提取模块信息
            if (intent.contains("用户")) {
                whereConditions.add("module LIKE '%用户%'");
            } else if (intent.contains("权限")) {
                whereConditions.add("module LIKE '%权限%'");
            } else if (intent.contains("系统")) {
                whereConditions.add("module LIKE '%系统%'");
            } else if (intent.contains("登录")) {
                whereConditions.add("module LIKE '%登录%'");
            }
        }
        
//        // 3. 关键词模糊匹配（使用实际存在的字段）
//        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
//            whereConditions.add("(message LIKE '%" + request.getKeyword() + "%' OR summary LIKE '%" + request.getKeyword() + "%' OR class_name LIKE '%" + request.getKeyword() + "%')");
//        }
        
        // 4. 用户ID过滤（使用create_by字段）
        if (request.getUserId() != null && !request.getUserId().trim().isEmpty()) {
            whereConditions.add("create_by LIKE '%" + request.getUserId() + "%'");
        }
        
        // 5. 时间范围过滤
        String timeCondition = parseTimeRange(request.getTimeRange(), intent);
        if (timeCondition != null) {
            whereConditions.add(timeCondition);
        }
        
        // 6. 排除删除的记录
        whereConditions.add("del_flag = 0");
    }
    
    /**
     * 解析日志查询条件（保留原方法作为备用）
     */
    private void parseLogQueryConditions(LogQueryRequest request, List<String> whereConditions) {
        String intent = request.getQueryIntent().toLowerCase();
        
        // 1. 日志级别过滤
        if (request.getLevel() != null && !request.getLevel().trim().isEmpty()) {
            whereConditions.add("level = '" + request.getLevel().toUpperCase() + "'");
        } else {
            // 从查询意图中提取日志级别
            if (intent.contains("错误") || intent.contains("error")) {
                whereConditions.add("level = 'ERROR'");
            } else if (intent.contains("警告") || intent.contains("warn")) {
                whereConditions.add("level = 'WARN'");
            } else if (intent.contains("信息") || intent.contains("info")) {
                whereConditions.add("level = 'INFO'");
            } else if (intent.contains("调试") || intent.contains("debug")) {
                whereConditions.add("level = 'DEBUG'");
            }
        }
        
        // 2. 模块过滤
        if (request.getModule() != null && !request.getModule().trim().isEmpty()) {
            whereConditions.add("module LIKE '%" + request.getModule() + "%'");
        } else {
            // 从查询意图中提取模块信息
            if (intent.contains("用户")) {
                whereConditions.add("module LIKE '%用户%'");
            } else if (intent.contains("权限")) {
                whereConditions.add("module LIKE '%权限%'");
            } else if (intent.contains("系统")) {
                whereConditions.add("module LIKE '%系统%'");
            } else if (intent.contains("登录")) {
                whereConditions.add("module LIKE '%登录%'");
            }
        }
        
        // 3. 关键词模糊匹配（使用实际存在的字段）
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            whereConditions.add("(message LIKE '%" + request.getKeyword() + "%' OR summary LIKE '%" + request.getKeyword() + "%' OR class_name LIKE '%" + request.getKeyword() + "%')");
        }
        
        // 4. 用户ID过滤（使用create_by字段）
        if (request.getUserId() != null && !request.getUserId().trim().isEmpty()) {
            whereConditions.add("create_by LIKE '%" + request.getUserId() + "%'");
        }
        
        // 5. 时间范围过滤
        String timeCondition = parseTimeRange(request.getTimeRange(), intent);
        if (timeCondition != null) {
            whereConditions.add(timeCondition);
        }
        
        // 6. 排除删除的记录
        whereConditions.add("del_flag = 0");
    }

    /**
     * 解析时间范围
     */
    private String parseTimeRange(String timeRange, String intent) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // 优先使用明确的时间范围参数
        if (timeRange != null && !timeRange.trim().isEmpty()) {
            String lowerTimeRange = timeRange.toLowerCase();
            if (lowerTimeRange.contains("今天")) {
                LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                return "create_time >= '" + startOfDay.format(formatter) + "'";
            } else if (lowerTimeRange.contains("昨天")) {
                LocalDateTime startOfYesterday = now.minusDays(1).toLocalDate().atStartOfDay();
                LocalDateTime endOfYesterday = now.toLocalDate().atStartOfDay();
                return "create_time >= '" + startOfYesterday.format(formatter) + "' AND create_time < '" + endOfYesterday.format(formatter) + "'";
            } else if (lowerTimeRange.contains("一周") || lowerTimeRange.contains("7天")) {
                LocalDateTime weekAgo = now.minusDays(7);
                return "create_time >= '" + weekAgo.format(formatter) + "'";
            }
        }
        
        // 从查询意图中提取时间信息
        if (intent.contains("今天")) {
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            return "create_time >= '" + startOfDay.format(formatter) + "'";
        } else if (intent.contains("昨天")) {
            LocalDateTime startOfYesterday = now.minusDays(1).toLocalDate().atStartOfDay();
            LocalDateTime endOfYesterday = now.toLocalDate().atStartOfDay();
            return "create_time >= '" + startOfYesterday.format(formatter) + "' AND create_time < '" + endOfYesterday.format(formatter) + "'";
        } else if (intent.contains("最近") || intent.contains("近期")) {
            if (intent.contains("天")) {
                // 提取天数
                String[] parts = intent.split("最近|近期");
                if (parts.length > 1) {
                    String daysPart = parts[1].replaceAll("[^0-9]", "");
                    if (!daysPart.isEmpty()) {
                        int days = Integer.parseInt(daysPart);
                        LocalDateTime daysAgo = now.minusDays(days);
                        return "create_time >= '" + daysAgo.format(formatter) + "'";
                    }
                }
            } else if (intent.contains("小时")) {
                // 提取小时数
                String[] parts = intent.split("最近|近期");
                if (parts.length > 1) {
                    String hoursPart = parts[1].replaceAll("[^0-9]", "");
                    if (!hoursPart.isEmpty()) {
                        int hours = Integer.parseInt(hoursPart);
                        LocalDateTime hoursAgo = now.minusHours(hours);
                        return "create_time >= '" + hoursAgo.format(formatter) + "'";
                    }
                }
            } else {
                // 默认最近24小时
                LocalDateTime dayAgo = now.minusDays(1);
                return "create_time >= '" + dayAgo.format(formatter) + "'";
            }
        }
        
        return null;
    }

    /**
     * 执行安全的数据库查询
     */
    public LLMQueryResult executeQuery(String sql) {
        try {
            // 1. 安全检查
            if (!isSafeQuery(sql)) {
                return LLMQueryResult.error("不安全的查询语句，只允许 SELECT 操作");
            }
            
            log.info("执行日志查询: {}", sql);
            
            // 2. 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            // 3. 不对结果进行截取，完全按照用户要求返回
            log.info("查询返回 {} 条日志记录", results.size());
            
            return LLMQueryResult.success(results, results.size());
            
        } catch (Exception e) {
            log.error("日志查询执行失败: {}", e.getMessage(), e);
            return LLMQueryResult.error("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 检查查询是否安全
     */
    private boolean isSafeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String upperSql = sql.toUpperCase();
        
        // 1. 检查是否为 SELECT 语句
        if (!SELECT_PATTERN.matcher(sql).matches()) {
            log.warn("非 SELECT 查询被拒绝: {}", sql);
            return false;
        }
        
        // 2. 检查危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword) && !upperSql.contains("CREATE_TIME")) {
                log.warn("包含危险关键词 '{}' 的查询被拒绝: {}", keyword, sql);
                return false;
            }
        }
        
        // 3. 检查是否包含子查询或联合查询（简单检查）
//        if (upperSql.contains("(") && upperSql.contains("SELECT")) {
//            log.warn("包含子查询的复杂查询被拒绝: {}", sql);
//            return false;
//        }
        
        return true;
    }

    /**
     * 格式化查询结果为美观的显示格式
     */
    private String formatResultForDisplay(LLMQueryResult queryResult, Integer requestLimit) {
        List<Map<String, Object>> results = queryResult.getData();
        if (results.isEmpty()) {
            return "📝 查询结果：未找到符合条件的日志记录";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("📝 日志查询结果：\n");
        formatted.append("═══════════════════════════════════════\n");
        
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> record = results.get(i);
            formatted.append(String.format("📋 第 %d 条日志：\n", i + 1));
            
            // 格式化每个字段，使用中文标签
            formatField(formatted, "🆔 日志ID", record.get("日志ID"));
            formatField(formatted, "📊 日志级别", record.get("日志级别"));
            formatField(formatted, "📦 所属模块", record.get("所属模块"));
            formatField(formatted, "💬 日志消息", record.get("日志消息"));
            formatField(formatted, "🏷️ 类名", record.get("类名"));
            formatField(formatted, "📄 接口描述", record.get("接口描述"));
            formatField(formatted, "🔗 请求路径", record.get("请求路径"));
            formatField(formatted, "⏰ 创建时间", record.get("创建时间"));
            
            formatted.append("───────────────────────────────────────\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * 格式化单个字段
     */
    private void formatField(StringBuilder sb, String label, Object value) {
        if (value != null && !value.toString().trim().isEmpty()) {
            String displayValue = value.toString();
            // 限制消息长度，避免过长
            if (label.contains("消息") && displayValue.length() > 100) {
                displayValue = displayValue.substring(0, 100) + "...";
            }
            sb.append(String.format("%s: %s\n", label, displayValue));
        }
    }
    
    /**
     * 获取查询指导信息
     */
    private String getQueryGuidance(LogQueryRequest request) {
        return "🎯 查询分析：\n" +
               "• 查询意图：" + request.getQueryIntent() + "\n" +
               "• 查询条数：" + (request.getLimit() != null ? request.getLimit() : 20) + " 条\n" +
               "• 优化策略：使用中文字段名，精简显示格式，提升查询速度";
    }

    /**
     * 获取日志表结构信息（基于实际的LogDO实体类）
     */
    private String getLogTableInfo() {
        return "【日志表结构信息】\n" +
               "表名: t_log\n" +
               "实际字段（基于LogDO实体类）:\n" +
               "- id: 主键ID (bigint)\n" +
               "- level: 日志级别 (varchar) - ERROR/WARN/INFO/DEBUG\n" +
               "- module: 所属模块 (varchar) - 如用户管理、权限管理等\n" +
               "- message: 日志消息内容 (text)\n" +
               "- thread: 线程名称 (varchar)\n" +
               "- class_name: 类名 (varchar)\n" +
               "- stack_trace: 堆栈跟踪 (text)\n" +
               "- summary: 接口描述 (varchar)\n" +
               "- path: 请求路径 (varchar)\n" +
               "- timestamp: 时间戳 (datetime)\n" +
               "- create_time: 创建时间 (datetime)\n" +
               "- update_time: 更新时间 (datetime)\n" +
               "- create_by: 创建人 (varchar)\n" +
               "- update_by: 更新人 (varchar)\n" +
               "- del_flag: 删除标志 (tinyint) - 0:未删除, 1:已删除\n";
    }
}