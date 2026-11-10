# AgentScope framework 分层重构

## 代码结构

业务代码位于 `src/main/java/com/iceeboot/framework`，运行配置绑定遵循项目结构放在 `com.iceeboot.config.agent`。原 `com.iceeboot.agent` 包已移除。

```text
framework/
├── controller/agent/             控制台与各类资源 CRUD
├── dto/create/agent/             请求 DTO
├── dto/update/agent/             更新 DTO，要求有效 ID
├── entity/agent/                 11 个数据库 DO
├── enums/agent/                  供应商、协议、资源类型等枚举
├── mapper/agent/                 11 个 BaseMapper
├── model/agent/                  内部不可变模型与发布快照
├── service/agent/                I…Service 接口
│   ├── impl/                    ServiceImpl 与事务协调
│   ├── convert/                 DTO/VO、运行时模型转换
│   ├── runtime/                 AgentScope SDK 模型与能力装配
│   └── security/                配置加密与脱敏
└── vo/agent/                    接口响应 VO
```

接口只接收 DTO、返回 VO。内部 record 供 SDK 装配与快照序列化使用，不再直接作为 HTTP 请求或响应类型。沿用 Lombok、Swagger 注解、四空格缩进、服务接口与 MyBatis Plus 实体服务的项目写法。

## 异常与消息常量

- 运行配置：`com.iceeboot.config.agent.AgentProperties`。
- 消息常量：沿用 `com.iceeboot.common.constant.ResponseMessage`，AgentScope 消息集中在 `ResponseMessage.Agent`，包含业务校验、资源查询、Schema、执行限制、加解密和失败响应。
- 业务异常：使用现有 `com.iceeboot.common.exception.IceeBootException`。例如 `throw new IceeBootException(ResponseMessage.Agent.MODEL_PROVIDER_PROTOCOL_MISMATCH)`。
- 异常处理器：`com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler`，仅作用于智能体控制器包，不改变其他业务模块的全局异常契约。无显式错误码的业务校验维持 HTTP 400；权限错误维持 403；运行环境和 SDK 调用失败使用 500。
- 第三方 SDK/JDK 异常在模型调用、执行和数据转换边界包装为框架异常，原异常保留为 cause。接口不返回未知异常的原始消息、请求头或凭据。

## 持久化

| 资源 | DO / Mapper | 表 |
| --- | --- | --- |
| 模型连接 | AgentModelDO / AgentModelMapper | t_agent_model |
| 统一资源注册 | AgentCapabilityDO / AgentCapabilityMapper | t_agent_capability |
| Skill | AgentSkillDO / AgentSkillMapper | t_agent_skill |
| Tool | AgentToolDO / AgentToolMapper | t_agent_tool |
| MCP | AgentMcpDO / AgentMcpMapper | t_agent_mcp |
| 沙箱模板 | AgentSandboxTemplateDO / AgentSandboxTemplateMapper | t_agent_sandbox_template |
| 知识库连接 | AgentKnowledgeDO / AgentKnowledgeMapper | t_agent_knowledge |
| 智能体定义 | AgentDefinitionDO / AgentDefinitionMapper | t_agent_definition |
| 发布版本 | AgentReleaseDO / AgentReleaseMapper | t_agent_release |
| 执行记录 | AgentRunDO / AgentRunMapper | t_agent_run |
| 模型调用记录 | AgentModelCallDO / AgentModelCallMapper | t_agent_model_call |

数值主键 DO 继承 `IceeBootDO`；发布版本、执行与模型调用保留 UUID 字符串主键，显式声明审计字段和逻辑删除。发布版本与调用记录需在启动前建表，现有实体扫描初始化器只处理 `IceeBootDO` 子类。

注册表管理全局资源 ID 和编码，明细表使用相同 ID，保持 Agent、Skill 与 Tool 的原有绑定。注册记录和资源明细在同一事务中创建/删除。Skill、Tool、MCP、沙箱模板和知识库通过各自 `IService` / `ServiceImpl` 与 Mapper 持久化。

