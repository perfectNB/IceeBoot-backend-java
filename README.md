

# Zboot 超级脚手架后端版

<div align="center">
  <p>
    <a href="#"><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg" alt="Spring Boot"></a>
    <a href="#"><img src="https://img.shields.io/badge/MyBatis-3.x-red.svg" alt="MyBatis"></a>
    <a href="#"><img src="https://img.shields.io/badge/Sa--Token-1.x-blue.svg" alt="Sa-Token"></a>
    <a href="#"><img src="https://img.shields.io/badge/MySQL-8.0+-orange.svg" alt="MySQL"></a>
    <a href="#"><img src="https://img.shields.io/badge/Redis-6.0+-red.svg" alt="Redis"></a>
    <a href="#"><img src="https://img.shields.io/badge/JDK-17+-green.svg" alt="JDK"></a>
  </p>
  <p>
    <a href="#"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"></a>
    <a href="#"><img src="https://img.shields.io/badge/version-1.0.0-brightgreen.svg" alt="Version"></a>
    <a href="#"><img src="https://img.shields.io/badge/build-passing-brightgreen.svg" alt="Build"></a>
  </p>
</div>

## 🚀 项目简介

**Zboot 超级脚手架后端版** 是一个面向现代化企业级应用开发的全栈后端解决方案。它不仅仅是一个简单的项目模板，更是一个集成了当前最前沿技术栈的智能化开发平台。Zboot 致力于为开发者提供一个开箱即用、功能完备、易于扩展的后端开发基础设施，让开发者能够专注于业务逻辑的实现，而无需在基础架构搭建上花费大量时间。

在当今快速发展的技术环境中，企业对于软件开发的效率和质量要求越来越高。传统的从零开始搭建项目架构的方式已经无法满足快速迭代的需求。Zboot 应运而生，它整合了业界最佳实践，融合了人工智能技术，为开发团队提供了一个强大而灵活的开发平台。

### 🎯 设计理念

- **开箱即用**：最小化配置，最大化功能，让开发者能够在最短时间内启动项目
- **智能化集成**：深度集成AI大模型和智能体技术，为应用赋予智能化能力
- **企业级标准**：采用企业级开发标准和最佳实践，确保代码质量和系统稳定性
- **高度可扩展**：模块化设计，支持灵活的功能扩展和定制化开发
- **安全优先**：内置完善的安全机制，从接口级别保障系统安全

## 🛠️ 核心技术栈

Zboot 采用了当前最主流和成熟的技术栈，确保项目的稳定性、可维护性和扩展性：

### 后端核心框架
- **Spring Boot 3.x**：作为核心框架，提供自动配置、依赖注入、AOP等企业级功能
- **Spring Security**：与Sa-Token配合，构建多层次的安全防护体系
- **Spring Data JPA**：简化数据访问层开发，提供强大的ORM能力

### 数据持久化
- **MyBatis 3.x**：灵活的SQL映射框架，支持复杂查询和高性能数据访问
- **MyBatis-Plus**：MyBatis的增强工具，提供代码生成、分页、性能分析等功能
- **MySQL 8.0+**：主流关系型数据库，支持JSON、全文索引等高级特性
- **Redis 6.0+**：高性能缓存和会话存储，支持多种数据结构

### 权限认证与安全
- **Sa-Token**：轻量级权限认证框架，支持多种认证模式和细粒度权限控制
- **JWT**：无状态的身份验证机制，支持分布式系统
- **BCrypt**：密码加密算法，确保用户密码安全

### 开发工具与中间件
- **Swagger/Knife4j**：API文档自动生成和在线调试
- **Logback**：高性能日志框架，支持异步日志和多种输出格式
- **Jackson**：JSON序列化和反序列化
- **Validation**：参数校验框架，支持自定义校验规则

## 🔐 权限控制系统

Zboot 采用 **Sa-Token** 作为权限认证框架，实现了业界领先的细粒度权限控制体系：

### 权限控制特性
- **接口级权限控制**：每个API接口都可以独立配置访问权限，确保系统安全
- **角色权限管理**：支持多角色、多权限的复杂权限模型
- **动态权限分配**：支持运行时动态调整用户权限，无需重启系统
- **权限缓存机制**：基于Redis的权限缓存，提升权限验证性能
- **会话管理**：支持单点登录、踢人下线、会话共享等功能

