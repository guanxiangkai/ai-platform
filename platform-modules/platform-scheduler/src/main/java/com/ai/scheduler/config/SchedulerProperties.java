package com.ai.scheduler.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台调度控制面及可调用业务处理器目录配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.scheduler")
public class SchedulerProperties {

    /** 管理端任务与实例查询的单页记录数上限。 */
    @Min(1)
    @Max(1_000)
    private int maxPageSize = 200;

    /** 单个调度实例允许配置的最长执行时间，单位为毫秒。 */
    @Min(1_000)
    @Max(31_536_000_000L)
    private long maxInstanceTimeLimitMs = 7L * 24 * 60 * 60 * 1_000;

    /** 单个任务允许并存的最大实例数。 */
    @Min(1)
    @Max(100_000)
    private int maxInstanceCount = 1_000;

    /** 单个实例允许配置的最大并发线程数。 */
    @Min(1)
    @Max(10_000)
    private int maxConcurrency = 1_000;

    /** 实例级或任务级允许配置的最大重试次数。 */
    @Min(0)
    @Max(10_000)
    private int maxRetryCount = 100;

    /** 固定频率或固定延迟表达式允许的最小毫秒数。 */
    @Min(1_000)
    @Max(86_400_000)
    private long minFixedIntervalMs = 1_000;

    /** PowerJob Server 的 Docker 内网地址列表。 */
    private List<String> serverAddresses = new ArrayList<>();

    /** 应用逻辑编码到 PowerJob 应用及处理器目录的映射。 */
    private Map<String, Application> applications = new LinkedHashMap<>();

    /**
     * 单个产品应用的调度配置。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    @Getter
    @Setter
    public static class Application {
        /** 归属租户，必须与网关传入的租户上下文一致。 */
        private String tenantId;
        /** 管理页面显示名称。 */
        private String displayName;
        /** PowerJob 中的应用名称。 */
        private String appName;
        /** PowerJob OpenAPI 应用密码，仅注入控制面容器。 */
        private String password;
        /** 允许管理的业务处理器白名单。 */
        private List<Handler> handlers = new ArrayList<>();
    }

    /**
     * 业务服务内可被调度的处理器目录项。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    @Getter
    @Setter
    public static class Handler {
        /** PowerJob 处理器 Bean 名称。 */
        private String processorInfo;
        /** 页面显示名称。 */
        private String displayName;
        /** 处理器用途说明。 */
        private String description;
    }
}
