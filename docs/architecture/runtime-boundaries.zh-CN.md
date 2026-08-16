# 运行时边界与部署规范

## 系统边界

- `ai-platform` 只承载跨产品通用能力：网关、认证、系统管理、文件、SSE、Agent 和调度控制面。
- 区域、导入模板、消息、天气和当前用户设置属于 `platform-system`，不得在产品后端复制实体和对外 API。
- 系统设置只保留语言、主题、通知、自动保存和安全等稳定字段；产品偏好写入命名空间 `extensions`。
- 天气外部契约只读，数据写入仅由内部同步任务访问；消息和设置始终以当前认证用户为边界。
- 产品只承载各自业务服务和业务任务处理器，不复制平台通用实现。
- `platform-agent`、`platform-scheduler` 都是独立部署服务；通用表示可跨产品复用，不表示与其他服务合并进程。

## Nacos 约定

- Namespace 表示环境，具体值只能由部署环境注入。
- 同一环境全部服务使用同一个可配置发现分组，保证网关和 `lb://` 客户端按服务名互相发现。
- 配置分组由部署环境注入；配置 Group 不参与服务隔离。
- 平台与各产品分别维护数据库与配置，不能用 Nacos Group 替代数据边界。

## 网关产品入口

- 产品路径前缀与租户标识只在部署配置的 `ai.gateway.tenant-path-ids` 中建立映射，基础源码不识别任何固定产品名。
- 带产品前缀且允许匿名访问的登录、刷新和公开配置路径必须逐项加入 `ai.gateway.tenant-exclude-paths`；未配置路径默认要求认证。
- Agent 路由只有在前缀存在于 `tenant-path-ids` 时才保留用户访问令牌；名称相似但未登记的路径按普通下游请求清除访问令牌。

## 可信代理与客户端地址

- 网关默认不信任任何转发头；`spring.cloud.gateway.server.webflux.trusted-proxies` 使用永不匹配的默认正则，并关闭 Netty 自定义转发头解析。
- 前方确有受控反向代理时，部署方必须同时配置 Spring Cloud Gateway 的代理正则和 `ai.gateway.trusted-proxy-ips` 精确 IP 列表，两处必须来自同一实际拓扑。
- `ai.gateway.trusted-proxy-ips` 不接受 CIDR 或主机名。网关从转发链右侧剥离可信代理，选择最接近信任边界的非可信地址，不能直接采用可由客户端插入的最左地址。
- 网关清除外部提交的身份、可信令牌和已验证地址头，再用内部共享令牌向下游写入 `X-Verified-Client-Ip`；下游只有在共享令牌匹配时才接受该地址。
- 登录保护、网关限流、服务限流和防重复提交的 Redis Key 只保存完整 SHA-256 指纹；请求日志不得输出原始查询值、Token 或凭据。
- 网关限流后端异常时默认拒绝请求并返回 503，避免登录防暴破和全局限流被依赖故障静默绕过；只有部署方具备其他入口防护并完成风险评估后，才能显式开启 `ai.gateway.rate-limit-fail-open`。

## JWT 验证概要

- Auth 只用 RSA 私钥签发 RS256 JWT；Gateway 只持有对应公钥，并固定校验算法与部署配置的 `key-id`，不能按令牌头动态选择算法或外部密钥地址。
- Auth 只接受 PKCS#8 RSA 私钥，Gateway 只接受 X.509 SubjectPublicKeyInfo 公钥，两端都拒绝低于 2048 位的 RSA 密钥；不支持的 PKCS#1 私钥必须由部署流程显式转换，不能在解析失败后静默猜测格式。
- 访问令牌与刷新令牌使用不同的 `typ`、`token_use` 和 `aud`。Gateway 只接受访问令牌，Auth 刷新接口只接受刷新令牌，两个验证规则互斥。
- 两端必须一致校验 `iss`；同时要求 `sub`、`iat`、`exp`、`jti`，允许的时钟偏差默认 60 秒且不得超过 5 分钟。
- `JWT_PRIVATE_KEY`、`JWT_PUBLIC_KEY` 以及覆盖默认概要的 `JWT_KEY_ID`、`JWT_ISSUER`、Audience 与有效期只由部署环境或 Secret 注入。修改任一概要值时必须同步更新 Auth 与 Gateway 并使存量令牌失效。

## 内部客户端约定

- 简单声明式 JSON 契约按服务分组使用 Spring Framework HTTP Service Registry，显式服务组名同时作为 Spring Cloud LoadBalancer 的服务标识，便于 AOT 提前发现代理和服务依赖。
- 需要 Multipart、流式响应或逐操作错误映射的客户端保留显式 WebClient 组合，不为追求统一而损失错误语义。
- 可信转发令牌和租户上下文以显式过滤器配置到目标客户端；平台禁止注册修改全部 WebClient 的全局 Bean 后处理器。
- 额外 Redis DB 连接统一通过 Redis Plus 构建器装配，并继承命令、连接、关闭超时、客户端名与 TLS 参数。

## 应用容器技术路线

- 平台运行时统一使用 Spring Boot、Spring Framework、Spring Cloud 与 Spring Data，不在同一应用或模块中混用 Quarkus ArC/CDI 容器。
- Quarkus 的 Spring DI 兼容层用于把部分 Spring 注解映射到 Quarkus 构建期 CDI 模型，不提供完整 Spring `ApplicationContext`，因此不能替代当前自动配置、Bean 定义扩展、Spring Data 与 Spring Cloud 生命周期。
- 只有出现需要独立扩缩容、极低冷启动或受限内存的全新无状态服务时，才允许以独立进程做 Quarkus 技术验证；验证必须使用同一业务负载比较启动时间、常驻内存、吞吐、AOT 构建时间和运维复杂度，并保持网关协议与数据所有权不变。

## 调度边界

- `platform-scheduler` 保存租户任务定义、校验处理器白名单并通过 PowerJob OpenAPI 管理任务。
- PowerJob Server 使用独立容器和独立数据库，由平台 Compose 统一编排；不与 Java 控制面合并镜像。
- 各产品分别使用独立 PowerJob 应用，业务处理器只存在于所属业务服务。
- 管理页面复用 `ai-ui` 的调度组件，各产品只适配自身登录态、权限和网关前缀。

## 容器与端口

- 平台 Java 服务共用一个 JDK 运行镜像模板；前端镜像只负责 Nginx 静态资源和反向代理。
- 后端只有 `platform-gateway` 发布宿主机端口，其他 Java 服务和 PowerJob 仅在部署环境指定的容器网络内暴露容器端口。
- PowerJob OpenAPI 仅供 `platform-scheduler` 内网访问，应用密码必须通过部署 Secret 注入。
- PowerJob 5.1.2 的 `/server/checkConnectivity` 在上游修复前必须由反向代理、服务网格或东西向防火墙阻断；仅“不映射宿主机端口”不能替代受信网络和最小出站策略。