### 安全防护机制
- **防重放攻击**：基于时间戳和随机数的防重放机制
- **接口限流**：支持基于IP、用户、接口的多维度限流
- **SQL注入防护**：MyBatis预编译语句，有效防止SQL注入
- **XSS防护**：输入输出过滤，防止跨站脚本攻击

## 🤖 AI大模型集成

Zboot 紧跟人工智能发展趋势，深度集成了国内外主流AI大模型，为应用提供强大的智能化能力：

### 国内AI大模型
- **DeepSeek**：深度求索科技的大语言模型，在代码生成和逻辑推理方面表现优异
- **文心一言**：百度推出的知识增强大语言模型，具备强大的中文理解和生成能力
- **通义千问**：阿里巴巴达摩院开发的大语言模型，在多模态理解方面具有优势
- **Kimi**：月之暗面公司的长文本处理大模型，支持超长上下文理解

### 国外AI大模型
- **ChatGPT**：OpenAI的GPT系列模型，在对话和文本生成方面表现卓越
- **Grok**：xAI公司开发的大语言模型，具有独特的幽默感和创造力
- **Gemini**：Google的多模态大模型，支持文本、图像、音频等多种输入
- **Claude**：Anthropic公司的AI助手，在安全性和有用性方面表现突出

### AI集成特性
- **统一API接口**：提供统一的调用接口，支持快速切换不同的AI模型
- **智能路由**：根据任务类型自动选择最适合的AI模型
- **结果缓存**：对AI模型的响应结果进行缓存，提升响应速度和降低成本
- **错误重试**：智能的错误处理和重试机制，确保服务稳定性

## 🤖 Agent智能体支持

Zboot 引入了先进的Agent智能体技术，支持构建和集成智能化的业务流程：

### 支持的Agent平台
- **Coze**：字节跳动推出的AI Bot开发平台，支持快速构建智能对话机器人
- **Dify**：开源的LLM应用开发平台，提供可视化的AI应用构建能力

### Agent功能特性
- **工作流编排**：支持复杂业务流程的自动化编排和执行
- **多模态交互**：支持文本、语音、图像等多种交互方式
- **知识库集成**：可以接入企业知识库，提供专业领域的智能问答
- **API调用能力**：Agent可以调用系统API，实现业务操作的自动化

## 🔧 第三方服务集成

Zboot 内置了多种常用的第三方服务，开箱即用，大大提升开发效率：

### 短信服务 (SMS)
- **多厂商支持**：集成阿里云、腾讯云、华为云等主流短信服务商
- **模板管理**：支持短信模板的统一管理和动态配置
- **发送记录**：完整的短信发送记录和状态跟踪
- **限流控制**：防止短信轰炸，支持基于手机号的发送频率限制

### 对象存储服务 (OSS)
- **多云支持**：支持阿里云OSS、腾讯云COS、华为云OBS等
- **文件管理**：提供文件上传、下载、删除、预览等完整功能
- **图片处理**：支持图片压缩、裁剪、水印等处理功能
- **CDN加速**：集成CDN服务，提升文件访问速度

### 邮件服务 (Email)
- **SMTP支持**：支持各种SMTP邮件服务器
- **模板邮件**：支持HTML邮件模板和动态内容填充
- **批量发送**：支持邮件的批量发送和队列处理
- **发送统计**：提供邮件发送成功率和打开率统计

### 接口文档
- **Swagger集成**：自动生成API文档，支持在线调试
- **版本管理**：支持API版本管理和向后兼容
- **权限标注**：文档中自动标注接口所需权限
- **示例代码**：自动生成多种语言的调用示例

### 日志系统
- **自动捕获**：自动捕获系统运行日志、错误日志、业务日志
- **结构化日志**：支持JSON格式的结构化日志输出
- **日志分析**：集成ELK栈，支持日志的搜索和分析
- **告警机制**：支持基于日志的实时告警和通知

## ⚡ 轻量化设计

Zboot 秉承"简单即美"的设计理念，提供轻量化的开发体验：

### 极简配置
- **零配置启动**：除了数据库连接，几乎无需其他配置即可启动项目
- **智能默认值**：所有配置项都有合理的默认值，支持渐进式配置
- **环境隔离**：支持开发、测试、生产环境的配置隔离
- **配置热更新**：支持部分配置的热更新，无需重启服务

### 依赖管理
- **最小依赖**：只引入必要的依赖，避免jar包冲突
- **版本锁定**：锁定依赖版本，确保构建的一致性
- **依赖分析**：提供依赖分析工具，帮助优化项目结构

