plugins {
    alias(libs.plugins.springboot)
}

dependencies {

    // ===== platform-auth 固定实现依赖（Web Plus + Nacos + JPA + Redis + JWT）=====
    // Auth 服务只引入核心、安全和日志能力，不引入 Web Plus Web 层基类。
    implementation(libs.bundles.platform.auth)
    runtimeOnly(libs.postgresql)
    // JPA 实体与 OpenAPI 注解只参与编译，实际运行能力已由固定实现 bundle 提供。
    compileOnly(libs.bundles.auth.compileOnly)

    implementation(project(":platform-api:platform-core-api"))

}
