/**
 * 这是一个 Gradle 构建脚本，用于配置项目的构建过程。
 *
 */
plugins {
    java
    `maven-publish`
    alias(libs.plugins.lombok)
    alias(libs.plugins.springboot) apply false   // 仅下载到 classpath，子模块按需 apply
}

allprojects {
    // 为所有项目设置项目分组和版本
    group = project.group
    version = project.version

    // 从 gradle.properties 中获取属性值
    val jdk = providers.gradleProperty("jdk").get()
    val encoding = providers.gradleProperty("encoding").get()

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(jdk.toInt()))
                vendor.set(org.gradle.jvm.toolchain.JvmVendorSpec.ORACLE)
            }
        }
    }

    // 编译选项
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = jdk
        targetCompatibility = jdk
        options.encoding = encoding
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Xlint:deprecation",
                "-Werror"
            )
        )
    }
}

val lombokPlugin: Provider<PluginDependency> = libs.plugins.lombok

val springBootDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.boot.dependencies
val springCloudDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.cloud.dependencies
val springCloudAlibabaDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.cloud.alibaba.dependencies

val springBootStarterTest: Provider<MinimalExternalModuleDependency> = libs.spring.boot.starter.test
val junitJupiter: Provider<MinimalExternalModuleDependency> = libs.junit.jupiter
val junitPlatformLauncher: Provider<MinimalExternalModuleDependency> = libs.junit.platform.launcher
val slf4jApi: Provider<MinimalExternalModuleDependency> = libs.slf4j.api
val logbackClassic: Provider<MinimalExternalModuleDependency> = libs.logback.classic

subprojects {
    apply {
        plugin("java-library")
        plugin(lombokPlugin.get().pluginId)
    }

    dependencies {
        // 平台应用以 Spring Cloud Alibaba 的正式兼容矩阵为准，禁止三方库携带的构建 BOM
        // 反向抬升运行时 Spring Boot 版本；api 只传递版本建议，平台自身配置强制使用锁定版本。
        // ⚠️ 根脚本 subprojects {} 中 Kotlin DSL 无法直接使用 api()，需通过字符串配置名
        "api"(platform(springBootDependencies.get()))
        "implementation"(enforcedPlatform(springBootDependencies.get()))
        "compileOnly"(enforcedPlatform(springBootDependencies.get()))
        "annotationProcessor"(enforcedPlatform(springBootDependencies.get()))
        testImplementation(enforcedPlatform(springBootDependencies.get()))
        // 导入 Spring Cloud BOM
        "api"(platform(springCloudDependencies.get()))
        // 导入 Spring Cloud Alibaba BOM
        "api"(platform(springCloudAlibabaDependencies.get()))
        // Lombok @Slf4j 生成的 Logger 字段需要编译期 SLF4J API
        "api"(slf4jApi)

        // 日志实现（仅运行时需要，编译时通过 SLF4J 抽象层使用）
        runtimeOnly(logbackClassic)

        testImplementation(springBootStarterTest)
        testImplementation(junitJupiter)
        testRuntimeOnly(junitPlatformLauncher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("-Xshare:off", "--enable-native-access=ALL-UNNAMED")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

}

val buildAll = tasks.register("buildAll") {
    group = "build"
    description = "构建并测试全部平台模块"
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.named("build") {
    dependsOn(buildAll)
}
