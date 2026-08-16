# ai-platform

当前代码和数据模型定义为 1.0.0 首个正式基线。源码、SQL、注释、测试、配置和文档只保留当前有效模型，不建立并行兼容契约或描述不存在的版本演进。公开数据库基线只表达完整结构和中性最小种子；真实租户、组织、用户、菜单、区域和业务初始化数据必须通过受控部署流程注入，不得进入仓库。

跨服务重复的安全、租户、数据、缓存、Web 和前端能力必须分别下沉到 AI Plus 或 AI UI；平台仓库仅保留平台领域组合。设计模式用于真实变化点，不为凑齐模式数量增加空抽象。公开 API 和关键边界统一使用准确的中文 Javadoc。

`platform-agent` 是跨产品、跨租户的通用独立服务，和网关、认证、系统、文件、SSE 一样由本仓库部署。它不得依赖任何产品模块、产品数据库、产品配置或硬编码产品默认智能体。

所有 Agent 数据统一写入平台数据库并使用 `ai_` 模块前缀；平台其他模块分别使用 `sys_`、`file_`、`sse_`、`scheduler_` 前缀。租户数据通过 `DataTenantEntity` 的 `tenant_id` 实施行级隔离；不得创建独立 Agent 数据库或在产品数据库复制 Agent、Skill、Knowledge、SSE 定义与记录。产品只经共享网关的可配置入口访问服务。

部署与数据变更：配置中心、数据库、Redis 和外部服务连接参数必须由部署环境或 Secret 注入，仓库不得保存真实地址、命名空间、账号、凭据或租户值。根 `docker/` 只保留通用 JDK Dockerfile、构建脚本和可移植编排模板；数据库通过 `deploy/database/apply-schema.sh` 应用 `V001` 结构基线，并保持 `cleanDisabled=true`。

所有 Java 模块的编译工具链和容器运行时统一使用 Oracle GraalVM 25.0.4；容器直接使用官方 `container-registry.oracle.com/graalvm/jdk:25.0.4` 基础镜像，并在构建时强制拉取。

`platform-scheduler` 是跨产品共享的调度控制面，PowerJob Server 是其独立容器内的第三方调度引擎，两者由根 `docker/docker-compose.yml` 统一编排但保持进程和镜像隔离。业务任务处理器按领域归属嵌入具体产品服务，产品业务逻辑不得进入通用调度服务。
