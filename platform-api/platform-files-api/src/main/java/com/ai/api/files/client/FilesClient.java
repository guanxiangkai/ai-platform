package com.ai.api.files.client;

import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileBusinessRootDTO;
import com.ai.api.files.dto.FileBusinessRootNodePageDTO;
import com.ai.api.files.dto.FileBusinessUploadDTO;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 平台文件服务内部客户端。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface FilesClient {

    /** 读取浏览器直传策略，不提供存储凭据。 */
    com.ai.api.files.dto.FileBrowserUploadPolicyDTO browserUploadPolicy();

    /** 为一个确定原件创建或续期当前租户的用户上传目标。 */
    com.ai.api.files.dto.FileBrowserUploadTargetDTO prepareBrowserUploadTarget(com.ai.api.files.dto.FileBrowserUploadTargetRequestDTO request);

    /** 校验上传主体及不可变版本，并撤销浏览器写入权限。 */
    com.ai.api.files.dto.FileBusinessUploadDTO acceptBrowserUpload(com.ai.api.files.dto.FileBrowserUploadAcceptRequestDTO request);


    /**
     * 上传文件。
     *
     * @param resource 文件资源
     * @param filename 原始文件名
     * @param contentType 内容类型
     * @param bizType 业务类型
     * @param bizId 业务主键
     * @return 文件上传结果
     */
    FileUploadResultDTO upload(Resource resource, String filename, String contentType, String bizType, String bizId);

    /** 幂等获取当前租户业务记录的稳定文件根目录。 */
    FileBusinessRootDTO ensureBusinessRoot(String businessType, String businessId, String displayName);

    /** 将文件上传到业务根目录内由服务端解析的相对目录。 */
    FileBusinessUploadDTO uploadToBusinessRoot(Resource resource, String filename, String contentType,
                                                String rootBusinessType, String rootBusinessId,
                                                String relativePath, String bizType, String bizId);

    /** 分页读取业务根目录内一个父节点的直属节点。 */
    FileBusinessRootNodePageDTO listBusinessRootNodes(String businessType, String businessId,
                                                       String parentId, String nodeType, int page, int size);

    /** 查询文件指定版本或当前版本在业务根目录中的真实位置。 */
    FileBusinessUploadDTO fileMetadata(String fileId, String versionId);

    /** 将既有文件节点放入已存在业务根目录内的完整相对路径。 */
    FileBusinessUploadDTO placeBusinessFile(String rootBusinessType, String rootBusinessId, String fileId,
                                            String expectedCurrentVersionId, String relativePath);

    /**
     * 查询当前租户指定业务记录关联的活动文件。
     *
     * @param bizType 业务类型
     * @param bizId 业务记录标识
     * @return 活动业务文件
     */
    List<FileBusinessFileDTO> activeBusinessFiles(String bizType, String bizId);

    /**
     * 幂等回收当前租户指定业务记录关联的活动文件。
     *
     * <p>回收只将文件移入回收站，保留文件版本和审计记录以支持恢复。</p>
     *
     * @param bizType 业务类型
     * @param bizId 业务记录标识
     */
    void recycleBusinessFiles(String bizType, String bizId);

    /**
     * 从文件服务流式下载文件。
     *
     * <p>响应头和响应体均由平台文件服务返回，调用方不得将内部文件地址重定向给浏览器。</p>
     *
     * @param fileId 文件标识
     * @return 包含文件响应头与流式内容的异步响应
     */
    Mono<ResponseEntity<Flux<DataBuffer>>> download(String fileId);

    /** 下载指定文件节点的一个可用历史版本。 */
    Mono<ResponseEntity<Flux<DataBuffer>>> downloadVersion(String fileId, String versionId);

}
