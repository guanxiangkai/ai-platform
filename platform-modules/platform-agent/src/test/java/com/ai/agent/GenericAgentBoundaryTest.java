package com.ai.agent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class GenericAgentBoundaryTest {
    @Test
    void agentSourceMustNotContainProductBusinessTypes() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/ai/agent");
        String source = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .map(this::read)
                .reduce("", (left, right) -> left + "\n" + right)
                .toLowerCase(Locale.ROOT);

        assertThat(source)
                .doesNotContain("businessrefid")
                .doesNotContain("businesstype")
                .doesNotContain("projectid")
                .doesNotContain("newscontroller")
                .doesNotContain("newsrepository")
                .doesNotContain("com.ai.business");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
