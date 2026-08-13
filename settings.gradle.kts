/**
 * Gradle 多项目构建配置
 *
 * 自动扫描根目录下包含 .gradle.kts 构建文件的子目录，注册为子项目。
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
        mavenCentral()
        google()
    }
}

rootProject.name = "ai-platform"

// ── AI Plus 可选源码联调 ──
// 默认从 Maven Central 消费已发布构件。仅在联合开发或发布前验证时，显式设置
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

// ── 自动发现并注册所有子项目 ──

val excludedDirs = setOf(
    ".ci",
    ".git",
    ".gradle",
    ".idea",
    "buildSrc",
    "build",
    "gradle",
    "src",
    "logs",
    "profile",
)
val buildFileExtension = ".gradle.kts"

fileTree(rootDir).matching {
    include("**/*$buildFileExtension")
    exclude(excludedDirs.map { "**/$it/**" })
}.files.mapNotNull { file ->
    file.parentFile?.takeIf { it.isPotentialProject(excludedDirs) }
}.toSet().forEach { it.registerAsSubproject() }

// ── 工具函数 ──

fun File.isPotentialProject(excluded: Set<String>): Boolean =
    isDirectory && this != rootDir && name !in excluded &&
            listFiles()?.any { it.name.endsWith(buildFileExtension) } == true

fun File.registerAsSubproject() {
    val projectName = toRelativeString(rootDir).replace(File.separator, ":")
    val buildFile = listFiles()?.find { it.name.endsWith(buildFileExtension) }

    include(":$projectName")
    project(":$projectName").apply {
        projectDir = this@registerAsSubproject
        buildFileName = buildFile?.name ?: "${name}$buildFileExtension"
    }
}
