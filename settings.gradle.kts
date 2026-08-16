/**
 * Gradle 多项目构建配置。
 *
 * 子项目采用显式清单，避免临时目录、示例或工具构建被文件扫描意外纳入发布图。
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "aiPlusPackages"
                    url = uri("https://maven.pkg.github.com/guanxiangkai/ai-plus")
                    credentials {
                        username = providers.environmentVariable("AI_PLUS_MAVEN_USERNAME")
                            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                            .orNull
                        password = providers.environmentVariable("AI_PLUS_MAVEN_TOKEN")
                            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                            .orNull
                    }
                }
            }
            filter {
                includeGroup("io.github.guanxiangkai")
            }
        }
        mavenCentral {
            content {
                excludeGroup("io.github.guanxiangkai")
            }
        }
        // 阿里云公共代理仓库（提升公共依赖拉取成功率）
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                excludeGroup("io.github.guanxiangkai")
            }
        }
        google {
            content {
                excludeGroup("io.github.guanxiangkai")
            }
        }
    }
}

rootProject.name = "ai-platform"

// ── AI Plus 可选源码联调 ──
// 默认从 GitHub Packages 消费已发布构件。仅在联合开发或发布前验证时，显式设置
// -PaiPlusHome=/path/to/ai-plus 或 AI_PLUS_HOME=/path/to/ai-plus 启用 composite build。
fun aiPlusBuildDirs(home: File): List<File> = listOf("jpa-plus", "redis-plus", "web-plus")
    .map { home.resolve(it) }

val configuredAiPlusHome = (
    providers.gradleProperty("aiPlusHome").orNull
        ?: providers.environmentVariable("AI_PLUS_HOME").orNull
)?.let { file(it).absoluteFile.toPath().normalize().toFile() }

if (configuredAiPlusHome != null) {
    val missingAiPlusBuilds = aiPlusBuildDirs(configuredAiPlusHome)
        .filterNot { it.resolve("settings.gradle.kts").isFile }
    if (missingAiPlusBuilds.isNotEmpty()) {
        throw GradleException(
            "Invalid AI Plus source build at ${configuredAiPlusHome.path}; missing Gradle builds: " +
                missingAiPlusBuilds.joinToString { it.name }
        )
    }
    aiPlusBuildDirs(configuredAiPlusHome).forEach { includeBuild(it) }
}

// ── 确定性的项目图 ──

val platformProjects = listOf(
    "platform-api",
    "platform-api:platform-agent-api",
    "platform-api:platform-core-api",
    "platform-api:platform-files-api",
    "platform-api:platform-system-api",
    "platform-auth",
    "platform-gateway",
    "platform-modules",
    "platform-modules:platform-agent",
    "platform-modules:platform-files",
    "platform-modules:platform-scheduler",
    "platform-modules:platform-sse",
    "platform-modules:platform-system",
)

platformProjects.forEach { projectPath ->
    val projectDirectory = rootDir.resolve(projectPath.replace(':', File.separatorChar))
    require(projectDirectory.resolve("build.gradle.kts").isFile) {
        "平台项目清单包含无效目录：$projectPath"
    }
    include(":$projectPath")
}
