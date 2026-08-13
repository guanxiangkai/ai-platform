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
