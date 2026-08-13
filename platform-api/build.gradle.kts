// ===== 预解析 Version Catalog 引用（subprojects {} 内部无法直接访问 libs）=====
var webPlusCore: Provider<MinimalExternalModuleDependency> = libs.web.plus.core
var httpInterface: Provider<ExternalModuleDependencyBundle> = libs.bundles.http.`interface`

subprojects {
    dependencies {

        // ===== api —— 需要暴露给消费方的类型 =====
        api(webPlusCore)                 // ApiResponse、BaseException

        // WebFlux + LoadBalancer（@HttpExchange 声明式客户端 + 负载均衡 WebClient）
        api(httpInterface)

    }

}