名称、类型、地址、脚本、模板等字段独立存储。JSON 保留给动态生成参数、Schema、请求头、资源内容映射、绑定 ID 列表和不可变发布快照。敏感内容继续加密，VO 使用脱敏占位符；移包不改变快照 JSON 字段名。

## 接口兼容

保留 `/v1/agent-console` 下原有模型、能力、智能体和执行记录接口以及 JSON 字段，原前端可以继续使用 `/capabilities`。

新增独立资源接口：

- `/skills`
- `/tools`
- `/mcps`
- `/sandbox-templates`
- `/knowledge`

每组接口提供 GET 列表、GET `/{id}` 详情、POST 新增、PUT 更新和 DELETE `/{id}` 删除。独立资源 DTO 将本资源配置平铺，例如 Skill 直接包含 `content`、`resources`、`toolIds`。更新 DTO 要求 `id`，不能跨资源类型编辑。

新接口复用 `ai:capability:read/write` 权限与超级管理员校验，无需放宽权限。模型、智能体与兼容能力接口补充 PUT 更新入口；原 POST 保存入口保留。

## 前端与动态权限

前端 `iceeboot-frontend-vue` 已有智能体、模型连接、Skill、Tool、MCP、知识库连接、沙箱模板和执行记录八个页面。五类资源页面共用 `CapabilityManagement.vue`，按路由调用各自的资源接口；请求发送平铺 DTO，响应转换为页面编辑模型。编辑使用 PUT，新增使用 POST。智能体绑定选择器汇总五类资源目录，Skill 页面额外读取工具目录。切换资源菜单时重新加载，并忽略旧请求的迟到结果。

六个 Agent 控制器的 44 个入口统一使用 `@RequirePermission(dynamic = true)`。权限表维护 HTTP 方法、路径模板与权限码，拦截器按请求方法和路径查询；支持 `/{id}`、`/{code}` 和 Ant 通配符，优先使用更具体的路径和明确的方法。GET 与 DELETE 即使路径相同也不会共用授权。缺少映射时返回 403，原有超级管理员身份校验继续生效。

Redis URL 权限缓存改用 `GET /v1/.../{id}` 形式的键；发现旧 URL 缓存时从数据库重新加载。部署新代码后重启后端，前端重新登录以刷新菜单和权限。

## 数据库升级

这些脚本由发布流程执行，应用不会自动复制旧数据。

### 已部署旧 AgentScope 版本

1. 停止旧节点并备份数据库，确保原六张 `ai_*` 表存在。
2. 在 MySQL 8 执行 `docs/sql/20261111_agentscope_framework.sql`。脚本创建目标表，并按原主键复制模型、各类资源、草稿、发布版本、执行与调用记录。原表保留，密文与发布快照原样复制。
3. 执行 `docs/sql/20261111_agent_console_permissions.sql`，补齐九个菜单、44 条方法与路径权限及超级管理员关联。脚本可重复执行，不改变其他角色授权。
4. 对照原表与目标表的数量、ID、资源种类、绑定关系和发布版本，确认无缺失后部署新代码。资源明细数量合计应与注册表数量一致。
5. 使用原 `AI_CONFIG_ENCRYPTION_KEY`，先验证列表与脱敏编辑，再验证发布、既有版本调用和 Token 账本。

脚本重复执行只复制目标表中缺失的 ID，不覆盖已编辑记录。新旧节点不可混跑；迁移后若新系统已有写入，回退旧系统前必须处理这些新增/修改数据，不能直接切回旧表。

### 新环境

先执行 `src/main/resources/sql/agentscope_schema.sql`，再执行 `docs/sql/20261111_agent_console_permissions.sql`。不必创建旧六张 AI 表。

三个环境的运行配置分别位于 `application-dev.yml`、`application-test.yml`、`application-prod.yml`，均保留环境变量注入。不同环境使用各自的数据库、密钥和工作区配置。

## 可靠性修复（2026-11-11）

