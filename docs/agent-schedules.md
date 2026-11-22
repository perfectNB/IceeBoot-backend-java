# 智能体定时任务

入口：智能体列表右上角「定时任务」。支持单次、每天、每周和高级 Quartz Cron，支持暂停、编辑、删除和按页查看触发记录/运行结果。

## 分层

- `framework/controller/agent/AgentScheduleController`：DTO/VO 和动态接口鉴权。
- `framework/service/agent/impl/AgentScheduleServiceImpl`：配置、所有者隔离和历史查询。
- `AgentScheduleDispatchServiceImpl`：短事务中的跨表认领、终态和恢复；数据库行锁及 `(schedule_id,scheduled_at)` 唯一索引防止多实例重复领取。
- `framework/service/agent/schedule`：时间策略、实时授权、扫描协调和既有执行服务适配，互相通过职责明确的服务协作。
- `framework/job/AgentScheduleDispatchJob` 与 `config/agent/AgentScheduleConfig`：复用 Quartz 唤醒扫描，独立有界线程池运行模型，不改动原有系统定时任务。
- `framework/entity|mapper|dto|vo|enums/agent`：分别定义数据库对象、Mapper、请求和响应，Controller 不返回 DO。

## 执行语义

1. 所有触发时间点以 UTC 毫秒保存；周期任务使用 IANA 时区计算下一次时间，Cron 秒位必须为 0。扫描间隔 10 秒，实际启动可能略晚于计划时间，服务负载也会影响延迟。
2. 单次任务错过后执行一次。周期触发晚于计划超过 60 秒时记录 `SKIPPED_MISFIRE`，直接推进至未来，不集中补跑停机期间的任务。
3. 同一任务上一轮未结束则记录 `SKIPPED_OVERLAP`。独立执行线程池无排队空间，满载时保留未认领任务，后续扫描再次尝试；单次任务不会因容量满而丢失，周期任务仍受 60 秒过期规则约束。认领仅在线程开始执行后发生。失败/跳过均不自动重试；周期任务未来正常触发。
4. 每次生成独立会话，使用执行时最新发布版本；复用执行服务的每日额度、单轮模型请求上限、全局并发上限、超时、过程记录和取消标记。历史记录关联 `runId`，失败发生在创建运行前时可以没有 `runId`。
5. 配置暂停只影响后续触发，不中断当前轮次。运行中禁止编辑/删除。已过时的单次任务需编辑为未来时间后才能再次启用。
6. 进程中断后，运行租约在「单轮超时 + 180 秒」后过期；下一次扫描将其标为 `INTERRUPTED`，不重试该次触发。迟到的旧执行结果不能覆盖新轮次状态。外部工具的副作用不能靠数据库认领提供端到端 exactly-once，工具应自行支持业务幂等。
7. 不存储用户登录令牌。任务归属取自登录上下文，操作只允许有效超级管理员查看自己的任务。创建、启用和每次执行均重新检查有效账号/角色、调用接口权限和 `ai:schedule:execute` 授权。依赖用户登录令牌的项目 API 工具（含子智能体）暂不允许用于定时任务。

## 部署与验证

- 后端启动自动应用 `V014__agent_schedules.sql` 和 `V015__agent_schedule_permissions.sql`。前者建两张表，后者加入六个接口权限及后台执行权限，只授权有效超级管理员。使用现有智能体菜单，不另建重复菜单；前端各按钮按对应接口权限显示。
- 配置 `iceeboot.agent.schedule-concurrency`（默认 2，范围 1–32）控制单节点定时执行线程数，仍共享 `max-concurrent-runs` 上限。现有 dev/test/prod 均可通过 Spring 环境变量 `ICEEBOOT_AGENT_SCHEDULE_CONCURRENCY` 覆盖，无需引入凭据配置。
- 重启后端以应用迁移和注册扫描任务，重新登录以获取新增的前端权限。
- `scripts/verify.sh` 验证包结构、异常约定、真实 SQL/事务/行锁、迁移幂等、权限覆盖、组件启动与执行适配。前端同名脚本验证请求契约、时间预设、交互竞态、类型检查与构建。
- 自动化测试使用本地 H2 和模拟执行器，不访问真实模型或云沙箱，不产生外部调用费用。
