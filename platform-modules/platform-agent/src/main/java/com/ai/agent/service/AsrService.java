package com.ai.agent.service;

import com.ai.agent.config.AgentAsrProperties;
import com.ai.agent.config.AsrUpstreamProtocol;
import com.ai.agent.domain.AgentVoiceRecognitionState;
import com.ai.agent.domain.dto.AsrDTO;
import com.ai.agent.domain.entity.AgentVoiceRecord;
import com.ai.agent.repository.AgentVoiceRecordRepository;
import io.github.guanxiangkai.web.plus.core.exception.CoreBizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 将受控语音输入转发给 ASR 上游，并只保存必要的租户审计字段。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AsrService {
    private static final String DASHSCOPE_SSE_HEADER = "X-DashScope-SSE";
    private static final int DASHSCOPE_DATA_URL_MAX_BYTES = 10_000_000;
    private static final Map<String, String> DASHSCOPE_FORMATS = Map.ofEntries(
            Map.entry("audio/aac", "aac"),
            Map.entry("audio/flac", "flac"),
            Map.entry("audio/m4a", "m4a"),
            Map.entry("audio/mp4", "mp4"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/ogg", "ogg"),
            Map.entry("audio/wav", "wav"),
            Map.entry("audio/webm", "webm"),
            Map.entry("audio/x-flac", "flac"),
            Map.entry("audio/x-m4a", "m4a"),
            Map.entry("audio/x-wav", "wav"),
            Map.entry("video/webm", "webm"));
    private static final Set<String> DASHSCOPE_LANGUAGE_HINTS = Set.of(
            "zh", "en", "ja", "ko", "vi", "th", "id", "ms", "tl", "hi",
            "ar", "fr", "de", "es", "pt", "ru", "it", "nl", "sv", "da",
            "fi", "no", "el", "pl", "cs", "hu", "ro", "bg", "hr", "sk");
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "audio/aac", "audio/flac", "audio/m4a", "audio/mp4", "audio/mpeg",
            "audio/ogg", "audio/wav", "audio/webm", "audio/x-flac", "audio/x-m4a",
            "audio/x-wav", "video/webm");
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile(
            "(?i)(auto|[a-z]{2,3}(?:-[a-z0-9]{2,8})*)");

    private final AgentAsrProperties properties;
    private final AgentVoiceRecordRepository records;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    /** 转写上传的语音文件。 */
    public Mono<AsrDTO.AsrResult> recognize(FilePart file, String language) {
        if (file == null) return Mono.error(CoreBizException.invalid("语音文件不能为空"));
        String type;
        try {
            type = normalizeContentType(file.headers().getContentType());
        } catch (RuntimeException exception) {
            return Mono.error(exception);
        }
        String normalizedLanguage;
        try {
            normalizedLanguage = normalizeLanguage(language);
        } catch (RuntimeException exception) {
            return Mono.error(exception);
        }
        return DataBufferUtils.join(file.content(), maxUploadBytes()).map(buffer -> {
            try {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                return bytes;
            } finally {
                DataBufferUtils.release(buffer);
            }
        }).onErrorMap(DataBufferLimitException.class,
                error -> CoreBizException.invalid("语音文件超过允许大小"))
                .flatMap(bytes -> recognize(bytes, file.filename(), type, normalizedLanguage));
    }

    private Mono<AsrDTO.AsrResult> recognize(byte[] bytes, String filename, String type, String language) {
        if (bytes.length == 0) return Mono.error(CoreBizException.invalid("语音文件内容为空"));
        if (!properties.isSpeechToTextEnabled() || !StringUtils.hasText(properties.getSpeechToTextUrl())) {
            return Mono.error(CoreBizException.invalid("ASR 上游未配置"));
        }
        if (bytes.length > properties.getSpeechToTextMaxUploadSize().toBytes()) {
            return Mono.error(CoreBizException.invalid("语音文件超过允许大小"));
        }
        return createRecord(filename, type, bytes.length, language).flatMap(record ->
                upstream(bytes, filename, type, language).map(this::payload)
                        .flatMap(value -> complete(record, value, language))
                        .onErrorResume(error -> failed(record, error).then(Mono.error(error))));
    }

    private Mono<String> upstream(byte[] bytes, String filename, String type, String language) {
        if (properties.getSpeechToTextProtocol() == AsrUpstreamProtocol.DASHSCOPE_MULTIMODAL) {
            return dashScopeUpstream(bytes, type, language);
        }
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", bytes).filename(safeFilename(filename)).contentType(mediaType(type));
        if (StringUtils.hasText(language)) body.part("language", language);
        WebClient.RequestBodySpec request = webClientBuilder.build().post().uri(properties.getSpeechToTextUrl());
        if (StringUtils.hasText(properties.getSpeechToTextApiKey())) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getSpeechToTextApiKey());
        }
        return request.contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build())).retrieve().bodyToMono(String.class)
                .timeout(properties.getSpeechToTextTimeout());
    }

    private Mono<String> dashScopeUpstream(byte[] bytes, String type, String language) {
        ObjectNode body = dashScopeBody(objectMapper, properties.getSpeechToTextModel(), bytes, type, language);
        WebClient.RequestBodySpec request = webClientBuilder.build().post().uri(properties.getSpeechToTextUrl());
        if (StringUtils.hasText(properties.getSpeechToTextApiKey())) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getSpeechToTextApiKey());
        }
        return request.header(DASHSCOPE_SSE_HEADER, "disable")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body).retrieve().bodyToMono(String.class)
                .timeout(properties.getSpeechToTextTimeout());
    }

    /** 构造百炼 Qwen Audio 同步识别请求，供协议回归测试复用。 */
    static ObjectNode dashScopeBody(
            ObjectMapper mapper, String model, byte[] bytes, String type, String language) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", StringUtils.hasText(model) ? model : "qwen-audio-3.0-asr-flash");
        ObjectNode input = body.putObject("input");
        ObjectNode message = input.putArray("messages").addObject();
        message.put("role", "user");
        ObjectNode audio = message.putArray("content").addObject();
        audio.put("type", "input_audio");
        audio.putObject("input_audio").put("data", dashScopeDataUrl(bytes, type));
        ObjectNode parameters = body.putObject("parameters");
        parameters.put("format", dashScopeFormat(type));
        String languageHint = dashScopeLanguageHint(language);
        if (languageHint != null) {
            parameters.putArray("language_hints").add(languageHint);
        }
        return body;
    }

    /** 将当前允许的 MIME 类型映射为百炼支持的音频格式。 */
    static String dashScopeFormat(String type) {
        String format = DASHSCOPE_FORMATS.get(type);
        if (format == null) throw CoreBizException.invalid("DashScope ASR 不支持该语音文件类型");
        return format;
    }

    /** 将语言区域标识映射为百炼语言提示；无法可靠映射时不发送提示。 */
    static String dashScopeLanguageHint(String language) {
        if (!StringUtils.hasText(language) || "auto".equalsIgnoreCase(language.trim())) return null;
        String base = language.trim().toLowerCase(Locale.ROOT).split("-", 2)[0];
        return DASHSCOPE_LANGUAGE_HINTS.contains(base) ? base : null;
    }

    /** 构造并校验百炼要求的音频 Data URL，避免超出 10,000,000 字节限制。 */
    static String dashScopeDataUrl(byte[] bytes, String type) {
        String prefix = "data:" + type + ";base64,";
        long encodedBytes = 4L * ((bytes.length + 2L) / 3L);
        if (prefix.length() + encodedBytes > DASHSCOPE_DATA_URL_MAX_BYTES) {
            throw CoreBizException.invalid("语音文件超过允许大小");
        }
        return prefix + java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private Mono<AgentVoiceRecord> createRecord(
            String filename, String type, long contentLength, String language) {
        return Mono.fromCallable(() -> {
            AgentVoiceRecord record = new AgentVoiceRecord();
            record.setTenantId(requireTenantId());
            record.setUserId(requireUserId());
            record.setFileName(limit(safeFilename(filename), AgentVoiceRecord.FILE_NAME_MAX_LENGTH));
            record.setContentType(limit(type, AgentVoiceRecord.CONTENT_TYPE_MAX_LENGTH));
            record.setContentLength(contentLength);
            record.setLanguage(limit(language, AgentVoiceRecord.LANGUAGE_MAX_LENGTH));
            record.setRecognitionStatus(AgentVoiceRecognitionState.RUNNING);
            return records.save(record);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AsrDTO.AsrResult> complete(AgentVoiceRecord record, JsonNode value, String language) {
        return Mono.fromCallable(() -> {
            String transcript = extractText(value);
            record.setTranscript(transcript);
            record.setRecognitionStatus(StringUtils.hasText(transcript)
                    ? AgentVoiceRecognitionState.SUCCEEDED
                    : AgentVoiceRecognitionState.NEED_REVIEW);
            record.setErrorMessage(null);
            AgentVoiceRecord saved = records.save(record);
            return new AsrDTO.AsrResult(saved.getId(), transcript, trimToNull(language));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> failed(AgentVoiceRecord record, Throwable error) {
        return Mono.fromRunnable(() -> {
            record.setRecognitionStatus(AgentVoiceRecognitionState.FAILED);
            record.setErrorMessage(limit(error.getMessage(), AgentVoiceRecord.ERROR_MESSAGE_MAX_LENGTH));
            records.save(record);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private JsonNode payload(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            return objectMapper.createObjectNode().put("text", text(value, ""));
        }
    }

    /** 从 ASR 上游标准 JSON 响应中递归提取转写文本。 */
    static String extractText(JsonNode value) {
        if (value == null || value.isNull()) return "";
        if (value.isString()) {
            String text = value.stringValue();
            return text == null ? "" : text.trim();
        }
        if (value.isValueNode()) return "";
        if (value.isArray()) {
            StringBuilder all = new StringBuilder();
            value.forEach(part -> {
                String text = extractText(part);
                if (StringUtils.hasText(text)) {
                    if (!all.isEmpty()) all.append('\n');
                    all.append(text);
                }
            });
            return all.toString();
        }
        for (String key : new String[]{"text", "transcript", "transcription", "output", "sentence", "content", "data", "result", "results", "segments"}) {
            String text = extractText(value.get(key));
            if (StringUtils.hasText(text)) return text;
        }
        return "";
    }

    private String requireUserId() {
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) throw CoreBizException.invalid("未获取到当前用户");
        return userId.trim();
    }

    private String requireTenantId() {
        String tenantId = SecurityUtils.getTenantId();
        if (!StringUtils.hasText(tenantId) || "0".equals(tenantId)) {
            throw CoreBizException.invalid("未获取到有效租户上下文");
        }
        return tenantId.trim();
    }

    private int maxUploadBytes() {
        return Math.toIntExact(properties.getSpeechToTextMaxUploadSize().toBytes());
    }

    private String normalizeContentType(MediaType value) {
        if (value == null) throw CoreBizException.invalid("语音文件类型不能为空");
        String type = (value.getType() + "/" + value.getSubtype()).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_CONTENT_TYPES.contains(type)) throw CoreBizException.invalid("不支持的语音文件类型");
        return type;
    }

    private String normalizeLanguage(String value) {
        if (!StringUtils.hasText(value)) return null;
        String language = value.trim();
        if (language.length() > AgentVoiceRecord.LANGUAGE_MAX_LENGTH
                || !LANGUAGE_PATTERN.matcher(language).matches()) {
            throw CoreBizException.invalid("语音识别语言参数无效");
        }
        return language;
    }

    private MediaType mediaType(String value) {
        return MediaType.parseMediaType(value);
    }

    private String safeFilename(String value) {
        String name = text(value, "voice.wav").replace('\\', '/');
        return text(name.substring(name.lastIndexOf('/') + 1), "voice.wav");
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
