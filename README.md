# ai-platform

跨产品、跨租户的共享基础平台。源码边界包括统一网关、认证、系统、文件中心、SSE、通用 Agent、调度控制面、后端契约、数据库结构基线和容器编排；产品业务模块、真实租户初始化数据和前端保留在各自受控仓库。

本仓库使用 [Apache License 2.0](LICENSE)。许可证覆盖当前公开源码，不包含真实租户数据、
部署秘密、企业标识、商标或第三方服务内容。

## 模块

- `platform-gateway`：唯一外部 API 入口和租户路由守卫。
- `platform-auth`：共享登录、Token 和登录保护。
- `platform-modules/platform-system`：共享组织、权限、字典、区域、导入模板、消息、天气和当前用户设置。
- `platform-modules/platform-files`：个人/部门空间、文件版本、共享授权、回收站、编辑锁、审计和对象存储。
- `platform-modules/platform-sse`：共享 SSE 连接和推送。
- `platform-modules/platform-agent`：跨产品、跨租户的通用智能体定义、提供方适配、会话、消息、调用审计和 ASR 入口，独立部署且不依赖产品模块。
- `platform-api/platform-agent-api`：产品服务按稳定 Agent 编码调用平台的内部契约，不包含工单、资讯等产品类型。
- `platform-modules/platform-scheduler`：租户隔离的调度控制面，治理任务定义、同步、手工触发和实例查询，不承载产品业务处理器。
- `platform-api/platform-core-api`：后台任务租户作用域等跨服务基础契约。
- `platform-api/platform-system-api`：供产品服务调用的系统平台契约。
- `platform-api/platform-files-api`：供产品服务上传和下载业务附件的文件契约。
- `docker/docker-compose.yml` 中的 `powerjob-server`：共享调度引擎；Worker 按领域嵌入具体产品服务。
- `deploy/database/V001__create_platform_schema.sql`：公开的 Flyway 结构基线脚本。全部表位于 `public`，并以模块前缀明确所有权：系统 `sys_*`、文件 `file_*`、SSE `sse_*`、AI/Agent/Skill/Knowledge `ai_*`、调度 `scheduler_*`；租户数据统一以 `tenant_id` 隔离。

本仓库不包含产品专属的 Agent 实现或共享前端；产品通过网关中由部署环境配置的入口访问 `platform-agent`，语音转写使用其中唯一的 `/agent/speech/transcribe` 契约。

Agent 的通用边界、产品适配方式和数据规则见 [通用 Agent 架构](docs/architecture/agent.md)。任何产品资讯、工作流或领域数据都不得进入本仓 Agent 模块。

后端统一使用 Java 25、Spring Boot 4 和 Jackson 3 执行应用级 JSON 序列化。Jackson core/databind API 使用 `tools.jackson.*`；配套 annotations 模块仍使用 `com.fasterxml.jackson.annotation.*` 包。Nacos Client、Swagger/PowerJob 和 Hibernate 等三方版本会传递携带 Jackson 2 运行库，本仓库应用源码不使用其 core/databind API、不显式声明直接依赖，也不建立第二套映射器。

公共 Java 能力使用 GitHub Packages 中已发布的 `io.github.guanxiangkai:*` 模块，版本以
`gradle/libs.versions.toml` 为准，默认不读取 Maven Local；仅在联合开发和发布前验证时，才通过显式
`AI_PLUS_HOME` 启用 `ai-plus` composite build。已发布版本不可覆盖，稳定性验证期间不创建新版本。

认证令牌采用 RS256，并按 `typ`、`token_use`、`aud`、`iss`、`key-id` 和时间声明执行互斥验证；
访问令牌只能用于 Gateway，刷新令牌只能用于 Auth 刷新接口。部署配置及轮换要求见
[运行时边界与部署规范](docs/architecture/runtime-boundaries.zh-CN.md#jwt-验证概要)。

CI 从公开的 `ai-plus` 仓库检出同名任务分支（不存在时回退 `dev`），再通过显式
`AI_PLUS_HOME` 执行源码联合构建，不需要跨仓库 PAT；生产和普通构建消费 GitHub Packages
已发布构件。包仓库需要认证时只使用 GitHub 受管令牌，凭据禁止写入仓库文件或构建日志。

内部 Agent 调用使用 Spring Framework 7 HTTP Service Registry 注册，并由 Spring Cloud
LoadBalancer 按 `platform-agent` 服务名发现实例；System 与 Files 客户端继续保留各自的
逐操作错误映射和流式/Multipart 语义。三类客户端统一复用 Web Plus 的租户透传过滤器，
不会通过全局 `BeanPostProcessor` 隐式修改应用中的其他 `WebClient`。

## Nacos 与部署

同一环境的全部服务使用由部署环境注入的 Nacos namespace、发现分组和配置分组。namespace 只区分运行环境，配置 Group 只隔离配置，二者都不用于拆分跨服务调用。服务通过 `application.yml` 的 `spring.config.import` 读取 Nacos，不使用 `bootstrap.yml`。

`deploy/nacos/` 保存不含连接信息的 Data ID 配置模板。数据库、Redis、Nacos、对象存储、外部 Agent 和 PowerJob 的地址、端口、库名、命名空间、账号及凭据都必须由部署环境或 Secret 注入。根目录 `docker/docker-compose.yml` 是平台七个 Java 服务和 PowerJob Server 的可移植编排模板；Java 服务共用一个 JDK Dockerfile，仅 `platform-gateway` 发布可配置端口，其余服务只在容器网络中互访。PowerJob 凭据通过 Spring Boot 环境变量绑定，不进入 Java 进程命令行；其 5.1.2 Server 的未修复 SSRF 风险及部署门禁见 [SECURITY.md](SECURITY.md)。

数据库结构统一通过 `deploy/database/apply-schema.sh` 调用部署环境提供的固定版本 Flyway 容器执行结构基线，且 `cleanDisabled=true`。真实租户、组织、用户、菜单、区域和产品初始化数据必须由受控部署流程提供，不得提交到本仓库；产品数据库不得保存通用表或 Agent 定义副本。

## 长期分支

- `dev`：默认开发集成分支
- `test`：阶段测试分支
- `main`：稳定分支

## 架构文档

- [运行时边界与部署规范](docs/architecture/runtime-boundaries.zh-CN.md)
