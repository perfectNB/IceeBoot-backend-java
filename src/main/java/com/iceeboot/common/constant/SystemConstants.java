/**
 * 系统常量类
 * 统一管理系统中使用的各种常量
 * 
 * @author CodeIcee
 * @date 2025-09-02
 */
package com.iceeboot.common.constant;

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


    /**
     * 大模型厂商常量
     */
    public static class SaaModel {
        /** deepseek v3 */
        public static final String DEEPSEEK_MODEL = "deepseek-v3";

        /** 千问 */
        public static final String QWEN_MODEL = "qwen-max";


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

        /** 已读 */
        public static final String READ = "read";

        /** 未读 */
        public static final String UNREAD = "unread";
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



    public static class WorkflowValue {
        public static final String DEFAULT_BPMN_FILE_NAME = "process.bpmn";
        public static final String DEFAULT_DELETE_REASON = "手动删除";
        public static final String DEFAULT_END_REASON = "手动结束流程";
        public static final String CONTENT_TYPE_PNG = "image/png";
        public static final String CONTENT_TYPE_XML = "application/xml";
    }

    public static class WorkflowNodeStatus {
        public static final String ACTIVE = "active";
        public static final String COMPLETED = "completed";
        public static final String PENDING = "pending";
    }

    public static class WorkflowResponseKey {
        public static final String ID = "id";
        public static final String KEY = "key";
        public static final String NAME = "name";
        public static final String ASSIGNEE = "assignee";
        public static final String ASSIGNEE_ID = "assigneeId";
        public static final String ASSIGNEE_NAME = "assigneeName";
        public static final String VERSION = "version";
        public static final String CATEGORY = "category";
        public static final String DEPLOYMENT_ID = "deploymentId";
        public static final String PROCESS_DEFINITION_ID = "processDefinitionId";
        public static final String RESOURCE_NAME = "resourceName";
        public static final String DIAGRAM_RESOURCE_NAME = "diagramResourceName";
        public static final String TENANT_ID = "tenantId";
        public static final String DEPLOYMENT_TIME = "deploymentTime";
        public static final String BUSINESS_KEY = "businessKey";
        public static final String START_TIME = "startTime";
        public static final String END_TIME = "endTime";
        public static final String ROLE_CODES = "roleCodes";
        public static final String USER_ID = "userId";
        public static final String USER_NAME = "userName";
        public static final String TIME = "time";
        public static final String MESSAGE = "message";
        public static final String COMMENTS = "comments";
        public static final String CONTENT_TYPE = "contentType";
        public static final String DATA = "data";
        public static final String TYPE = "type";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String STATUS = "status";
    }


    public static class chat {
        /** 大模型 */
        public static final Integer LLMCHAT = 1;

        /** rag */
        public static final Integer RAGCHAT = 2;

    }

    // ==================== 私有构造函数 ====================
    
    private SystemConstants() {
        // 工具类，禁止实例化
    }
}
