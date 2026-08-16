dependencies {
    implementation(libs.bundles.platform.scheduler)
    runtimeOnly(libs.postgresql)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // 调度控制面仅调用 PowerJob OpenAPI 客户端；不承载任何 Worker 处理器。
    implementation(project(":platform-api:platform-system-api"))
}
