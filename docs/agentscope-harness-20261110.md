# AgentScope 智能体控制台

## 运行边界

本模块采用与 talkplatform-agent-swarm 相同的主要分层：项目负责配置、版本、权限和执行账本，AgentScope Harness 负责推理与工具执行循环。锁定 AgentScope Java `2.1.0-BETA1`，不覆盖 SDK 内核类，不引入参考项目的企业 AI Gateway、电脑纳管或团队业务模块。

代码已按当前项目规范迁入 `com.iceeboot.framework`，结构、表设计和升级步骤见 [framework 分层重构说明](agentscope-framework-20261111.md)。

- `entity/agent`、`mapper/agent`：独立 DO 与 MyBatis Plus Mapper。
- `dto/create/agent`、`dto/update/agent`、`vo/agent`：创建/更新请求与响应视图。
- `service/agent`、`service/agent/impl`：服务接口、实现与事务协调。
- `service/agent/runtime`、`model/agent`、`enums/agent`：SDK 装配及内部不可变模型。
- `framework/controller/agent`：接口入口；`config/agent`：环境配置绑定。
- `common/constant/ResponseMessage.Agent`：模块消息常量；`common/exception/agent`：接口异常处理。

不再保留 Coze、Dify、Spring AI Alibaba、百炼 Agent/模型/知识库、Ollama 与演示 RAG 控制器。原供应商 HTTP/SSE 策略由 AgentScope 模型组件替代。Activiti 审批、OSS、SMS、Email、登录与支付基础模块保留。

## 数据库与权限

新环境先执行 `src/main/resources/sql/agentscope_schema.sql` 创建规范化表，再执行 `docs/sql/20261111_agent_console_permissions.sql` 补齐菜单和动态权限。已部署旧版本的环境按 [重构升级步骤](agentscope-framework-20261111.md) 执行 `docs/sql/20261111_agentscope_framework.sql` 和菜单/权限脚本。

配置资源迁入 `t_agent_*` 表，Skill、Tool、MCP、知识库和沙箱模板独立存储。资源注册表分配统一 ID，旧绑定 ID 保持不变。发布版本继续保存包含完整资源的不可变 JSON 快照。

新控制台拥有九个菜单项、八个功能页面，新增接口的权限仅分配给角色编码 `superAdmin`。47 个接口统一使用 `@RequirePermission(dynamic = true)`，从数据库按 HTTP 方法与路径模板获取 `ai:*` 权限码，同时要求登录和超级管理员身份。没有有效映射时不放行，GET 权限不能授权同一路径的 DELETE。普通管理员不能通过单独添加权限码绕过角色限制。

角色权限缓存由现有启动初始化器刷新。已登录的前端需要刷新用户信息或重新登录，以获取新菜单。

## 部署配置

运行配置分别位于 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。选择对应 Profile，并在环境变量中设置：

- `AI_CONFIG_ENCRYPTION_KEY`：Base64 编码的随机 32 字节主密钥。可使用 `openssl rand -base64 32` 生成，交给部署环境或密钥管理系统保存；不可提交 Git。更换该密钥需要迁移现有密文，不能直接覆盖。
- `ALIYUN_E2B_API_KEY`、`ALIYUN_E2B_API_BASE_URL`、`ALIYUN_E2B_DOMAIN`：阿里云 E2B 平台连接信息，在 application 的 `iceeboot.agent.e2b` 中绑定。
- `AGENT_PROJECT_API_BASE_URL`：本项目内部 API 根地址，默认 `http://127.0.0.1:9000/iceeboot/api`。配置“项目接口工具”时只填写 `/v1/...` 相对路径。
- 可选 `AGENT_WORKSPACE`、`AGENT_EXECUTION_TIMEOUT_SECONDS`、`AGENT_MAX_CONCURRENT_RUNS`。

前端 `VITE_APP_BASE_API` 配置项目根地址，例如 `http://127.0.0.1:9000/iceeboot`，现有请求方法会附加 `/api/v1/...`。

模型连接在后台保存供应商、协议、Base URL、API Key、模型和默认参数。支持 GPT、Claude、Gemini、Qwen、DeepSeek、GLM、Kimi、MiniMax。Qwen 通过自行配置的 OpenAI 兼容接口接入，不包含百炼模型客户端。供应商和具体型号的思考参数、原生结构化输出能力有差异；以所选服务实际支持的能力为准。

## 使用顺序

1. 配置模型连接。API Key、工具/MCP 请求头、沙箱环境变量加密保存，读接口仅返回 `********`。编辑时保留占位符表示不修改。
2. 配置 Skill、Tool、MCP、知识库连接与 E2B 模板。MCP 当前使用远程 Streamable HTTP，必须明确填写远端工具白名单，不接受 `*`，工具名增加服务编码前缀。
3. 创建 Agent。code 必须唯一且创建后不可修改，配置模型、提示词、输出格式、能力绑定、可选子 Agent 和环境变量。
4. 保存草稿并发布。发布时复制模型参数、能力内容、密文和子 Agent 已发布版本，校验循环和最多五层的委派深度。修改配置后重新发布才生效。
5. 通过 code 调用 Agent。运行只装配快照中的能力；Skill 说明按需加载，已绑定 Skill 的关联工具与显式绑定工具共同组成可调用白名单。当前不额外使用动态工具分组。子 Agent 每次委派拥有独立 Harness、身份和账本。

