// ===== 预解析 Version Catalog 引用（subprojects {} 内部无法直接访问 libs）=====
val mapstructProcessors: Provider<ExternalModuleDependencyBundle> = libs.bundles.mapstruct.processors
val cloudService: Provider<ExternalModuleDependencyBundle> = libs.bundles.cloud.service
val springboot: Provider<PluginDependency> = libs.plugins.springboot

subprojects {

    apply {
        plugin(springboot.get().pluginId)
    }

    // 运行时 JVM 参数：允许 Netty 等库调用受限本地方法
    tasks.withType<JavaExec>().configureEach {
        jvmArgs(
            "--enable-native-access=ALL-UNNAMED"
        )
    }

    dependencies {

        // ===== 微服务基础设施（Nacos 注册 & 配置 + 负载均衡 + Actuator）=====
        implementation(cloudService)

        // MapStruct-Plus 注解处理器（必须显式声明，注解处理器不可传递，否则 @AutoMapper 不生成转换器代码）
        annotationProcessor(mapstructProcessors)

    }
}
