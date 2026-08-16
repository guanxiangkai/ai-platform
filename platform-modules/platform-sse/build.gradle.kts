dependencies {

    api(libs.bundles.platform.sse.api)
    implementation(libs.bundles.platform.sse.implementation)
    runtimeOnly(libs.postgresql)

    // ===== 服务 API 契约（HTTP Interface 客户端）=====
    implementation(project(":platform-api:platform-system-api"))

}
