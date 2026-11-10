# 七项基础配置与登录安全同步

目标为 iceeboot 后端 `20261110-agentscope`，在 `f6f2bf9` 上继续同步面试帝 `34ee683` 的下列七项通用改进。其他差异不在本次范围。

1. Tomcat：使用当前 Spring Boot 的 `server.tomcat.threads.max/min-spare`，支持 `SERVER_TOMCAT_THREADS_MAX`、`SERVER_TOMCAT_THREADS_MIN_SPARE`，默认 200/20。
2. 定时任务：Spring 调度线程池为 4，线程名前缀 `IceeBoot-Scheduler-`，关闭时等待任务，最长 30 秒。保留原支付补偿开关和 Quartz 配置。
3. 微信登录失败跳转：控制器和服务均使用已有 `YunGouOS.frontendUrl`，由 URI 构造器编码错误信息，不再跳向写死的本机地址。
4. 微信登录安全：所有环境均校验 state；每次授权/二维码请求生成独立随机 state，并写入回调地址。使用 Redis GETDEL 一次性消费，拒绝过期或重放。原环境中的 stateSecret 配置可保留，新实现不再依赖时间戳签名。
5. 刷新令牌：从来源的原子替换/撤销逻辑中提取不依赖桌面会话的 Lua，仅操作原有后台缓存键。保持 Jackson 序列化、TTL、旧令牌及消费标记清理行为；不引入产品、桌面端或 Cookie 认证。
6. 登录 ticket：回调使用 `#ticket=...`，不记录包含临时凭证的完整跳转地址。兑换接口新增 JSON 请求体 `{ "ticket": "..." }`，保留原查询参数兼容和 GETDEL 单次消费。配套 iceeboot 前端读取 fragment、兼容旧 query 链接、兑换时用请求体，并清除 URL 中临时凭证。
7. 日志：路径、单文件大小、历史天数可配置，滚动文件压缩；总容量可用 `logging.file.total-size-cap` 配置，默认 512MB。保留 traceId。生产配置使用独立 prod 文件名、MyBatis 不输出 SQL。

没有同步接口文档开关、其他 AI 参数、产品业务、Skill 引擎、AgentScope 或数据库结构；不连接现有 Redis、数据库或真实微信服务。

## 兼容与部署

前后端应配套发布；旧前端仅读取 query，无法处理新回调的 fragment。新前端同时兼容旧回调，适合先发布前端。

需要 Redis 6.2 或以上支持 GETDEL，此要求与之前 ticket 原子消费一致。已登录用户的刷新令牌缓存格式保持不变。刷新令牌替换/撤销各自在 Redis 中原子执行，不改变原后台的访问令牌生命周期策略。

日志由配置统一控制，默认所有环境均输出文件，生产 SQL 日志关闭。现有 `logging.file.name/max-size/max-history` 配置开始实际生效。

## 验证方法

普通回归使用现有模拟服务和 H2，不调用外部平台。

为核实真实 Lua 与项目 Jackson 值序列化兼容，增加独立本机 Redis 回归；显式传入可执行程序后，测试自行选择空闲回环端口、启动进程并在结束时停止，不读取任何业务 Redis 地址：

```shell
mvn -Diceeboot.test.redis-server=/path/to/redis-server clean verify
```

未传入此参数时，仅跳过该组独立 Redis 测试。回归覆盖令牌替换/撤销的并发一致性、一次性 state 与 ticket、全部环境的 state 校验、前端回调地址、fragment 传递、兑换接口兼容，以及 Spring Boot 线程参数绑定。日志另通过独立进程验证配置路径和 traceId 实际输出。

本次在指定后端仓库完成干净构建：181 项测试中 137 项通过、44 项真实厂商测试跳过，失败及错误均为 0；6 项独立 Redis 测试全部执行通过。日志路径与 traceId 输出的独立进程验证通过。前端类型检查、生产构建及新旧回调行为验证通过。未进行真实微信账号授权联调。
