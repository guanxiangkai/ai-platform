package com.ai.files.domain.dto;

import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileRole;

import java.time.Instant;

/** 平台文件写操作请求契约。 */
public final class FileRequests {

    private FileRequests() {
    }

    /** 创建或获取部门空间的请求。 */
    public record DepartmentSpace(String deptId, String spaceName, Long quotaBytes) {
    }

    /** 创建文件夹的请求。 */
    public record CreateFolder(String spaceId, String parentId, String nodeName) {
    }

    /** 重命名节点的请求。 */
    public record RenameNode(String nodeName) {
    }

    /** 移动节点的请求。 */
    public record MoveNode(String parentId) {
    }

    /** 创建或更新共享授权的请求。 */
    public record UpsertGrant(
            FilePrincipalType principalType,
            String principalId,
            FileRole role,
            Boolean inherited,
            Instant expiresAt
    ) {
    }

    /** 释放编辑锁的请求。 */
    public record ReleaseLock(String token) {
    }
}
