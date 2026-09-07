# AI Platform

AI Platform 是面向通用 AI 应用的多租户基础后端，提供统一网关、认证、组织与权限、文件中心、SSE、Agent 运行时和调度控制面。仓库不包含任何具体业务系统、真实租户、私有网络、数据库数据或配置中心数据。

## 模块

| 模块 | 职责 |
| --- | --- |
| `platform-gateway` | 统一 API 入口、JWT 验证、租户路由守卫和可信身份转发 |
| `platform-auth` | 登录、Token、会话失效与登录保护 |
| `platform-modules/platform-system` | 组织、权限、字典、区域、消息、设置和注册治理 |
| `platform-modules/platform-files` | 文件空间、版本、授权、编辑锁和对象存储适配 |
| `platform-modules/platform-sse` | 多租户 SSE 连接、推送和消息桥接 |
| `platform-modules/platform-agent` | Agent、Skill、Knowledge、会话、模型提供方适配和调用审计 |
| `platform-modules/platform-scheduler` | 多租户调度定义、同步、触发与实例查询 |
| `platform-api/*` | 服务间稳定 HTTP 契约和租户执行上下文 |

各消费方通过配置为网关声明自己的路径前缀、租户映射和下游路由；源码不预置任何产品名、租户 ID 或业务服务名。

## 设计与扩展边界

平台按领域拆分独立服务，`platform-api` 提供服务间契约，AI Plus 提供可复用基础能力。
典型请求经过网关校验和租户路由后，由目标服务的 Controller 接收，应用服务编排用例，
Repository 负责数据库访问，外部适配器连接模型服务、S3 或 PowerJob。
网关保持响应式调用；领域服务使用 JPA 等阻塞依赖时，必须通过专用执行器隔离
Netty 事件循环，启用虚拟线程本身不等于消除阻塞。

| 变化点 | 当前设计 | 扩展方式 |
| --- | --- | --- |
| 系统与 Agent 服务调用 | Spring HTTP Interface 与自动配置的 WebClient | 在服务契约中增加操作，复用服务发现与可观测性配置 |
| 内部文件调用 | `FilesClient` 隔离调用方，`HttpFilesClient` 封装传输及响应校验 | 复用 `ApiResponse<T>` 和 Spring/Jackson 泛型解码，增加 DTO 与契约测试；下载保持流式传递 |
| SSE 连接维护 | Spring 定时调度调用 `SseOperations` 的心跳与清理契约 | 替换实现必须实现维护方法；维护范围仅为本实例，不依赖默认实现的类型判断 |
| SSE 生命周期事件 | Spring ApplicationEvent 与事件监听器 | 在独立监听器中处理连接、断开和消息事件 |
| 文件存储 | AWS SDK v2 的 S3 客户端与平台存储适配器 | 配置 S3 兼容端点；保留 SDK 签名、协议和资源管理能力 |
| 任务调度 | PowerJob 独立引擎与平台调度控制面 | 处理器位于任务所属领域，平台只维护定义、同步与执行查询 |

设计模式只用于真实变化点：适配器隔离外部协议，接口约束可替换实现，事件监听器解耦生命周期响应。
不额外引入通用工厂、全局服务定位器或与 AI Plus 重复的基础框架。

当前仍需关注两个运行边界：SSE 的 Redis Pub/Sub 桥接不提供离线重放或投递确认；
调度定义同步仍在数据库事务中调用 PowerJob。需要可靠投递或自动补偿时，必须同时设计
持久化、幂等、租约、失败重试及并发验收，不能仅用进程内异步事件替代持久化同步。

## 技术基线

- Oracle GraalVM 25.0.4
- Spring Boot 4.0.8
- Spring Cloud 2025.1.3
- Spring Cloud Alibaba 2025.1.0.0
- Gradle 9.7.1
- Jackson 3
- AI Plus 公共 Maven Central 制品；联合开发可通过 `AI_PLUS_HOME` 使用当前源码 composite build

依赖版本集中在 `gradle/libs.versions.toml`。同一用途的依赖优先通过 Version Catalog bundle 引入，避免多个模块重复维护依赖组合。

Spring Boot、Spring Cloud、Spring Cloud Alibaba 的升级必须共同核对官方支持矩阵，
并验证网关、Nacos 与 JPA 的实际运行契约，不能只按最高版本号单独抬升 BOM。
AWS SDK v2 使用 2.54.13；升级验收覆盖 S3 兼容端点、路径风格、签名与上传下载。
本地预签名测试验证 SDK 的端点解析与签名参数，不替代真实存储服务的读写验收。

