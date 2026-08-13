package com.ai.api.files.client;

import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.api.files.dto.FileBusinessFileDTO;
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

}
