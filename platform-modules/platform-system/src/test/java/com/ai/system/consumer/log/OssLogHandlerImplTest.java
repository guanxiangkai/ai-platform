package com.ai.system.consumer.log;

import com.ai.api.context.TenantExecutionScope;
import com.ai.system.domain.entity.OssLog;
import com.ai.system.service.IOssLogService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** OSS 日志异步持久化的租户上下文回归测试。 */
class OssLogHandlerImplTest {

    @Test
    void shouldRestoreEntityTenantScopeBeforeSaving() {
        IOssLogService service = mock(IOssLogService.class);
        OssLogHandlerImpl handler = new OssLogHandlerImpl(service);
        OssLog entity = new OssLog();
        entity.setTenantId(" tenant-1 ");
        entity.setOriginalName("diagram.png");
        AtomicReference<String> tenantDuringSave = new AtomicReference<>();
        doAnswer(invocation -> {
            tenantDuringSave.set(TenantExecutionScope.currentTenantId());
            return null;
        }).when(service).createEntity(entity);

        handler.handle(entity);

        assertThat(tenantDuringSave).hasValue("tenant-1");
        assertThat(TenantExecutionScope.currentTenantId()).isNull();
    }

    @Test
    void shouldRejectLogWithoutTenantId() {
        IOssLogService service = mock(IOssLogService.class);
        OssLogHandlerImpl handler = new OssLogHandlerImpl(service);
        OssLog entity = new OssLog();
        entity.setOriginalName("diagram.png");

        handler.handle(entity);

        verifyNoInteractions(service);
    }
}
