dependencies {

    implementation(project(":platform-api:platform-core-api"))
    api(libs.web.plus.core)
    api(libs.web.plus.error)
    api(libs.web.plus.log)
    api(libs.web.plus.mq)
    api(libs.web.plus.security)
    api(libs.web.plus.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.mapstruct.plus.spring.boot.starter)
    implementation(libs.jpa.plus.starter)
    runtimeOnly(libs.postgresql)
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // ===== 服务 API 契约（HTTP Interface 客户端）=====
    implementation(project(":platform-api:platform-system-api"))

    // ===== SSE 固定实现依赖（MQ + Redis 票据/限流/桥接）=====
    implementation(libs.bundles.ai.sse)

}
