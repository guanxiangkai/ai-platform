package com.ai.agent.controller;

import com.ai.agent.domain.dto.AsrDTO;
import com.ai.agent.service.AsrService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 面向已登录用户的智能体语音转写接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RequiresLogin
@Tag(name = "智能体语音", description = "受控语音输入转写与审计")
@RestController
@RequestMapping("/agent/speech")
@RequiredArgsConstructor
public class AgentSpeechController {
    private final AsrService asr;

    /** 转写上传的语音文件。 */
    @Operation(summary = "语音转写")
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<AsrDTO.AsrResult>> transcribe(
            @RequestPart("file") Mono<FilePart> file,
            @RequestParam(required = false) String language) {
        return file.flatMap(value -> asr.recognize(value, language)).map(ApiResponse::ok);
    }
}
