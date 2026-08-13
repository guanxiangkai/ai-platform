package com.ai.files.controller;

import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.files.service.FilesService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 仅供受信微服务调用的业务附件接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class InternalFilesController {

    private final FilesService filesService;

    /** 上传当前租户的内部业务附件。 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<FileUploadResultDTO>> upload(
            @RequestPart("file") Mono<FilePart> filePart,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId) {
        return filePart.flatMap(file -> filesService.internalUpload(file, bizType, bizId))
                .map(ApiResponse::ok);
    }

    /** 下载当前租户的内部业务附件。 */
    @GetMapping("/{nodeId}")
    public Mono<ResponseEntity<Resource>> download(@PathVariable String nodeId) {
        return filesService.internalDownload(nodeId);
    }

    /** 查询当前租户业务记录关联的活动附件。 */
    @GetMapping("/business-files")
    public ApiResponse<List<FileBusinessFileDTO>> activeBusinessFiles(
            @RequestParam String bizType,
            @RequestParam String bizId) {
        return ApiResponse.ok(filesService.internalActiveBusinessFiles(bizType, bizId));
    }

    /** 幂等回收当前租户业务记录关联的活动附件。 */
    @PostMapping("/business-files/recycle")
    public ApiResponse<Void> recycleBusinessFiles(
            @RequestParam String bizType,
            @RequestParam String bizId) {
        filesService.internalRecycleBusinessFiles(bizType, bizId);
        return ApiResponse.ok();
    }
}
