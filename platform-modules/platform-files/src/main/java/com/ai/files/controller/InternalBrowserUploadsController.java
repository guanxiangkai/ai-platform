package com.ai.files.controller;

import com.ai.api.files.dto.*;
import com.ai.files.service.BrowserBusinessUploadService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 可信业务服务预约和封存浏览器原件；每个方法再次核对内部主体。 */
@RestController @RequestMapping("/internal/files/browser-uploads") @RequiredArgsConstructor
public class InternalBrowserUploadsController {
    private final BrowserBusinessUploadService uploads;
    /** 读取共享并发及单文件策略。 */
    @GetMapping("/policy") public ApiResponse<FileBrowserUploadPolicyDTO> policy(){return ApiResponse.ok(uploads.policy());}
    /** 为一个确定原件授予短时目录写权限。 */
    @PostMapping("/prepare") public ApiResponse<FileBrowserUploadTargetDTO> prepare(@RequestBody FileBrowserUploadTargetRequestDTO request){return ApiResponse.ok(uploads.prepare(request));}
    /** 核对不可变原件并移入用户不可写的系统目录。 */
    @PostMapping("/accept") public ApiResponse<FileBusinessUploadDTO> accept(@RequestBody FileBrowserUploadAcceptRequestDTO request){return ApiResponse.ok(uploads.accept(request));}
}
