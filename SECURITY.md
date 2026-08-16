# 安全策略

## 支持范围

安全修复只面向仍受维护的最新稳定分支。部署模板不包含生产秘密；实际运行环境的 Nacos、数据库、
Redis、对象存储、JWT 和调度凭据由部署方负责保管、轮换与撤销。

## 报告漏洞

请通过 GitHub 仓库的 **Security → Report a vulnerability** 私密报告功能提交问题，不要先创建
公开 Issue，也不要附带真实生产凭据、个人信息、内部地址、租户数据或客户数据。报告应尽量包含
受影响模块、版本、最小复现步骤、影响判断和建议缓解措施。

维护者完成初步确认前，请不要公开可利用细节。若 GitHub 私密漏洞报告不可用，可先创建一个
不含利用细节和敏感数据的普通 Issue，请求维护者开启私密沟通渠道。

## 凭据泄露

若怀疑秘密已经进入日志、制品或 Git 历史，应先识别全部依赖方并在权威系统中轮换；删除当前源码
不能使历史对象、缓存、Actions 日志或已下载副本失效。

## PowerJob 已知风险

PowerJob 5.1.2 是截至 2026-08-13 的最新正式版本，但其 Server 的
`/server/checkConnectivity` 仍受 [CVE-2025-14518](https://nvd.nist.gov/vuln/detail/CVE-2025-14518)
影响，上游 [Issue #1144](https://github.com/PowerJob/PowerJob/issues/1144) 尚未关闭，也没有可直接升级的
修复版本。

`platform-scheduler` 只使用 PowerJob OpenAPI Client，不包含上游 `ServerController` 暴露的漏洞入口；
依赖扫描仍会因为共享的 `powerjob-common` 包报告该 CVE。部署模板中的第三方 `powerjob-server`
容器则是真实受影响对象，不得把应用侧判断用于豁免容器扫描。

在上游发布修复版之前，部署方必须同时执行以下措施：

- 不发布 PowerJob Server 宿主机端口，也不允许非受信容器加入其运行网络；
- 在反向代理、服务网格或东西向防火墙阻断 `/server/checkConnectivity`；
- 对 Server 的数据库、Worker 与其他目标实施最小化出站网络策略；
- 启用账号权限，轮换管理员与应用密码，并独立扫描实际选择的 `POWERJOB_SERVER_IMAGE` 摘要。

无法落实上述控制时，不应启用 PowerJob Server。版本升级后仍需复核上游修复提交、镜像摘要和
接口级回归测试，不能只依据版本号移除风险记录。
