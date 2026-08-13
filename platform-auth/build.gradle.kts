plugins {
    alias(libs.plugins.springboot)
}

dependencies {

    // ===== ai-auth 固定实现依赖（Web Plus Core/Security/Log + Nacos + Redis + JWT）=====
    // Auth 服务只引入核心、安全和日志能力，不引入 Web Plus Web 层基类。
    implementation(libs.bundles.ai.auth)
    runtimeOnly(libs.postgresql)
    compileOnly(libs.jakarta.persistence.api)

    // ===== OpenAPI 注解（当前仅用于 Controller / DTO 注解编译期可见性）=====
    compileOnly(libs.springdoc.openapi.starter.webflux.ui)

    implementation(project(":platform-api:platform-core-api"))

}
