package com.ai.files.controller;

import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.api.files.dto.FileAccessUrlDTO;
import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.files.domain.dto.FileRequests;
import com.ai.files.domain.entity.FileGrant;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.domain.entity.FileVersion;
import com.ai.files.domain.vo.FileViews;
import com.ai.files.service.FilesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/** 面向租户用户的企业文件中心接口。 */
@RestController
@RequestMapping({"/files", "/api/files"})
@RequiredArgsConstructor
@Tag(name = "文件中心", description = "个人和部门空间、共享权限、文件版本、回收站与编辑锁")
public class FilesController {

    private final FilesService filesService;

    /** 返回当前用户可访问的文件空间。 */
    @GetMapping("/spaces")
    @Operation(summary = "查询可访问空间")
    public ApiResponse<List<FileSpace>> spaces() {
        return ApiResponse.ok(filesService.accessibleSpaces());
    }

    /** 获取或创建当前用户的个人空间。 */
    @PostMapping("/spaces/personal")
    @Operation(summary = "获取个人空间")
    public ApiResponse<FileSpace> personalSpace() {
        return ApiResponse.ok(filesService.personalSpace());
    }

    /** 获取或创建当前用户所属部门的空间。 */
    @PostMapping("/spaces/department")
    @Operation(summary = "获取部门空间")
    public ApiResponse<FileSpace> departmentSpace(@RequestBody FileRequests.DepartmentSpace request) {
        return ApiResponse.ok(filesService.departmentSpace(request));
    }

    /** 查询空间配额。 */
    @GetMapping("/spaces/{spaceId}/usage")
    @Operation(summary = "查询空间配额")
    public ApiResponse<FileViews.SpaceUsage> usage(@PathVariable String spaceId) {
        return ApiResponse.ok(filesService.usage(spaceId));
    }

    /** 查询目录内容。 */
    @GetMapping("/spaces/{spaceId}/children")
    @Operation(summary = "查询目录内容")
    public ApiResponse<List<FileNode>> children(@PathVariable String spaceId,
                                                @RequestParam(required = false) String parentId) {
        return ApiResponse.ok(filesService.children(spaceId, parentId));
    }

    /** 查询共享给当前用户或其部门的节点。 */
    @GetMapping("/shared")
    @Operation(summary = "查询共享给我的文件")
    public ApiResponse<List<FileNode>> sharedNodes() {
        return ApiResponse.ok(filesService.sharedNodes());
    }

    /** 创建文件夹。 */
    @PostMapping("/folders")
    @Operation(summary = "创建文件夹")
    public ApiResponse<FileNode> createFolder(@RequestBody FileRequests.CreateFolder request) {
        return ApiResponse.ok(filesService.createFolder(request));
    }

    /** 上传文件或创建同名文件的新版本。 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件")
    public Mono<ApiResponse<FileUploadResultDTO>> upload(
            @RequestParam String spaceId,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String editToken,
            @RequestPart("file") Mono<FilePart> filePart) {
        return filePart.flatMap(file -> filesService.upload(
                spaceId, parentId, file, businessType, businessId, editToken)).map(ApiResponse::ok);
    }

    /** 下载当前版本。 */
    @GetMapping("/{nodeId}")
    @Operation(summary = "下载文件")
    public Mono<ResponseEntity<Resource>> download(@PathVariable String nodeId) {
        return filesService.download(nodeId);
    }

    /** 生成文件短时下载地址。 */
    @GetMapping("/{nodeId}/access-url")
    @Operation(summary = "生成短时下载地址")
    public Mono<ApiResponse<FileAccessUrlDTO>> accessUrl(@PathVariable String nodeId) {
        return filesService.accessUrl(nodeId).map(ApiResponse::ok);
    }

    /** 查询文件历史版本。 */
    @GetMapping("/{nodeId}/versions")
    @Operation(summary = "查询文件版本")
    public ApiResponse<List<FileVersion>> versions(@PathVariable String nodeId) {
        return ApiResponse.ok(filesService.versions(nodeId));
    }

    /** 将历史版本切换为当前版本。 */
    @PostMapping("/{nodeId}/versions/{versionId}/restore")
    @Operation(summary = "恢复历史版本")
    public ApiResponse<FileNode> restoreVersion(@PathVariable String nodeId, @PathVariable String versionId) {
        return ApiResponse.ok(filesService.restoreVersion(nodeId, versionId));
    }

    /** 重命名节点。 */
    @PutMapping("/{nodeId}/name")
    @Operation(summary = "重命名文件或文件夹")
    public ApiResponse<FileNode> rename(@PathVariable String nodeId,
                                        @RequestBody FileRequests.RenameNode request) {
        return ApiResponse.ok(filesService.rename(nodeId, request));
    }

    /** 移动节点。 */
    @PutMapping("/{nodeId}/parent")
    @Operation(summary = "移动文件或文件夹")
    public ApiResponse<FileNode> move(@PathVariable String nodeId,
                                      @RequestBody FileRequests.MoveNode request) {
        return ApiResponse.ok(filesService.move(nodeId, request));
    }

    /** 将节点放入回收站。 */
    @DeleteMapping("/{nodeId}")
    @Operation(summary = "移入回收站")
    public ApiResponse<Boolean> trash(@PathVariable String nodeId) {
        filesService.trash(nodeId);
        return ApiResponse.ok(Boolean.TRUE);
    }

    /** 从回收站恢复节点。 */
    @PostMapping("/{nodeId}/restore")
    @Operation(summary = "从回收站恢复")
    public ApiResponse<Boolean> restore(@PathVariable String nodeId) {
        filesService.restore(nodeId);
        return ApiResponse.ok(Boolean.TRUE);
    }

    /** 查询节点共享授权。 */
    @GetMapping("/{nodeId}/grants")
    @Operation(summary = "查询共享授权")
    public ApiResponse<List<FileGrant>> grants(@PathVariable String nodeId) {
        return ApiResponse.ok(filesService.grants(nodeId));
    }

    /** 创建或更新节点共享授权。 */
    @PostMapping("/{nodeId}/grants")
    @Operation(summary = "创建或更新共享授权")
    public ApiResponse<FileGrant> upsertGrant(@PathVariable String nodeId,
                                              @RequestBody FileRequests.UpsertGrant request) {
        return ApiResponse.ok(filesService.upsertGrant(nodeId, request));
    }

    /** 删除节点共享授权。 */
    @DeleteMapping("/{nodeId}/grants/{grantId}")
    @Operation(summary = "删除共享授权")
    public ApiResponse<Boolean> deleteGrant(@PathVariable String nodeId, @PathVariable String grantId) {
        filesService.deleteGrant(nodeId, grantId);
        return ApiResponse.ok(Boolean.TRUE);
    }

    /** 获取或续期编辑锁。 */
    @PostMapping("/{nodeId}/edit-lock")
    @Operation(summary = "获取编辑锁")
    public ApiResponse<FileViews.EditLock> acquireLock(@PathVariable String nodeId) {
        return ApiResponse.ok(filesService.acquireLock(nodeId));
    }

    /** 释放编辑锁。 */
    @DeleteMapping("/{nodeId}/edit-lock")
    @Operation(summary = "释放编辑锁")
    public ApiResponse<Boolean> releaseLock(@PathVariable String nodeId,
                                            @RequestBody FileRequests.ReleaseLock request) {
        filesService.releaseLock(nodeId, request.token());
        return ApiResponse.ok(Boolean.TRUE);
    }
}
