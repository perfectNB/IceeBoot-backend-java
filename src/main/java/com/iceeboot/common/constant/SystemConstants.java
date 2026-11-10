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
        /** DeepSeek V3.2（DashScope） */
        public static final String DEEPSEEK_MODEL = "deepseek-v3.2";

        /** DeepSeek V4 Pro（官方 DeepSeek API） */
        public static final String DEEPSEEK_V4_PRO = "deepseek-v4-pro";

        /** DeepSeek V4 Flash（DashScope） */
        public static final String DEEPSEEK_V4_FLASH = "deepseek-v4-flash";

        /** 千问 */
        public static final String QWEN_PLUS = "qwen-plus";

        public static final String QWEN_MAX = "qwen-max";

        /** GLM */
        public static final String GLM_47 = "glm-4.7";


    }

    // ==================== 短信厂商 ====================

    /**
     * 短信厂商常量
     */
    public static class SMSProvider {
        /** 阿里云 */
        public static final String ALIYUN = "aliyun";

        /** 腾讯云 */
        public static final String TENCENT = "tencent";

        /** 华为云 */
        public static final String HUAWEI = "huawei";

        /** 网易云信 */
        public static final String NETEASE = "netease";

        /** 容联云 */
        public static final String RONGLIAN = "ronglian";

        /** 七牛云 */
        public static final String QINIU = "qiniu";
    }

    // ==================== 对象存储厂商 ====================

    /**
     * 对象存储厂商常量
     */
    public static class OSSProvider {
        /** 阿里云OSS */
        public static final String ALIYUN = "aliyun";

        /** 腾讯云COS */
        public static final String TENCENT = "tencent";

        /** 华为云OBS */
        public static final String HUAWEI = "huawei";

        /** MinIO */
        public static final String MINIO = "minio";

        /** 七牛云Kodo */
        public static final String QINIU = "qiniu";
    }

    // ==================== 邮件厂商 ====================

    /**
     * 邮件厂商常量
     */
    public static class EmailProvider {
        /** QQ邮箱 */
        public static final String QQ = "qq";

        /** Gmail */
        public static final String GMAIL = "gmail";

        /** 网易邮箱 */
        public static final String NETEASE = "netease";

        /** Outlook */
        public static final String OUTLOOK = "outlook";

        /** 腾讯企业邮 */
        public static final String TENCENT = "tencent";

        /** 阿里云邮件 */
        public static final String ALIYUN = "aliyun";
    }

    // ==================== 邮箱域名 ====================

    /**
     * 邮箱域名常量（用于自动识别邮件厂商）
     */
    public static class EmailDomain {
        public static final String QQ_COM = "qq.com";
        public static final String NETEASE_163 = "163.com";
        public static final String NETEASE_126 = "126.com";
        public static final String YEAH_NET = "yeah.net";
        public static final String GMAIL_COM = "gmail.com";
        public static final String OUTLOOK_COM = "outlook.com";
        public static final String HOTMAIL_COM = "hotmail.com";
        public static final String LIVE_COM = "live.com";
    }

    // ==================== SMTP端口 ====================

    /**
     * SMTP端口常量
     */
    public static class SmtpPort {
        /** 默认SMTP端口 */
        public static final int DEFAULT = 25;

        /** SMTP提交端口（STARTTLS） */
        public static final int SUBMISSION = 587;

        /** SMTP SSL端口 */
        public static final int SSL = 465;
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


    public static class ChatType {
        /** 大模型 */
        public static final Integer LLM_CHAT = 1;

        /** RAG */
        public static final Integer RAG_CHAT = 2;

    }

    // ==================== 私有构造函数 ====================
    
    private SystemConstants() {
        // 工具类，禁止实例化
    }
}
