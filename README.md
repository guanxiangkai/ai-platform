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

## 技术基线

- Oracle GraalVM 25.0.4
- Spring Boot 4.1.0
- Spring Cloud 2025.1.2
- Spring Cloud Alibaba 2025.1.0.0
- Gradle 9.6.1
- Jackson 3
- AI Plus 公共 Maven Central 制品；联合开发可通过 `AI_PLUS_HOME` 使用当前源码 composite build

依赖版本集中在 `gradle/libs.versions.toml`。同一用途的依赖优先通过 Version Catalog bundle 引入，避免多个模块重复维护依赖组合。

## 配置与安全边界

仓库只声明运行时环境变量契约，不保存 Nacos 地址、命名空间、用户名、密码、数据库连接、Redis/Kafka/S3 凭据、JWT 密钥或真实数据。`application.yml` 中的值由部署环境提供，Gradle 不执行凭据资源替换，因此构建产物不会固化某个环境的连接信息。

`docker/docker-compose.yml` 只是无真实值的参数化编排示例。生产配置、数据库结构与数据迁移由使用方在独立私有交付物中维护，不属于本公开仓库。

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

Linux CI 会同时检出 AI Plus 当前 `main`，以源码 composite build 验证平台实际使用的是 AI Plus 的最新公开基线：

```bash
AI_PLUS_HOME=/path/to/ai-plus ./gradlew buildAll --no-daemon --stacktrace
```

PostgreSQL 并发约束测试使用 Testcontainers 和测试类内的最小临时 DDL，不读取或分发部署数据库脚本。

## 分支与许可证

当前公开基线只维护受保护的 `main`。后续修改通过功能分支和 Pull Request，并通过构建、测试与敏感信息扫描后合入。项目采用 [Apache License 2.0](LICENSE)。
