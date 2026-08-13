plugins {
    alias(libs.plugins.springboot)
}

dependencies {

    // ===== 网关核心（Gateway + Nacos + 响应式 Redis + Actuator + Web Plus Core/Security）=====
    // ⚠️ Gateway 基于 WebFlux + Netty，绝对不可引入任何包含 starter-web 的模块！
    implementation(libs.bundles.ai.gateway)
    implementation(project(":platform-api:platform-core-api"))

}