### 快速启动
1. 配置MySQL数据库连接
2. 配置Redis连接信息
3. 运行主类，系统自动完成初始化
4. 访问Swagger文档，开始API开发

## 🔨 智能代码生成器

Zboot 内置了强大的代码生成器，支持基于数据库表结构的代码自动生成：

### 生成能力
- **完整的CRUD代码**：自动生成Controller、Service、ServiceImpl、Mapper、Entity等
- **DTO和VO类**：自动生成数据传输对象和视图对象
- **Swagger注解**：自动添加API文档注解
- **权限注解**：根据配置自动添加权限控制注解

### 模板定制
- **可定制模板**：支持自定义代码生成模板
- **命名规范**：支持多种命名规范的配置
- **包结构**：支持自定义包结构和文件组织方式
- **代码风格**：支持不同的代码风格和格式化规则

### 智能特性
- **字段类型映射**：智能映射数据库字段类型到Java类型
- **关联关系处理**：自动处理表之间的关联关系
- **索引优化建议**：根据查询模式提供索引优化建议

## 🗄️ 数据库自动管理

Zboot 提供了智能的数据库管理功能，大大简化了数据库维护工作：

### 自动建表
- **启动检测**：项目启动时自动检测数据库结构
- **表结构同步**：根据Entity类自动创建或更新表结构
- **字段变更**：支持字段的新增、修改、删除等操作
- **索引管理**：自动创建和维护数据库索引

### 数据迁移
- **版本控制**：支持数据库版本控制和迁移脚本管理
- **回滚机制**：支持数据库结构的回滚操作
- **数据备份**：自动备份重要数据，防止数据丢失

### 性能优化
- **慢查询监控**：自动监控和记录慢查询
- **连接池优化**：智能的数据库连接池配置
- **缓存策略**：多级缓存策略，提升数据访问性能

## 🚧 项目现状与发展规划

### 当前状态
Zboot 项目目前处于快速发展阶段，虽然开发时间不到一个月，但已经具备了完整的基础功能和核心特性。项目采用敏捷开发模式，持续迭代和完善功能。

### 已完成功能
- ✅ 基础架构搭建
- ✅ 权限认证系统
- ✅ AI大模型集成
- ✅ 第三方服务集成
- ✅ 代码生成器
- ✅ 数据库自动管理
- ✅ 接口文档生成
- ✅ 日志系统

### 未来规划

#### 短期目标（1-3个月）
- **MCP协议集成**：引入Model Context Protocol，提升AI模型的上下文理解能力
- **插拔式AI接口**：实现AI接口的插拔式架构，支持快速集成新的AI模型
- **性能监控**：集成APM工具，提供系统性能监控和分析
- **单元测试**：完善单元测试覆盖率，确保代码质量

#### 中期目标（3-6个月）
- **微服务支持**：支持微服务架构，提供服务注册发现、配置中心等功能
- **消息队列集成**：集成RabbitMQ、Kafka等消息中间件
- **分布式事务**：支持分布式事务处理
- **多租户支持**：提供SaaS化的多租户解决方案

#### 长期目标（6个月以上）
- **前端脚手架**：开发配套的前端脚手架，实现全栈开发
- **DevOps集成**：集成CI/CD流水线，支持自动化部署
- **云原生支持**：支持Docker、Kubernetes等云原生技术
- **国际化**：支持多语言和国际化功能

## 📁 项目结构

