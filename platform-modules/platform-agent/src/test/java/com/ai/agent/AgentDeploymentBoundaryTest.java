package com.ai.agent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDeploymentBoundaryTest {
    @Test
    void agentMustUsePlatformDatabaseAndSingleBaselineSchema() throws IOException {
        Path root = repositoryRoot();
        List<Path> deploymentFiles = List.of(
                root.resolve("docker/docker-compose.yml"),
                root.resolve("deploy/nacos/platform-agent.yml"));
        List<Path> documentationFiles = List.of(
                root.resolve("README.md"),
                root.resolve("AGENTS.md"),
                root.resolve("docs/architecture/agent.md"));

        String deploymentContract = deploymentFiles.stream()
                .map(this::read)
                .reduce("", (left, right) -> left + "\n" + right);
        String documentationContract = documentationFiles.stream()
                .map(this::read)
                .reduce("", (left, right) -> left + "\n" + right);

        assertThat(deploymentContract)
                .doesNotContain("AGENT_DB_")
                .doesNotContain("ai_agent");
        assertThat(documentationContract)
                .doesNotContain("/ai-agent");
        String schema = read(root.resolve("deploy/database/V001__create_platform_schema.sql"));
        assertThat(schema)
                .contains("CREATE TABLE public.ai_agent_config")
                .contains("CREATE TABLE public.ai_agent_session_record")
                .contains("CREATE TABLE public.ai_agent_message_record")
                .contains("CREATE TABLE public.ai_agent_call_record")
                .contains("CREATE TABLE public.ai_agent_voice_record")
                .contains("pk_ai_agent_config")
                .contains("uk_ai_agent_config_tenant_code_active")
                .doesNotContain("CREATE TABLE public.agent_");
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("未找到 ai-platform 仓库根目录");
        return current;
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("读取部署契约失败: " + path, exception);
        }
    }
}
