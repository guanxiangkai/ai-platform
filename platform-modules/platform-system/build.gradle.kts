dependencies {

    implementation(libs.bundles.platform.system)
    annotationProcessor(libs.spring.boot.configuration.processor)
    runtimeOnly(libs.postgresql)

    // ===== 跨服务 API 契约（HTTP Interface 客户端）=====
    implementation(project(":platform-api:platform-core-api"))
    implementation(project(":platform-api:platform-system-api"))
}
