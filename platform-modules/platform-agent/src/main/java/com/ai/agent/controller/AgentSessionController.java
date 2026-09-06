package com.ai.agent.controller;

import com.ai.agent.domain.dto.AgentSessionAskRequest;
import com.ai.agent.domain.vo.AgentSessionAskResponse;
import com.ai.agent.domain.vo.AgentSessionStreamEvent;
import com.ai.agent.service.AgentSessionService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 面向已登录用户的通用智能体会话接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RequiresLogin
@Tag(name = "智能体会话", description = "使用授权技能发起通用智能体会话")
@RestController
@RequestMapping("/agent/session")
@RequiredArgsConstructor
public class AgentSessionController {
    private final AgentSessionService sessions;

    /** 使用授权技能发起或继续智能体会话。 */
    @Operation(summary = "智能体问答")
    @PostMapping("/ask")
    public Mono<ApiResponse<AgentSessionAskResponse>> ask(
            @Valid @RequestBody AgentSessionAskRequest request) {
        return Mono.fromCallable(() -> ApiResponse.ok(sessions.ask(request)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 使用授权技能发起流式智能体会话。 */
    @Operation(summary = "智能体流式问答")
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentSessionStreamEvent>> stream(
            @Valid @RequestBody AgentSessionAskRequest request) {
        return Mono.fromCallable(() -> sessions.stream(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(events -> events)
                .map(event -> ServerSentEvent.builder(event).event(event.type()).build())
                .onErrorResume(error -> Flux.just(ServerSentEvent.builder(
                                AgentSessionStreamEvent.error(null, null, safeMessage(error)))
                        .event("error")
                        .build()));
    }

    private String safeMessage(Throwable error) {
        return error instanceof BizException && error.getMessage() != null && !error.getMessage().isBlank()
                ? error.getMessage()
                : "智能体会话暂不可用，请稍后重试";
    }
}