```
zboot-backend-java/
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/
│   │   │   └── 📁 com/zboot/
│   │   │       ├── 📁 common/                    # 通用模块
│   │   │       │   ├── 📁 config/               # 配置类
│   │   │       │   ├── 📁 constant/             # 常量定义
│   │   │       │   ├── 📁 exception/            # 异常处理
│   │   │       │   ├── 📁 service/              # 通用服务
│   │   │       │   │   ├── 📁 ai/               # AI服务集成
│   │   │       │   │   ├── 📁 email/            # 邮件服务
│   │   │       │   │   ├── 📁 sms/              # 短信服务
│   │   │       │   │   └── 📁 oss/              # 对象存储服务
│   │   │       │   └── 📁 util/                 # 工具类
│   │   │       ├── 📁 generator/                # 代码生成器
│   │   │       │   ├── 📄 CodeGenerator.java    # 核心生成器
│   │   │       │   └── 📁 templates/            # 代码模板
│   │   │       ├── 📁 project/                  # 业务模块
│   │   │       │   └── 📁 zboot_test/           # 示例业务模块
│   │   │       │       ├── 📁 controller/       # 控制器层
│   │   │       │       ├── 📁 service/          # 服务层
│   │   │       │       │   └── 📁 impl/         # 服务实现
│   │   │       │       ├── 📁 mapper/           # 数据访问层
│   │   │       │       ├── 📁 entity/           # 实体类
│   │   │       │       ├── 📁 dto/              # 数据传输对象
│   │   │       │       │   ├── 📁 create/       # 创建DTO
│   │   │       │       │   └── 📁 update/       # 更新DTO
│   │   │       │       ├── 📁 vo/               # 视图对象
│   │   │       │       └── 📁 query/            # 查询对象
│   │   │       └── 📄 ZbootApplication.java     # 启动类
│   │   └── 📁 resources/
│   │       ├── 📄 application.yml               # 主配置文件
│   │       ├── 📄 application-dev.yml           # 开发环境配置
│   │       ├── 📄 application-prod.yml          # 生产环境配置
│   │       ├── 📁 mapper/                       # MyBatis映射文件
│   │       ├── 📁 templates/                    # 代码生成模板
│   │       │   ├── 📄 controller.btl
│   │       │   ├── 📄 service.btl
│   │       │   ├── 📄 serviceImpl.btl
│   │       │   ├── 📄 mapper.btl
│   │       │   ├── 📄 entity.btl
│   │       │   ├── 📄 createDto.btl
│   │       │   ├── 📄 updateDto.btl
│   │       │   ├── 📄 vo.btl
│   │       │   ├── 📄 query.btl
│   │       │   └── 📄 mapperXml.btl
│   │       └── 📁 static/                       # 静态资源
│   └── 📁 test/                                 # 测试代码
│       └── 📁 java/
├── 📁 logs/                                     # 日志文件
├── 📁 disabled/                                 # 禁用模块示例
├── 📄 pom.xml                                   # Maven配置文件
├── 📄 README.md                                 # 项目说明文档
├── 📄 api.md                                    # API文档
├── 📄 package.md                                # 打包说明
├── 📄 mock.md                                   # Mock数据说明
└── 📄 .gitignore                                # Git忽略文件
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-username/zboot-backend-java.git
   cd zboot-backend-java
   ```

2. **配置数据库**
   ```yaml
   # application-dev.yml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/zboot?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
       username: your_username
       password: your_password
   ```

3. **配置Redis**
   ```yaml
   # application-dev.yml
   spring:
     redis:
       host: localhost
       port: 6379
       password: your_redis_password
   ```

4. **启动项目**
   ```bash
   mvn spring-boot:run
   ```

5. **访问接口文档**
   打开浏览器访问：http://localhost:8080/doc.html

## 📖 使用指南

### 代码生成
1. 运行 `CodeGenerator.java` 主方法
2. 输入包名和实体名称
3. 系统自动生成完整的CRUD代码

### AI模型调用
```java
@Autowired
private AIService aiService;

public String chatWithAI(String message) {
    return aiService.chat("deepseek", message);
}
```

### 权限控制
```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @SaCheckPermission("user:list")
    @GetMapping("/list")
    public Result getUserList() {
        // 需要user:list权限才能访问
        return Result.success();
    }
}
```

## 🤝 贡献指南

我们欢迎所有形式的贡献，包括但不限于：

- 🐛 Bug报告
- 💡 功能建议
- 📝 文档改进
- 🔧 代码贡献
- 🧪 测试用例

### 贡献流程
1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 📞 联系我们

如果您有任何问题、建议或合作意向，欢迎通过以下方式联系：

- 📧 **邮箱**：857787854@qq.com
- 💬 **QQ群**：待建立
- 🐙 **GitHub Issues**：[提交问题](https://github.com/your-username/zboot-backend-java/issues)

## 🙏 致谢

感谢以下开源项目和技术社区的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis](https://mybatis.org/)
- [Sa-Token](https://sa-token.cc/)
- [Knife4j](https://doc.xiaominfo.com/)
- 以及所有为开源社区做出贡献的开发者们

---

<div align="center">
  <p>⭐ 如果这个项目对您有帮助，请给我们一个Star！</p>
  <p>🚀 让我们一起构建更好的开发体验！</p>
</div>