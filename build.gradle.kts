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
                "-Xlint:deprecation"
            )
        )
    }
}

val lombokPlugin: Provider<PluginDependency> = libs.plugins.lombok

val springBootDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.boot.dependencies
val springCloudDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.cloud.dependencies
val springCloudAlibabaDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.cloud.alibaba.dependencies
val jackson3Bom: Provider<MinimalExternalModuleDependency> = libs.jackson3.bom
val jackson2Bom: Provider<MinimalExternalModuleDependency> = libs.jackson2.bom
val nettyBom: Provider<MinimalExternalModuleDependency> = libs.netty.bom
val runtimeSecurityPatches: Provider<ExternalModuleDependencyBundle> = libs.bundles.runtime.security.patches

val testingBundle: Provider<ExternalModuleDependencyBundle> = libs.bundles.testing
val junitPlatformLauncher: Provider<MinimalExternalModuleDependency> = libs.junit.platform.launcher
val slf4jApi: Provider<MinimalExternalModuleDependency> = libs.slf4j.api

subprojects {
    apply {
        plugin("java-library")
        plugin(lombokPlugin.get().pluginId)
    }

    dependencies {
        // 主框架 BOM 保持不变，只覆盖已确认存在漏洞且兼容的运行时补丁版本。
        "api"(platform(jackson3Bom.get()))
        "api"(platform(jackson2Bom.get()))
        "api"(platform(nettyBom.get()))
        // 导入 Spring Boot BOM（使用 api 确保版本管理传递到依赖方）
        // ⚠️ 根脚本 subprojects {} 中 Kotlin DSL 无法直接使用 api()，需通过字符串配置名
        "api"(platform(springBootDependencies.get()))
        "implementation"(platform(springBootDependencies.get()))
        "compileOnly"(platform(springBootDependencies.get()))
        "annotationProcessor"(platform(springBootDependencies.get()))
        testImplementation(platform(springBootDependencies.get()))
        // 导入 Spring Cloud BOM
        "api"(platform(springCloudDependencies.get()))
        // 导入 Spring Cloud Alibaba BOM
        "api"(platform(springCloudAlibabaDependencies.get()))
        // Lombok @Slf4j 生成的 Logger 字段需要编译期 SLF4J API
        "api"(slf4jApi)
        constraints {
            runtimeSecurityPatches.get().forEach { dependency ->
                // 版本目录返回不可变最小依赖；作为约束导入即可保留其精确版本。
                "api"(dependency)
            }
        }

        testImplementation(testingBundle)
        testRuntimeOnly(junitPlatformLauncher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

}
