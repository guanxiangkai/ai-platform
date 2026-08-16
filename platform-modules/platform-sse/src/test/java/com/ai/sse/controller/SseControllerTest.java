package com.ai.sse.controller;

import io.github.guanxiangkai.web.plus.security.aspect.AuthPermissionAspect;
import io.github.guanxiangkai.web.plus.security.spi.PermissionResolver;
import io.github.guanxiangkai.web.plus.core.context.CurrentUser;
import io.github.guanxiangkai.web.plus.core.spi.CurrentUserProvider;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.sse.service.ISseService;
import com.ai.sse.service.SseTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SseControllerTest {

    @Test
    void createTicketShouldNotBeBlockedByWebPlusRequiresLoginAspect() {
        SseTicketService ticketService = mock(SseTicketService.class);
        when(ticketService.createTicket(any(), any(), any(), any())).thenReturn("ticket-123");

        SseController target = new SseController(mock(ISseService.class), ticketService,
                io.github.guanxiangkai.web.plus.core.net.ClientIpResolver.directPeer());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new AuthPermissionAspect(permissionResolver(), currentUserProvider()));
        SseController controller = proxyFactory.getProxy();

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/sse/ticket")
                        .header(AuthConstants.HeaderConstants.USER_ID, "user-123")
                        .header(AuthConstants.HeaderConstants.TENANT_ID, "tenant-456")
                        .header("User-Agent", "JUnit")
                        .build()
        );

        ApiResponse<String> response = controller.createTicket(exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.data()).isEqualTo("ticket-123");
        verify(ticketService).createTicket(eq("user-123"), eq("tenant-456"), any(), eq("JUnit"));
    }

    private PermissionResolver permissionResolver() {
        return new PermissionResolver() {
            @Override
            public boolean hasPermission(io.github.guanxiangkai.web.plus.core.context.CurrentUser user, String permission) {
                return false;
            }

            @Override
            public boolean hasRole(io.github.guanxiangkai.web.plus.core.context.CurrentUser user, String role) {
                return false;
            }
        };
    }

    private CurrentUserProvider currentUserProvider() {
        CurrentUser currentUser = CurrentUser.ofUserId("user-123");
        return new CurrentUserProvider() {
            @Override
            public Optional<CurrentUser> getCurrentUser() {
                return Optional.of(currentUser);
            }

            @Override
            public Mono<Optional<CurrentUser>> getCurrentUserMono() {
                return Mono.just(Optional.of(currentUser));
            }
        };
    }
}
