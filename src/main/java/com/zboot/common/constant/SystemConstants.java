/**
 * 系统常量类
 * 统一管理系统中使用的各种常量
 * 
 * @author CodeIcee
 * @date 2025-09-02
 */
package com.zboot.common.constant;

/**
 * 系统常量类
 * 统一管理系统中使用的各种常量
 * 
 * @author CodeIcee
 * @date 2025-09-02
 */
public class SystemConstants {

    // ==================== 大模型厂商 ====================
    
    /**
     * 大模型厂商常量
     */
    public static class LLMProvider {
        /** ChatGPT */
        public static final String CHATGPT = "chatgpt";
        
        /** Grok */
        public static final String GROK = "grok";
        
        /** Claude */
        public static final String CLAUDE = "claude";
        
        /** Gemini */
        public static final String GEMINI = "gemini";
        
        /** 文心一言 */
        public static final String WENXIN = "wenxin";
        
        /** 月之暗面 */
        public static final String MOONSHOT = "moonshot";
        
        /** 通义千问 */
        public static final String QWEN = "qwen";
        
        /** DeepSeek */
        public static final String DEEPSEEK = "deepseek";
        
        /** 智谱AI */
        public static final String ZHIPU = "zhipu";
        
        /** 百川智能 */
        public static final String BAICHUAN = "baichuan";
        
        /** 讯飞星火 */
        public static final String SPARK = "spark";
    }

    // ==================== 删除标识 ====================
    
    /**
     * 删除标识常量
     */
    public static class DeleteFlag {
        /** 未删除 */
        public static final Integer NOT_DELETED = 0;
        
        /** 已删除 */
        public static final Integer DELETED = 1;
    }

    // ==================== 状态标识 ====================
    
    /**
     * 通用状态常量
     */
    public static class Status {
        /** 激活/启用 */
        public static final String ACTIVE = "active";
        
        /** 未激活/禁用 */
        public static final String INACTIVE = "inactive";
        
        /** 正常 */
        public static final String NORMAL = "normal";
        
        /** 停用 */
        public static final String DISABLED = "disabled";
    }

    // ==================== Agent类型 ====================
    
    /**
     * AI Agent平台类型
     */
    public static class AgentPlatform {
        /** Coze平台 */
        public static final String COZE = "coze";
        
        /** Dify平台 */
        public static final String DIFY = "dify";
    }
    
    /**
     * AI Agent类型
     */
    public static class AgentType {
        /** 工作流类型 */
        public static final String WORKFLOW = "workflow";
        
        /** 代理类型 */
        public static final String AGENT = "agent";
    }

    // ==================== 任务状态 ====================
    
    /**
     * 定时任务状态
     */
    public static class TaskStatus {
        /** 运行中 */
        public static final String ACTIVE = "active";
        
        /** 已停止 */
        public static final String INACTIVE = "inactive";
        
        /** 暂停 */
        public static final String PAUSED = "paused";
        
        /** 错误 */
        public static final String ERROR = "error";
    }

    // ==================== 作业状态 ====================
    
    /**
     * 作业状态常量
     */
    public static class JobStatus {
        /** 待执行 */
        public static final Integer PENDING = 0;
        
        /** 执行中 */
        public static final Integer RUNNING = 1;
        
        /** 已完成 */
        public static final Integer COMPLETED = 2;
        
        /** 失败 */
        public static final Integer FAILED = 3;
        
        /** 已取消 */
        public static final Integer CANCELLED = 4;
    }

    // ==================== 活动状态 ====================
    
    /**
     * 活动状态常量
     */
    public static class ActivityStatus {
        /** 未开始 */
        public static final Integer NOT_STARTED = 0;
        
        /** 进行中 */
        public static final Integer IN_PROGRESS = 1;
        
        /** 已结束 */
        public static final Integer ENDED = 2;
        
        /** 已取消 */
        public static final Integer CANCELLED = 3;
    }

    // ==================== 优先级 ====================
    
    /**
     * 优先级常量
     */
    public static class Priority {
        /** 低优先级 */
        public static final Integer LOW = 1;
        
        /** 中等优先级 */
        public static final Integer MEDIUM = 2;
        
        /** 高优先级 */
        public static final Integer HIGH = 3;
        
        /** 紧急 */
        public static final Integer URGENT = 4;
    }

    // ==================== 日志级别 ====================
    
    /**
     * 日志级别常量
     */
    public static class LogLevel {
        /** DEBUG级别 */
        public static final String DEBUG = "DEBUG";
        
        /** INFO级别 */
        public static final String INFO = "INFO";
        
        /** WARN级别 */
        public static final String WARN = "WARN";
        
        /** ERROR级别 */
        public static final String ERROR = "ERROR";
    }

    // ==================== 菜单类型 ====================
    
    /**
     * 菜单类型常量
     */
    public static class MenuType {
        /** 目录 */
        public static final String DIRECTORY = "directory";
        
        /** 菜单 */
        public static final String MENU = "menu";
        
        /** 按钮 */
        public static final String BUTTON = "button";
    }

    // ==================== 权限类型 ====================
    
    /**
     * 权限类型常量
     */
    public static class PermissionType {
        /** 菜单权限 */
        public static final String MENU = "menu";
        
        /** 按钮权限 */
        public static final String BUTTON = "button";
        
        /** API权限 */
        public static final String API = "api";
    }

    // ==================== HTTP方法 ====================
    
    /**
     * HTTP请求方法常量
     */
    public static class HttpMethod {
        /** GET请求 */
        public static final String GET = "GET";
        
        /** POST请求 */
        public static final String POST = "POST";
        
        /** PUT请求 */
        public static final String PUT = "PUT";
        
        /** DELETE请求 */
        public static final String DELETE = "DELETE";
        
        /** PATCH请求 */
        public static final String PATCH = "PATCH";
    }

    // ==================== 验证码类型 ====================
    
    /**
     * 验证码类型常量
     */
    public static class CaptchaType {
        /** 数学计算验证码 */
        public static final String MATH = "math";
        
        /** 字符验证码 */
        public static final String CHAR = "char";
        
        /** 图片验证码 */
        public static final String IMAGE = "image";
    }

    // ==================== 性别 ====================
    
    /**
     * 性别常量
     */
    public static class Gender {
        /** 男性 */
        public static final String MALE = "male";
        
        /** 女性 */
        public static final String FEMALE = "female";
        
        /** 未知 */
        public static final String UNKNOWN = "unknown";
    }

    // ==================== 私有构造函数 ====================
    
    private SystemConstants() {
        // 工具类，禁止实例化
    }
}