# 通用 Agent 架构

## 定位

`platform-agent` 是与具体产品无关的独立平台服务。它只负责通用 Agent 运行时能力，不拥有任何产品业务模型，也不读取产品数据库。

通用能力包括：

- 按租户维护 Agent 定义、提供方协议、调用模式、发布状态和修订号；
- 通过提供方策略调用 OpenAI Chat Completions 协议与 Dify Chat/Workflow 协议；
- 按稳定 `agentCode` 为产品服务提供内部调用契约；
- 保存会话、消息、调用审计和语音转写审计；
- 对凭据加密存储，对管理接口和日志始终脱敏。

## 业务边界

产品服务通过消息、变量、`contextNamespace` 和 `contextReference` 传递业务上下文。上下文字段只是通用关联键，平台不解释其业务含义。

下列内容必须保留在产品仓库：

- 产品资讯、工单、备忘、快捷功能及其权限和数据表；
- 订单、合同、审批等垂直领域模型；
- 产品提示词、结构化结果校验、业务工具实现和业务数据访问。

例如，产品工单服务负责构造提示词与变量、验证结构化输出，再通过 `platform-agent-api` 按 Agent 编码调用平台；`platform-agent` 不包含任何产品工单类型。产品资讯继续保留在所属业务服务及业务表中，禁止挂到通用 Agent 路径。

## 依赖方向

```text
产品业务模块
        │  platform-agent-api
        ▼
platform-agent
        │  provider strategy
        ├─ OpenAI-compatible endpoint
        └─ Dify chat/workflow endpoint
```

`platform-agent-api` 只暴露 `agentCode + message + generic context + variables` 的稳定契约。平台实现不得依赖产品仓库，产品也不得依赖 `platform-agent` 实现模块。

## 数据与发布

Agent 数据写入平台数据库 `public` schema 的 `ai_agent_*` 表；Skill 与 Knowledge 也使用 `ai_*` 前缀。平台其他模块分别使用 `sys_*`、`file_*`、`sse_*`、`scheduler_*`，禁止在多模块数据库中出现无所有权前缀的业务表。所有租户数据通过 `tenant_id` 隔离。产品数据库只保留产品领域数据和对平台 Agent 主键的必要引用，不复制 Agent、Skill、Knowledge 或 SSE 表。定义默认以草稿创建，配置有效凭据并完成真实协议测试后才能发布；普通业务调用只允许访问已发布且启用的定义。

调用审计仅保存长度、变量数量、Token、耗时、状态和受限摘要，不保存提供方密钥或完整原始请求。数据库结构由 `deploy/database/V001__create_platform_schema.sql` 定义，表和字段具备中文注释；生产数据操作必须先完成备份、恢复路径和结果校验。