选型依据：[Spring Cloud 发布说明](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/)、
[Alibaba 兼容线](https://sca.aliyun.com/en/docs/2025.x/overview/faq/)、
[Spring HTTP 客户端](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)、
[AWS SDK 发布说明](https://github.com/aws/aws-sdk-java-v2/releases/tag/2.54.13)。

## 配置与安全边界

仓库只声明运行时环境变量契约，不保存 Nacos 地址、命名空间、用户名、密码、数据库连接、Redis/Kafka/S3 凭据、JWT 密钥或真实数据。`application.yml` 中的值由部署环境提供，Gradle 不执行凭据资源替换，因此构建产物不会固化某个环境的连接信息。

`docker/docker-compose.yml` 只是无真实值的参数化编排示例。生产配置、数据库结构与数据迁移由使用方在独立私有交付物中维护，不属于本公开仓库。

Web Plus 接口载荷加密采用显式选择契约：未标注 `@ApiCrypto` 的端点始终使用标准 JSON，继承基础 Controller 不会隐式启用加密。只有调用方实现相同信封协议且端点确有载荷加密要求时，才在具体 JSON 端点标注；Dify、SSE、文件上传下载和其他非 JSON 流量保持未标注。

账户接口使用 `password` 字段传递 UTF-8 密码的 40 位小写 SHA-1 摘要，
空密码摘要、明文密码和 BCrypt 存储值均不是合法输入。服务端使用标准 BCrypt 存储摘要，
登录、注册、开通、改密和重置密码遵循同一协议。摘要属于密码等效凭据，传输仍须使用 HTTPS。
改密和重置接口使用 `oldPassword`、`newPassword`；输入校验与 BCrypt 编码复用
Web Plus Security 的 `PasswordProtocol` 和 `ProtocolPasswordEncoder`，平台只负责显式装配和业务编排。

`POST /agent/session/ask/stream` 返回 `start`、`delta`、`replace`、`complete` 或 `error` 事件。
Dify 适配器处理真实增量与全文替换；OpenAI-compatible 适配器当前返回单个完成结果，
不提供逐 token 输出。上游结果先持久化为可恢复状态，再完成消息和会话写入；重复幂等调用
可恢复已持久化结果。提供方错误只向调用方返回固定文案，审计记录保留错误类别，
不保存第三方异常正文。平台时间上下文只用于提供方请求，用户原始消息保留原文。

SSE 生命周期审计在 PostgreSQL 事务中按租户与连接标识串行合并事件，
断开先到也会保留终态，重复或延迟的连接事件不会覆盖首次断开事实。

## 全链路可观测性

所有平台服务统一引入 Web Plus TraceId 能力和 Spring Boot OpenTelemetry：

- HTTP 入站、网关短路响应和正常响应都会返回 `X-Trace-Id`；该值与活动 Micrometer Span 的 TraceId 一致。
- 服务间调用必须使用 Spring Boot 自动配置的 `WebClient.Builder`，由框架同时传播 W3C `traceparent` 与 `X-Trace-Id`。
- Spring Cloud Stream 消息使用标准 Observation，并在消息头恢复 Web Plus 请求上下文；`@Async`、虚拟线程和 Reactor 调度器共享同一上下文传播机制。
- 控制台日志统一输出应用名、TraceId 和 SpanId。OTLP Trace、Log、Metric 导出默认关闭，避免未配置 Collector 时持续重试。

部署环境按需配置：

| 环境变量 | 用途 | 默认值 |
| --- | --- | --- |
| `TRACING_SAMPLING_PROBABILITY` | Trace 采样概率，范围 0.0～1.0 | `0.1` |
| `OTEL_TRACING_EXPORT_ENABLED` | 启用 OTLP Trace 导出 | `false` |
| `OTEL_LOGGING_EXPORT_ENABLED` | 启用 OTLP Log 导出 | `false` |
| `OTEL_METRICS_EXPORT_ENABLED` | 启用 OTLP Metric 导出 | `false` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry Collector 根地址 | 无 |

启用任一导出前必须先提供可达的 Collector 地址；认证 Header、证书和后端凭据继续由部署环境注入。

## 构建与验证

Linux CI 以两条独立路径验证依赖：源码联调检出工作流固定的 AI Plus 提交，
发布依赖校验直接从 Maven Central 解析制品。两条路径都执行全部平台模块的构建与测试，
可部署制品仅由发布依赖校验产出，避免源码替换掩盖缺失或不完整的发布依赖。

源码联调：

```bash
AI_PLUS_HOME=/path/to/ai-plus ./gradlew buildAll --no-daemon --stacktrace
```

发布依赖校验不设置 `AI_PLUS_HOME` 或 `aiPlusHome`：

```bash
./gradlew buildAll --no-daemon --stacktrace
```

Gradle、Version Catalog、GitHub Actions 与 Docker 镜像由 `.github/dependabot.yml`
每周检查并分组提交更新；所有更新仍需通过源码联调和发布依赖两条校验路径，
不能绕过质量门禁直接进入 `main`。

PostgreSQL 并发约束测试使用 Testcontainers 和测试类内的最小临时 DDL，不读取或分发部署数据库脚本。

## 分支与许可证

当前公开基线只维护受保护的 `main`。后续修改通过功能分支和 Pull Request，并通过构建、测试与敏感信息扫描后合入。项目采用 [Apache License 2.0](LICENSE)。