- 新增用户复制普通资料时排除密码，再设置 BCrypt 哈希。已存在的历史明文密码记录需要按账号重置密码，本次代码变更不自动修改生产用户数据。
- 已声明 `RequirePermission` 的接口遇到缺失、空或空白权限配置时返回 403；配置加载失败不再生成示例权限。启动时清除单个 URL 权限映射缓存，从真实配置重建，已授权的 SSE 异步重派发行为保留。发布前应补齐需要使用的动态接口权限配置。
- 草稿更新不再写入 `active_release_id`，实体将该字段标记为普通更新禁写；发布流程仍通过显式更新切换版本。覆盖首次发布与后续发布同时保存草稿的竞态。
- 资源和智能体采用生成列 `active_code`，仅对 `del_flag=0` 的记录保证编码唯一。逻辑删除后可重复使用编码，历史记录仍保留原编码和发布快照。
- Schema 在保存和发布时执行内置元模式校验与验证器预加载，提前拒绝无效关键字、类型和不存在的内部引用。保留有效内部引用、递归 Schema 与内置草案支持；不同文档的引用上下文独立。

### 已存在规范化表的索引升级

已部署旧版 `t_agent_capability` / `t_agent_definition` 的环境，需要在停止旧节点并备份后执行 `docs/sql/20261111_agentscope_active_code.sql`。该脚本先建立仅约束有效编码的唯一索引，再删除旧 `UNIQUE(code)` 索引；按元数据判断，可重复执行。新环境的建表脚本和旧 `ai_*` 迁移脚本均已包含新索引结构。

仅重新启动应用不会替换旧数据库索引。SQL 文件由发布流程执行；已有规范化表的其他环境仍需单独升级。

## 验证

执行 JDK 21 下的 `mvn clean verify`。测试使用 H2 的 MySQL 模式、真实 MyBatis Mapper 和 Spring 事务代理，覆盖资源分表 CRUD、审计与逻辑删除、事务回滚、DTO/VO 隔离、敏感值脱敏和原有发布/执行行为。HTTP 工具测试需要允许本机端口监听。

构建前执行 clean，避免旧包 class 残留。含非空历史数据的迁移、厂商模型和 E2B 云沙箱需要在部署环境另行验收。

2026-11-11 验证结果：JDK 21 下 `clean verify` 通过，124 项测试中 118 项通过、6 项环境相关测试跳过，无失败或错误。包含真实 Spring 组件扫描、Mapper 注入与事务服务装配检查。可执行 JAR 不含旧 `com/iceeboot/agent` 类，包含新表建表脚本。本次未连接或修改真实数据库。

异常规范统一后再次执行 `clean verify`：129 项测试中 123 项通过、6 项跳过，无失败或错误。新增回归覆盖框架异常与消息常量、SDK 原始 cause 保留、异常响应中的敏感信息保护，并修正 `config.agent` 移包后的组件扫描路径。JAR 已确认包含新的公共异常处理器和消息常量，且无旧配置类、旧控制器异常处理器残留。

五项可靠性修复后执行 `clean verify`：144 项测试中 138 项通过、6 项环境相关测试跳过，无失败或错误。新增回归验证密码哈希与角色分配、缺失权限拒绝和 SSE 重派发、权限缓存故障及旧缓存失效、并发保存与发布、同编码多次删除重建和历史快照保留、Schema 保存/发布前校验。可执行 JAR 包含更新后的有效编码索引定义；真实 MySQL 索引升级脚本尚未执行。

前后端接口与动态权限对齐后执行 `clean verify`：151 项测试中 145 项通过、6 项环境相关测试跳过，无失败或错误。新增覆盖方法隔离、路径参数、匹配优先级、旧缓存重建、查询权限不能删除，以及菜单/权限 SQL 重复执行与 44 个控制器入口逐一对应。前端 `npm run test:agent-console` 八项通过，类型检查、生产构建和本次修改文件的 ESLint 检查通过。

2026-11-11 已在用户指定的 MySQL `zboot` 执行 framework 迁移和菜单/权限脚本，执行前保存本模块 RBAC 备份。原六张 `ai_*` 表均为空；已创建 11 张规范化表（包含有效编码生成列索引），保留原表，确认九个菜单和 44 条接口权限，超级管理员关联分别为九条和 44 条。逐一核对方法、路径、权限码、菜单组件和父子关系；其他角色及无关菜单权限未改动。该结果不代替非空历史数据迁移验收。未重启运行中的应用，未调用真实模型或创建云沙箱。
