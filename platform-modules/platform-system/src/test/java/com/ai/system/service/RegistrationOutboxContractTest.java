package com.ai.system.service;

import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.RegisterState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注册可靠开通的源码状态机契约。
 */
class RegistrationOutboxContractTest {

    @Test
    void statesShouldSeparatePendingProcessingAndFinalDelivery() {
        assertThat(OutboxDeliveryState.values()).contains(
                OutboxDeliveryState.PENDING, OutboxDeliveryState.PROCESSING,
                OutboxDeliveryState.RETRY, OutboxDeliveryState.SENT, OutboxDeliveryState.DEAD);
        assertThat(RegisterState.values()).contains(RegisterState.PROVISIONING, RegisterState.ACTIVE);
    }

    @Test
    void remoteClientShouldOnlyBeCalledByTransactionFreeProcessor() throws Exception {
        Path sourceDirectory = projectRoot()
                .resolve("platform-modules/platform-system/src/main/java/com/ai/system/service");
        String processor = Files.readString(sourceDirectory.resolve("RegistrationOutboxProcessor.java"));
        String transactionService = Files.readString(sourceDirectory.resolve("RegistrationOutboxTransactionService.java"));
        assertThat(processor).contains("directoryClient.currentAssignment", "directoryClient.linkUser");
        assertThat(processor).doesNotContain("@Transactional");
        assertThat(transactionService).doesNotContain("directoryClient.");
        assertThat(processor).doesNotContain("exception.getMessage()");
    }

    private Path projectRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null && !Files.exists(directory.resolve("settings.gradle.kts"))) {
            directory = directory.getParent();
        }
        if (directory == null) {
            throw new IllegalStateException("未找到 ai-platform 项目根目录");
        }
        return directory;
    }
}
