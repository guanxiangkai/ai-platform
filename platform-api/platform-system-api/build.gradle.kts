dependencies {
    api(libs.bundles.http.contract)
    implementation(libs.bundles.http.client.runtime)
    // 租户透传、操作日志协议与 Redis Stream 是系统 API 的固定实现契约。
    implementation(libs.bundles.platform.api.system)
    implementation(project(":platform-api:platform-core-api"))
}
