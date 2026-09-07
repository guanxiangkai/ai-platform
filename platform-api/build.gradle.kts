import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata

// ===== 预解析 Version Catalog 引用（subprojects {} 内部无法直接访问 libs）=====
var webPlusCore: Provider<MinimalExternalModuleDependency> = libs.web.plus.core
var httpInterface: Provider<ExternalModuleDependencyBundle> = libs.bundles.http.`interface`

subprojects {
    apply(plugin = "maven-publish")

    dependencies {

        // ===== api —— 需要暴露给消费方的类型 =====
        api(webPlusCore)                 // ApiResponse、BaseException

        // WebFlux + LoadBalancer（@HttpExchange 声明式客户端 + 负载均衡 WebClient）
        api(httpInterface)

    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                name = "apiBundle"
                url = rootProject.layout.buildDirectory.dir("platform-api-maven-repository").get().asFile.toURI()
            }
        }
    }

    // 平台运行期通过 enforcedPlatform 锁定依赖；该约束不能作为 Gradle 模块元数据传给消费方。
    // API bundle 仅发布标准 Maven POM 与 Java JAR，消费者按自己的兼容矩阵解析公共依赖。
    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }

}
