package com.ai.agent.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AsrServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExtractTextFromJsonObject() throws Exception {
        assertThat(extract("{\"text\":\"  会议开始  \"}"))
                .isEqualTo("会议开始");
    }

    @Test
    void shouldExtractAndJoinTextFromJsonArray() throws Exception {
        assertThat(extract("[\"第一句\", {\"transcript\": \"第二句\"}, null]"))
                .isEqualTo("第一句\n第二句");
    }

    @Test
    void shouldExtractPlainTextJsonValue() throws Exception {
        assertThat(extract("\"  纯文本  \""))
                .isEqualTo("纯文本");
    }

    @Test
    void shouldReturnEmptyTextForNullValue() throws Exception {
        assertThat(extract("null")).isEmpty();
        assertThat(AsrService.extractText(null)).isEmpty();
    }

    @Test
    void shouldIgnoreNonTextScalarValues() throws Exception {
        assertThat(extract("false")).isEmpty();
        assertThat(extract("0")).isEmpty();
        assertThat(extract("{\"data\":false}")).isEmpty();
    }

    private String extract(String json) throws Exception {
        JsonNode value = objectMapper.readTree(json);
        return AsrService.extractText(value);
    }
}
