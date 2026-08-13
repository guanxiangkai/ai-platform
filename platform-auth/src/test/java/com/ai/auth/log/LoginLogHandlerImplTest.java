package com.ai.auth.log;

import com.ai.auth.repository.AuthLoginLogRepository;
import com.ai.auth.properties.AuthSuperAdminProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginLogHandlerImplTest {

    @Test
    void shouldPersistLoginLogDirectlyWithoutRedisStream() {
        AuthLoginLogRepository repository = mock(AuthLoginLogRepository.class);
        AuthSuperAdminProperties properties = new AuthSuperAdminProperties(false, null, null);
        LoginLogHandlerImpl handler = new LoginLogHandlerImpl(repository, properties);
        AuthLoginLogRecord record = new AuthLoginLogRecord();
        record.setTenantId("tenant-1");
        record.setUserId("user-1");
        record.setUsername("tester");
        record.setAction("LOGIN");
        record.setStatus("SUCCESS");
        record.setUserAgent("Mozilla/5.0 (Mac OS X) Chrome/140.0");

        handler.handle(record);

        ArgumentCaptor<AuthLoginLogRecord> captor = ArgumentCaptor.forClass(AuthLoginLogRecord.class);
        verify(repository).saveAndFlush(captor.capture());
        AuthLoginLogRecord saved = captor.getValue();
        assertThat(saved.getId()).hasSize(32);
        assertThat(saved.getLogTime()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getBrowser()).isEqualTo("Google Chrome");
        assertThat(saved.getOs()).isEqualTo("macOS");
    }

    @Test
    void shouldPersistConfiguredSuperAdminWithStableUserId() {
        AuthLoginLogRepository repository = mock(AuthLoginLogRepository.class);
        AuthSuperAdminProperties properties = new AuthSuperAdminProperties(
                true,
                "root",
                "$2b$12$" + "A".repeat(53)
        );
        LoginLogHandlerImpl handler = new LoginLogHandlerImpl(repository, properties);
        AuthLoginLogRecord record = new AuthLoginLogRecord();
        record.setUserId("internal-service");
        record.setUsername("root");
        record.setAction("LOGIN");

        handler.handle(record);

        verify(repository).saveAndFlush(record);
        assertThat(record.getUserId()).isEqualTo("platform-super-admin");
        assertThat(record.getTenantId()).isEqualTo("0");
    }
}