知识库本期通过已有知识库的 HTTP 检索接口接入，注册为只对绑定 Agent 开放的检索工具；不包含文档上传、切分和向量索引建设，也不依赖百炼知识库。

脚本 Tool 的输入参数以 JSON 从标准输入传入命令。Skill 中脚本的实际路径以加载结果为准。Python、Shell、curl 等命令只能在 E2B 执行；未启用沙箱时不注册文件和 Shell 执行工具，不允许回退到应用服务器执行。HTTP Tool 使用 Java HTTP 客户端，无需 Shell。项目接口工具从登录请求捕获身份，并在每次调用时重新校验配置的接口权限，令牌不作为模型参数暴露。

环境变量只注入对应 Agent 的 E2B，不放进提示词、不自动向子 Agent 继承。每个 Agent 可选择不同模板。

## 调用接口

接口前缀：`/iceeboot/api/v1/agent-console`。

- `GET/POST/PUT /models`、`DELETE /models/{id}`：模型连接管理。
- `POST /models/invoke`：业务直接调用模型，不进入 Agent 循环；请求包含 `modelConfigId`、`input` 和可选 `systemPrompt`。
- `/skills`、`/tools`、`/mcps`、`/knowledge`、`/sandbox-templates`：前端使用的独立资源接口，每组提供 GET 列表、GET `/{id}` 详情、POST 新增、PUT 更新和 DELETE `/{id}` 删除。
- `GET/POST/PUT /capabilities`、`DELETE /capabilities/{id}`：保留的兼容资源接口。
- `GET/POST/PUT /agents`、`GET/DELETE /agents/{id}`：Agent 草稿管理。
- `POST /agents/{id}/publish`、`GET /agents/{id}/releases`：发布与历史版本。
- `POST /agents/by-code/{code}/invoke`：调用已发布 Agent，请求示例：

```json
{"input":"分析销售数据并生成报告","sessionId":"conversation_001"}
```

- `GET /runs`、`GET /runs/{id}`：最近执行与父子调用详情。

运行接口需要 Bearer 登录令牌和 `ai:agent:invoke` 权限。当前控制台接口只允许超级管理员；项目内部业务可注入 `AgentExecutionService`，在业务自己的权限校验后构造可信 Caller 调用，不能使用请求体中的用户 ID 代替登录身份。

## 执行限制与记录

默认最多八个根任务同时执行；同一进程内同一用户/发布版本/会话不能并发执行。每个根任务与所有子任务共享最多一百次模型请求预算和根任务超时，每个 Agent 另有最大执行轮次。当前同步请求执行，不提供持久任务队列或跨进程会话排队；多副本部署应在入口按会话路由，或增加分布式执行协调。

记录每个 Agent 的输入、输出、状态、版本及自身 Token，并保存每轮模型请求的输入输出和用量。流内 usage 使用最后有效快照，每次模型请求只入账一次；父子合计从实际请求记录求和。供应商未返回用量时标记未知，不伪装为准确的零消耗。

## 验证

测试覆盖模型客户端创建、自定义 Base URL、工具协议、AES 密文和脱敏、code 不可变、发布快照、循环关联、Skill 加载和关联工具白名单、MCP 白名单、项目接口身份、无沙箱拒绝脚本、父子调用账本、流式用量去重、会话并发和接口/菜单权限。

真实供应商密钥和阿里云 E2B 凭据需由部署环境提供。自动化测试使用隔离数据库、本地 HTTP 服务和确定的模型响应，不会调用付费模型或创建云上沙箱。上线前用实际供应商和 E2B 模板补充端到端验证。

### 本次验证记录（2026-11-10）

- 后端完整测试 113 项：107 项通过、6 项环境相关测试跳过，无失败；后端打包通过。
- 前端类型检查与生产构建通过；隔离页面数据下检查配置弹窗、资源编辑、JSON 错误拦截与响应式布局。
- 指定 MySQL 已执行增量迁移：六张表、九个菜单、十六条 API 权限，仅关联超级管理员。旧 AI 菜单停用，历史 AI 表保留。
- 本地应用连接指定 MySQL 与独立测试 Redis，验证匿名 401、普通用户 403、超级管理员 200，新菜单携带权限字段。
- MySQL 自动回滚事务验证配置加密保存、发布版本和 Token 账本；没有保留该验证的测试数据。
- 未使用真实模型供应商或云上 E2B 进行付费联调。

### 接口与权限对齐记录（2026-11-11）

- 前端五类资源接入各自的 CRUD 接口，模型和智能体编辑使用 PUT；资源菜单切换时重新加载，丢弃过期请求结果。
- 后端 151 项测试：145 项通过、6 项环境相关测试跳过；前端八项接口测试、类型检查和生产构建通过。
- 用户指定 MySQL `zboot` 已创建 11 张规范化表，九个菜单、44 条方法与路径权限及超级管理员关联均已核对。原六张表为空并保留，其他角色授权未改动，执行前保存本模块 RBAC 备份。
- 部署新代码后重启后端并重新登录前端，使运行进程和菜单缓存使用本次更新。真实厂商模型和云沙箱未联调。

### 后续维护能力

新版本提供数据库原子调用额度、密文主密钥轮换、安全失败诊断、执行取消和启动版本迁移。配置、接口权限和升级步骤见 [维护说明](agent-maintenance-20261111.md)。
