# 通用基础代码迁移

## 分支与范围

目标分支：`20261110-agentscope`。

基线：本仓库 `master`，提交 `a1610e5`。

迁移内容复用已验证的通用基础提交 `58b1d89`，在本仓库 master 上重新应用并验证。
来源：`origin/interview-emperor/20261104-mock-interview`，提交 `34ee683`。

本次逐文件提取通用修复，不执行整分支合并，不引入 AgentScope 依赖。

## 已迁移

- LLM 配置名称、自定义 URL、按名称查找及对应 Mapper 映射；聊天和密钥验证使用保存的 URL。地址留空时保留厂商默认行为。
- GPT 的 Responses/Chat Completions 协议处理、流式解析和兼容回退；DeepSeek、Kimi、GLM 的图片/思考参数等通用适配，以及 Qwen 的连接与流式处理改进。
- 可选公网地址解析保护；应用层显式设置 publicEndpointOnly 时启用。后台自有模型仍允许连接自行部署的服务。
- 默认关闭的提示词诊断日志，以及模型适配中的敏感日志处理。
- 认证存储故障返回 503；SSE 断连处理、MVC 异步线程池和经过首轮认证/授权的 SSE 重派发处理。
- 请求头与 Cookie 的统一 Token 解析，不再从 URL 查询参数读取凭据；微信登录 ticket 原子消费。保留 base 原有登录接口契约。原子 ticket 消费要求 Redis 6.2 或以上支持 GETDEL。
- 用户角色缓存使用 roleId，而不是用户角色关联记录的 id；恢复用户详情、角色新增的权限注解。
- OSS 编辑支持留空保留原密钥，避免保存脱敏密钥或改错记录。
- Quartz 任务依赖注入、活动任务恢复、更新与回滚处理。剔除来源中的业务任务、专用时区和归档逻辑。
- LocalDateTime 兼容解析；数据库初始化的显式 decimal 精度及已有主键兼容处理；初始化先于其他启动任务执行。
- Maven 依赖版本管理、可执行 JAR 打包、参数名保留、JUnit 5 测试执行。

## 排除项

不迁移面试/小说业务、邀请奖励、余额计费、客户端与官网认证、业务菜单和任务初始化、业务 SQL、业务资源或环境配置。

不迁移 framework/skill、Skill 控制器、Skill 资源、自研 NativeToolLoop、工具终止控制器或模型工具循环。

前次最小迁移未纳入通知与支付扩展；后续已继续同步通用部分，详见 `docs/foundation-sync-20261110.md`。产品专用支付分发仍排除。Activiti 审批、OSS、SMS、Email 等原有 base 功能保留。现有供应商的增删、Coze/Dify 清理和 AgentScope 接入另行进行。

本仓库 master 已无业务命名占位类，本次没有新增 project 业务代码。master 原有 AI 演示入口保持原状，后续接入 AgentScope 时再统一整理；它们没有被当成生产 Agent/Skill 引擎迁入。

## 数据与缓存

`t_ai_llm_key` 新增 `name`、`url` 两个可空字符串列。新部署沿用实体初始化；已有环境可在发布前按部署流程执行 `docs/sql/20261110_llm_key_configuration.sql`，不要在已有列上重复执行。未自动删除旧字段或业务表。

升级前停止旧节点，按运维流程清理受影响用户的 `user:role:<userId>` 缓存，避免继续读取旧版本写入的关联记录 ID。不要清理整个 Redis。

本次没有连接、迁移或修改真实数据库，没有复制来源分支环境凭据。

## 验证

使用 JDK 21 执行 `mvn clean verify`。普通测试使用模拟模型、本机临时 HTTP 服务、内存 Redis 替身及 H2；真实厂商调用测试默认跳过，只有显式设置 `RUN_LIVE_LLM_TESTS=true` 才启用，使用前需自行提供测试凭据。

受限环境可通过 Maven 的 `-DargLine=-javaagent:<mockito-core.jar>` 显式加载测试代理，避免 Mockito 动态附加受限。打包前必须 clean，防止旧分支 target 目录中的业务 class 混入 JAR。

检查最终包内不存在 com/iceeboot/project、framework/skill、自研 NativeToolLoop 或 AgentScope 依赖。

本次在 JDK 21 下完成干净构建、回归和可执行 JAR 检查：93 项测试通过，44 项真实厂商测试跳过，失败与错误均为 0。最终 JAR 不含业务目录、Skill 引擎、NativeToolLoop、AgentScope 或 H2 运行时依赖。真实数据库启动、云存储、短信、邮件及厂商模型未联调。
