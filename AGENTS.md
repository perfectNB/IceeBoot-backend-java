# 开发约定

- 业务代码按现有 `framework/controller|service|entity|mapper|dto|vo` 分层；运行配置放 `config`，公共异常和消息放 `common`。禁止恢复 `com.iceeboot.agent` 独立包。
- Controller 接收请求 DTO、返回 `R<VO>`；不得将 DO、SDK 类或运行时 model 直接作为 HTTP 契约。
- Agent 接口使用 `@RequirePermission(dynamic = true)`，同步维护方法、路径、权限码及角色关联 SQL。缺失权限配置必须拒绝请求。
- 主动抛出的 Agent 业务异常使用 `IceeBootException(ResponseMessage.Agent.*)`；第三方错误在边界转换，不返回或记录凭据和第三方异常原始消息。
- 每个存储服务负责一个数据边界；跨资源原子写入必须通过公开事务服务调用，禁止绕过 Spring 事务代理。
- 新增字段与索引必须提供可恢复执行的版本迁移，历史迁移文件发布后不得修改。保留旧版本密文的解密能力，禁止将主密钥写入代码、数据库或 Git。
- 遵循现有 Lombok、Swagger、四空格缩进与 Service 接口命名。修改后执行 `scripts/verify.sh`；使用 `scripts/install-hooks.sh` 安装提交检查。
- 不提交构建产物、工作区、数据库凭据或本机密钥。真实模型和云沙箱联调必须与不产生外部费用的自动化测试区分记录。
