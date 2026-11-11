# Skill 压缩包与 GitHub 导入

技能页面增加“导入技能”。先解析预览、修改名称和编码、选择启用状态及需要的技能，再确认入库。预览和下载不修改数据库，不执行脚本，不自动绑定工具或智能体。已发布智能体需重新发布才能使用更新后的资源。

## 支持格式

- ZIP、tar.gz、tgz：支持根目录 SKILL.md、单个包装目录和多个技能目录，tar 包常见的 `./` 根目录也支持。
- 每个技能以 SKILL.md 为入口，解析 YAML frontmatter 的 name 和 description，正文原样保留。无元数据时按目录生成名称并显示提示；不符合编码规则的名称生成可编辑的有效编码。
- 脚本、参考文档及其他 UTF-8 文本存入现有 resources。子技能单独导入，不会被重复当作父技能资源。二进制附件、无效 UTF-8、链接及危险路径会明确拒绝，不静默丢弃技能内的文件。
- GitHub 支持公开仓库、tree 目录链接，以及显式分支、Tag、提交和相对目录。分支含斜线时分别填写完整分支及目录。留空分支使用仓库默认分支；不读取服务器 Git 凭据，不支持私有仓库 Token。

GitHub 下载使用[官方仓库 ZIP 归档接口](https://docs.github.com/en/rest/repos/contents#download-a-repository-archive-zip)，手动跟随最多三次重定向，仅允许 HTTPS api.github.com / codeload.github.com。下载响应在接收时限制体积，每个请求最长60秒。

## 限制与事务

压缩包最大20MiB，解压内容累计最大50MiB，最多2000个归档条目、50个技能，单个归档文件最大5MiB。已有 Skill 约束继续有效：正文最多100000字符、每个技能最多100个资源、单个资源最多1000000字符、资源累计最多5000000字符。包中非技能目录不会作为独立技能导入。

确认接口再次执行完整请求校验，不信任前端预览数据；同批编码重复或已有有效资源编码冲突时整批回滚，不覆盖已有 Skill。前端保留预览供修改或取消选择后重试。

## API 与升级

- POST /v1/agent-console/skills/import/archive：multipart/form-data，文件字段 file；返回预览。
- POST /v1/agent-console/skills/import/github：JSON `{ "url": "https://github.com/owner/repo", "ref": "main", "path": "skills" }`；返回预览。
- POST /v1/agent-console/skills/import：JSON `{ "skills": [AgentSkillDTO] }`；原子新增选中技能并返回结果。

三个入口沿用超级管理员身份限制和动态权限，权限码分别为 ai:skill:import:archive:api、ai:skill:import:github:api、ai:skill:import:commit:api。新增 V008 权限迁移，不修改历史迁移文件；启动时执行，仅为有效超级管理员关联。升级后刷新用户权限或重新登录。

新增自动化回归使用内存压缩包、H2、MockMvc 和模拟 GitHub HTTP 响应，不读取真实私有仓库、不创建云沙箱或付费模型请求。
