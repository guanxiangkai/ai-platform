package com.ai.files.controller;

import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileBusinessRootDTO;
import com.ai.api.files.dto.FileBusinessRootNodePageDTO;
import com.ai.api.files.dto.FileBusinessUploadDTO;
import com.ai.api.files.dto.FileBusinessDirectoriesRequestDTO;
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
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 幂等确保当前租户业务记录的稳定根目录。 */
    @PostMapping("/business-roots/ensure")
    public ApiResponse<FileBusinessRootDTO> ensureBusinessRoot(@RequestParam String businessType,
                                                               @RequestParam String businessId,
                                                               @RequestParam String displayName) {
        return ApiResponse.ok(filesService.ensureBusinessRoot(businessType, businessId, displayName));
    }

    /** 在已存在业务根目录中幂等创建空目录。 */
    @PostMapping("/business-roots/directories")
    public ApiResponse<Void> ensureBusinessDirectories(@RequestBody @jakarta.validation.Valid FileBusinessDirectoriesRequestDTO request) {
        filesService.ensureBusinessDirectories(request.rootBusinessType(), request.rootBusinessId(), request.relativePaths());
        return ApiResponse.ok(null);
    }

    /** 上传文件到当前租户业务根目录的相对路径。 */
    @PostMapping(value = "/business-roots/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<FileBusinessUploadDTO>> uploadToBusinessRoot(
            @RequestPart("file") Mono<FilePart> filePart,
            @RequestParam String rootBusinessType,
            @RequestParam String rootBusinessId,
            @RequestParam(required = false) String relativePath,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId) {
        return filePart.flatMap(file -> filesService.uploadToBusinessRoot(file, rootBusinessType,
                        rootBusinessId, relativePath, bizType, bizId))
                .map(ApiResponse::ok);
    }

    /** 分页读取业务根目录或其子目录的直属节点。 */
    @GetMapping("/business-roots/nodes")
    public ApiResponse<FileBusinessRootNodePageDTO> businessRootNodes(
            @RequestParam String businessType,
            @RequestParam String businessId,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String nodeType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(filesService.businessRootNodes(businessType, businessId, parentId, nodeType, page, size));
    }

    /** 查询文件指定版本或当前版本的真实元数据和业务根目录位置。 */
    @GetMapping("/metadata")
    public ApiResponse<FileBusinessUploadDTO> fileMetadata(@RequestParam String fileId,
                                                           @RequestParam(required = false) String versionId) {
        return ApiResponse.ok(filesService.fileMetadata(fileId, versionId));
    }

    /** 在不重传对象和不改写版本的前提下将既有文件节点迁入业务根目录。 */
    @PostMapping("/business-roots/place")
    public ApiResponse<FileBusinessUploadDTO> placeBusinessFile(
            @RequestParam String rootBusinessType,
            @RequestParam String rootBusinessId,
            @RequestParam String fileId,
            @RequestParam String expectedCurrentVersionId,
            @RequestParam String relativePath) {
        return ApiResponse.ok(filesService.placeBusinessFile(rootBusinessType, rootBusinessId, fileId,
                expectedCurrentVersionId, relativePath));
    }

    /** 下载当前租户的内部业务附件。 */
    @GetMapping("/{nodeId}")
    public Mono<ResponseEntity<Resource>> download(@PathVariable String nodeId) {
        return filesService.internalDownload(nodeId);
    }

    /** 下载可信内部服务指定的文件历史版本。 */
    @GetMapping("/{nodeId}/versions/{versionId}")
    public Mono<ResponseEntity<Resource>> downloadVersion(@PathVariable String nodeId,
                                                           @PathVariable String versionId) {
        return filesService.internalDownloadVersion(nodeId, versionId);
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
