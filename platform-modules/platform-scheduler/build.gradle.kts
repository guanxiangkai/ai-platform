dependencies {
    implementation(libs.bundles.platform.service)
    implementation(libs.web.plus.log)
    implementation(libs.spring.boot.starter.validation)
    runtimeOnly(libs.postgresql)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // 调度控制面仅调用 PowerJob OpenAPI 客户端；不承载任何 Worker 处理器。
    implementation(libs.powerjob.client)
    implementation(project(":platform-api:platform-system-api"))
}
