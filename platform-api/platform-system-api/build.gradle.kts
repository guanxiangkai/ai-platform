dependencies {
    // 内部客户端从当前用户上下文透传租户 ID，确保下游数据库查询继续受租户过滤器约束。
    implementation(libs.web.plus.security)
    // 产品服务通过平台统一契约将操作日志写入共享 Redis Stream，由 platform-system 集中落库。
    api(libs.web.plus.log)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(project(":platform-api:platform-core-api"))
}
