package com.ai.agent.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 通用智能体服务的 ASR 上游契约配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.agent")
public class AgentAsrProperties {
    private boolean speechToTextEnabled = true;
    private String speechToTextUrl;
    private String speechToTextApiKey;

    /** ASR 上游请求超时时间。 */
    @NotNull
    private Duration speechToTextTimeout = Duration.ofSeconds(60);

    /** 单个语音文件允许上传的最大容量。 */
    @NotNull
    private DataSize speechToTextMaxUploadSize = DataSize.ofMegabytes(10);

    /** 校验上游请求超时处于安全范围。 */
    @AssertTrue(message = "ASR 上游请求超时必须大于 0 且不超过 10 分钟")
    public boolean isSpeechToTextTimeoutWithinBounds() {
        return speechToTextTimeout != null
                && !speechToTextTimeout.isZero()
                && !speechToTextTimeout.isNegative()
                && speechToTextTimeout.compareTo(Duration.ofMinutes(10)) <= 0;
    }

    /** 校验上传容量可安全传入响应式缓冲区聚合边界。 */
    @AssertTrue(message = "ASR 上传容量必须大于 0 且不超过 1GB")
    public boolean isSpeechToTextMaxUploadSizeWithinBounds() {
        return speechToTextMaxUploadSize != null
                && speechToTextMaxUploadSize.toBytes() > 0
                && speechToTextMaxUploadSize.compareTo(DataSize.ofGigabytes(1)) <= 0;
    }
}
