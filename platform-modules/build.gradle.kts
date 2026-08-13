// ===== 预解析 Version Catalog 引用（subprojects {} 内部无法直接访问 libs）=====
var mapstructPlus: Provider<MinimalExternalModuleDependency> = libs.mapstruct.plus.processor
var lombokMapstructBinding: Provider<MinimalExternalModuleDependency> = libs.lombok.mapstruct.binding
var cloudService: Provider<ExternalModuleDependencyBundle> = libs.bundles.cloud.service
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
        annotationProcessor(mapstructPlus)
        // Lombok-MapStruct 绑定：确保 Lombok AST 变换在 MapStruct 代码生成之前完成
        // io.freefair.lombok 插件仅在检测到 org.mapstruct:mapstruct-processor 直接依赖时自动添加；
        // 使用 mapstruct-plus-processor 时需手动声明。
        annotationProcessor(lombokMapstructBinding)

    }
}
