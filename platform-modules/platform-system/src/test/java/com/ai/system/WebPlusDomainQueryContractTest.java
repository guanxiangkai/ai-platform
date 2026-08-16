package com.ai.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Web Plus 领域查询与部署变量的静态契约测试。 */
class WebPlusDomainQueryContractTest {

    @Test
    void mustUseDomainQueriesWithoutUnboundedAllApi() throws IOException {
        assertThat(readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/service/impl/MenuServiceImpl.java"))
                .doesNotContain("all()")
                .contains("getAssignableMenus()", "findByEnabledTrueAndDeletedFalse()");
        assertThat(readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/service/impl/DeptServiceImpl.java"))
                .doesNotContain("all()")
                .contains("repository.findByDeletedFalse()");
        assertThat(readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/service/impl/TenantServiceImpl.java"))
                .doesNotContain("all()")
                .contains("findByEnabledTrueAndDeletedFalse(");
        assertThat(readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/service/impl/DictServiceImpl.java"))
                .doesNotContain("all()")
                .contains("findByEnabledTrueAndDeletedFalse(");
        assertThat(readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/service/impl/PostServiceImpl.java"))
                .doesNotContain("all()")
                .contains("findByEnabledTrueAndDeletedFalse(");
        assertThat(readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/service/impl/RoleServiceImpl.java"))
                .doesNotContain("all()")
                .contains("findByEnabledTrueAndDeletedFalse(");
    }

    @Test
    void mustRequireRolePermissionForAuthorizationQueries() throws IOException {
        String controller = readProjectFile("platform-modules/platform-system/src/main/java/com/ai/system/controller/RoleController.java");
        assertThat(controller).contains("@RequiresPermission(\"system:role:assignPermission\")");
        assertThat(controller).contains("menuService.getAssignableMenus()", "menuService.getAssignableMenuTree()");
    }

    @Test
    void mustRequireExplicitInfrastructureAndCurrentWebPlus() throws IOException {
        String application = readProjectFile("deploy/nacos/application.yml");
        String agent = readProjectFile("deploy/nacos/platform-agent.yml");
        String files = readProjectFile("deploy/nacos/platform-files.yml");
        String compose = readProjectFile("docker/docker-compose.yml");

        assertThat(application)
                .contains("${PLATFORM_DB_HOST}", "${PLATFORM_DB_PORT}", "${PLATFORM_DB_NAME}",
                        "${PLATFORM_DB_SCHEMA}", "${REDIS_HOST}", "${REDIS_PORT}");
        assertThat(agent)
                .contains("${REDIS_HOST}", "${REDIS_PORT}");
        assertThat(files)
                .contains("${OSS_ENDPOINT}", "${OSS_BUCKET}", "${OSS_REGION}");
        assertThat(compose)
                .contains("${NACOS_SERVER_ADDR:?NACOS_SERVER_ADDR is required}",
                        "${PLATFORM_DB_HOST:?PLATFORM_DB_HOST is required}",
                        "${REDIS_HOST:?REDIS_HOST is required}",
                        "${OSS_ENDPOINT:?OSS_ENDPOINT is required}");
        assertThat(readProjectFile("gradle/libs.versions.toml"))
                .contains("web-plus-web = \"5.2.0\"");
        assertThat(readProjectFile("deploy/database/V001__create_platform_schema.sql"))
                .doesNotContain("INSERT INTO", "COPY public.", "UPDATE public.", "DELETE FROM public.");
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null && !Files.exists(directory.resolve("settings.gradle.kts"))) {
            directory = directory.getParent();
        }
        if (directory == null) {
            throw new IllegalStateException("未找到 ai-platform 项目根目录");
        }
        return Files.readString(directory.resolve(relativePath));
    }
}
