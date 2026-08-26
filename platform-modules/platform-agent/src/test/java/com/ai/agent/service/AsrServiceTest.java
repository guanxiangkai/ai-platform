package com.ai.agent.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldBuildDashScopeMultimodalRequest() {
        ObjectNode body = AsrService.dashScopeBody(objectMapper, "qwen-audio-3.0-asr-flash",
                new byte[]{0x01, 0x02}, "audio/wav", "zh-CN");

        assertThat(body.get("model").stringValue()).isEqualTo("qwen-audio-3.0-asr-flash");
        assertThat(body.at("/input/messages/0/role").stringValue()).isEqualTo("user");
        assertThat(body.at("/input/messages/0/content/0/type").stringValue()).isEqualTo("input_audio");
        assertThat(body.at("/input/messages/0/content/0/input_audio/data").stringValue())
                .isEqualTo("data:audio/wav;base64,AQI=");
        assertThat(body.at("/parameters/format").stringValue()).isEqualTo("wav");
        assertThat(body.at("/parameters/language_hints/0").stringValue()).isEqualTo("zh");
    }

    @Test
    void shouldMapDashScopeFormatsAndOmitUnknownLanguageHint() {
        assertThat(AsrService.dashScopeFormat("audio/mpeg")).isEqualTo("mp3");
        assertThat(AsrService.dashScopeFormat("audio/ogg")).isEqualTo("ogg");
        assertThat(AsrService.dashScopeFormat("audio/webm")).isEqualTo("webm");
        assertThat(AsrService.dashScopeFormat("video/webm")).isEqualTo("webm");
        assertThat(AsrService.dashScopeFormat("audio/flac")).isEqualTo("flac");
        assertThat(AsrService.dashScopeLanguageHint("en-US")).isEqualTo("en");
        assertThat(AsrService.dashScopeLanguageHint("vi-VN")).isEqualTo("vi");
        assertThat(AsrService.dashScopeLanguageHint("auto")).isNull();
        assertThat(AsrService.dashScopeLanguageHint("xx-YY")).isNull();
        assertThatThrownBy(() -> AsrService.dashScopeFormat("audio/unknown"))
                .hasMessageContaining("不支持");
    }

    @Test
    void shouldRejectDashScopeDataUrlOverTenMillionBytes() {
        byte[] bytes = new byte[7_500_000];
        assertThat(AsrService.dashScopeDataUrl(new byte[7_499_970], "audio/wav")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(10_000_000);
        assertThatThrownBy(() -> AsrService.dashScopeDataUrl(bytes, "audio/wav"))
                .hasMessageContaining("超过允许大小");
    }

    @Test
    void shouldExtractDashScopeNestedSentenceText() throws Exception {
        assertThat(extract("{\"output\":{\"output\":{\"sentence\":{\"text\":\"  百炼识别结果  \"}}}}"))
                .isEqualTo("百炼识别结果");
    }

    private String extract(String json) throws Exception {
        JsonNode value = objectMapper.readTree(json);
        return AsrService.extractText(value);
    }
}
