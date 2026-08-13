package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.LoginLogPageDTO;
import com.ai.system.domain.vo.LoginLogPageVO;
import com.ai.system.service.ILoginLogService;
import com.ai.system.service.IOperationLogService;
import com.ai.system.service.IOssLogService;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogControllerTest {

    @Test
    void logListAlwaysUsesFilteredServiceQuery() {
        ILoginLogService loginLogService = mock(ILoginLogService.class);
        LogController controller = controller(loginLogService);
        LoginLogPageDTO query = new LoginLogPageDTO();
        @SuppressWarnings("unchecked")
        PageResponse<LoginLogPageVO> page = mock(PageResponse.class);
        when(loginLogService.list(query)).thenReturn(page);

        controller.loginList(query);

        verify(loginLogService).list(query);
    }

    private LogController controller(ILoginLogService loginLogService) {
        return new LogController(
                loginLogService,
                mock(IOperationLogService.class),
                mock(IOssLogService.class)
        );
    }
}
