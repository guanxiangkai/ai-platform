// ===== 预解析 Version Catalog 引用（subprojects {} 内部无法直接访问 libs）=====
val webPlusCore: Provider<MinimalExternalModuleDependency> = libs.web.plus.core

subprojects {

    apply(plugin = "maven-publish")

    dependencies {

        // ===== api —— 需要暴露给消费方的类型 =====
        api(webPlusCore)                 // ApiResponse、BaseException

    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("platformApi") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                name = "platformPackages"
                url = uri(
                    providers.environmentVariable("PLATFORM_MAVEN_REPOSITORY")
                        .orElse("https://maven.pkg.github.com/guanxiangkai/ai-platform")
                        .get()
                )
                credentials {
                    username = providers.environmentVariable("PLATFORM_MAVEN_USERNAME")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orNull
                    password = providers.environmentVariable("PLATFORM_MAVEN_TOKEN")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orNull
                }
            }
        }
    }
}
