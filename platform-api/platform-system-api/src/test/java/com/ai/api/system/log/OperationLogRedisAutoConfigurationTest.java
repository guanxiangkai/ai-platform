package com.ai.api.system.log;

import io.github.guanxiangkai.web.plus.log.spi.OperationLogHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class OperationLogRedisAutoConfigurationTest {

    @Test
    void shouldPreserveTenantAndOperationFields() {
        PlatformOperationLog log = new PlatformOperationLog();
        log.setTenantId("tenant-1");
        log.setTraceId("trace-1");
        log.setUsername("tester");
        log.setStatus("SUCCESS");
        log.setOperationId("operation-1");
        log.setOperationTypeCode("QUERY");
        log.setLogTime(LocalDateTime.of(2026, 8, 3, 10, 30));

        Map<String, String> values = OperationLogStreamCodec.encode(log);

        assertThat(values)
                .containsEntry("tenantId", "tenant-1")
                .containsEntry("traceId", "trace-1")
                .containsEntry("username", "tester")
                .containsEntry("status", "SUCCESS")
                .containsEntry("operationId", "operation-1")
                .containsEntry("operationTypeCode", "QUERY")
                .containsEntry("logTime", "2026-08-03T10:30");
    }

    @Test
    void shouldRejectOperationLogWithoutRequiredStreamFields() {
        PlatformOperationLog log = new PlatformOperationLog();
        log.setTenantId("tenant-1");
        log.setOperationId("operation-1");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> OperationLogStreamCodec.encode(log))
                .withMessage("操作日志缺少必填字段：status");
    }

    @Test
    void shouldRequireSwitchAndSharedAuthRedisTemplate() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OperationLogRedisAutoConfiguration.class));

        runner.run(context -> assertThat(context).doesNotHaveBean(OperationLogHandler.class));

        runner.withPropertyValues("web-plus.log.operation-log-enabled=true")
                .withBean("authStringRedisTemplate", StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> assertThat(context).hasSingleBean(OperationLogHandler.class));
    }
}
