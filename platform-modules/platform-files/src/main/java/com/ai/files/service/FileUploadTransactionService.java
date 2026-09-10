package com.ai.files.service;

import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileNodeState;
import com.ai.files.domain.FileOperationType;
import com.ai.files.domain.FileRecordStatus;
import com.ai.files.domain.FileNodeType;
import com.ai.files.domain.FileRole;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.FileUploadState;
import com.ai.files.domain.FileVersionState;
import com.ai.files.domain.entity.FileEditLock;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileOperationLog;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.domain.entity.FileUploadRecord;
import com.ai.files.domain.entity.FileVersion;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileNodeRepository;
import com.ai.files.repository.FileOperationLogRepository;
import com.ai.files.repository.FileSpaceRepository;
import com.ai.files.repository.FileUploadRecordRepository;
import com.ai.files.repository.FileVersionRepository;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传 saga 的短事务状态机。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
class FileUploadTransactionService {
    private static final int ERROR_MAX_LENGTH = 2_000;
    private static final Set<FileNodeState> OCCUPYING_NODE_STATES =
            Set.of(FileNodeState.ACTIVE, FileNodeState.UPLOADING);

    private final FileSpaceRepository spaces;
    private final FileNodeRepository nodes;
    private final FileVersionRepository versions;
    private final FileUploadRecordRepository uploads;
    private final FileEditLockRepository locks;
    private final FileOperationLogRepository operationLogs;
    private final FileAccessService access;
    private final FilesProperties properties;
    private final com.ai.files.repository.FileBrowserUploadTargetRepository browserTargets;

    /** 暂存目标的子目录也受约束，不能通过创建子目录绕过单原件限制。 */
    private com.ai.files.domain.entity.FileBrowserUploadTarget browserTarget(FileNode node, String tenantId) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        FileNode cursor = node;
        while (cursor != null) {
            if (!visited.add(cursor.getId())) throw new BizException("文件目录存在循环");
            var target = browserTargets.findLockedByIdAndTenantId(cursor.getId(), tenantId).orElse(null);
            if (target != null) return target;
            cursor = StringUtils.hasText(cursor.getParentId())
                    ? nodes.findByIdAndDeletedFalse(cursor.getParentId()).orElseThrow(() -> new BizException("父目录不存在")) : null;
            if (cursor != null && !tenantId.equals(cursor.getTenantId())) throw new BizException("父目录租户不一致");
        }
        return null;
    }

    /** 锁定空间和节点，原子预留配额、版本号和对象键。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(UploadCommand command) {
        FileSpace space = spaces.findLockedByIdAndTenantId(command.spaceId(), command.tenantId())
                .orElseThrow(() -> new BizException("文件空间不存在"));
        FileNode parent = lockedParent(space, command.parentId(), command.tenantId());
        access.require(space, parent, FileRole.UPLOADER);
        var browserTarget = browserTarget(parent, command.tenantId());
        if (browserTarget != null) {
            if (!browserTarget.getId().equals(command.parentId())
                    || !"OPEN".equals(browserTarget.getStatus()) || !browserTarget.getExpiresAt().isAfter(Instant.now())
                    || !browserTarget.getUploaderUserId().equals(command.userId())
                    || !browserTarget.getBusinessType().equals(command.businessType())
                    || !browserTarget.getBusinessId().equals(command.businessId())
                    || !browserTarget.getFilename().equals(command.originalName())
                    || browserTarget.getSizeBytes() != command.sizeBytes()
                    || !browserTarget.getSha256().equals(command.sha256())) {
                throw new BizException("上传目标已使用、已过期或原件与预约不一致");
            }
        }


        FileNode found = nodes.findBySpaceIdAndParentIdAndNodeNameAndNodeStateInAndDeletedFalse(
                        space.getId(), parentId(parent), command.originalName(), OCCUPYING_NODE_STATES)
                .orElse(null);
        if (browserTarget != null && found != null) {
            throw new BizException("该原件已上传或正在上传，请读取已有结果");
        }
        if (found != null && found.getNodeState() == FileNodeState.UPLOADING) {
            throw new BizException("同名文件正在上传");
        }
        if (found != null && found.getNodeType() != FileNodeType.FILE) {
            throw new BizException("目录中已存在同名文件夹");
        }

        FileNode node = found == null ? newFileNode(space, parent, command) : lockedNode(found, command.tenantId());
        if (StringUtils.hasText(node.getActiveUploadId())) {
            throw new BizException("当前文件已存在进行中的上传");
        }
        verifyEditLock(space, node, command.editToken(), command.userId());

        long used = nonNegative(space.getUsedBytes());
        long reserved = nonNegative(space.getReservedBytes());
        if (used + reserved + command.sizeBytes() > space.getQuotaBytes()) {
            throw new BizException("文件空间剩余配额不足");
        }

        String uploadId = id();
        String versionId = id();
        String executionToken = id();
        int versionNo = versions.findMaxVersionNo(node.getId(), command.tenantId()).orElse(0) + 1;
        String objectKey = "tenants/" + FileChecksum.sha256(command.tenantId()).substring(0, 32)
                + "/nodes/" + node.getId() + "/versions/" + versionId;

        node.setActiveUploadId(uploadId);
        nodes.saveAndFlush(node);

        FileVersion version = new FileVersion();
        version.setId(versionId);
        version.setTenantId(command.tenantId());
        version.setNodeId(node.getId());
        version.setVersionNo(versionNo);
        version.setVersionState(FileVersionState.PENDING);
        version.setObjectKey(objectKey);
        version.setOriginalName(command.originalName());
        version.setContentType(command.contentType());
        version.setSizeBytes(command.sizeBytes());
        version.setSha256(command.sha256());
        versions.saveAndFlush(version);

        Instant now = Instant.now();
        FileUploadRecord upload = new FileUploadRecord();
        upload.setId(uploadId);
        upload.setTenantId(command.tenantId());
        upload.setSpaceId(space.getId());
        upload.setNodeId(node.getId());
        upload.setVersionId(versionId);
        upload.setObjectKey(objectKey);
        upload.setOperatorUserId(command.userId());
        upload.setBusinessType(trimToNull(command.businessType()));
        upload.setBusinessId(trimToNull(command.businessId()));
        upload.setReservedBytes(command.sizeBytes());
        upload.setNewNode(found == null);
        upload.setUploadState(FileUploadState.PREPARED);
        upload.setExecutionToken(executionToken);
        upload.setLeaseExpiresAt(now.plus(properties.resolvedUploadLease()));
        upload.setNextAttemptAt(now);
        upload.setAttemptCount(1);
        uploads.save(upload);

        space.setReservedBytes(reserved + command.sizeBytes());
        spaces.save(space);
        return new Reservation(uploadId, command.tenantId(), executionToken, objectKey);
    }

    /** 记录对象已写入，使后续元数据发布可重入。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markObjectStored(Reservation reservation) {
        FileUploadRecord upload = ownedUpload(reservation);
        if (upload.getUploadState() == FileUploadState.OBJECT_STORED
                || upload.getUploadState() == FileUploadState.COMPLETED) {
            return;
        }
        requireState(upload, FileUploadState.PREPARED);
        upload.setUploadState(FileUploadState.OBJECT_STORED);
        upload.setLastError(null);
        uploads.save(upload);
    }

    /** 在单个短事务中发布版本、切换当前版本并结算配额。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileUploadResultDTO complete(Reservation reservation) {
        FileUploadRecord upload = ownedUpload(reservation);
        if (upload.getUploadState() == FileUploadState.COMPLETED) {
            return result(upload);
        }
        requireState(upload, FileUploadState.OBJECT_STORED);
        FileSpace space = lockedSpace(upload);
        FileNode node = lockedNode(upload.getNodeId(), upload.getTenantId());
        verifyActiveUpload(node, upload);
        FileVersion version = requireVersion(upload);

        version.setVersionState(FileVersionState.AVAILABLE);
        versions.save(version);
        node.setCurrentVersionId(version.getId());
        node.setActiveUploadId(null);
        node.setNodeState(FileNodeState.ACTIVE);
        node.setBusinessType(upload.getBusinessType());
        node.setBusinessId(upload.getBusinessId());
        nodes.save(node);

        var browserTarget = browserTarget(node, upload.getTenantId());
        if (browserTarget != null) {
            if (!"OPEN".equals(browserTarget.getStatus()) || !browserTarget.getId().equals(node.getParentId())
                    || !browserTarget.getUploaderUserId().equals(upload.getOperatorUserId())
                    || !browserTarget.getSha256().equals(version.getSha256())
                    || browserTarget.getSizeBytes() != version.getSizeBytes()) {
                throw new BizException("上传目标在对象保存期间发生变化");
            }
            browserTarget.setFileId(node.getId());browserTarget.setVersionId(version.getId());
            browserTarget.setStatus("UPLOADED");browserTargets.save(browserTarget);
        }
        settleSpace(space, upload.getReservedBytes(), true);
        upload.setUploadState(FileUploadState.COMPLETED);
        upload.setLeaseExpiresAt(Instant.now());
        upload.setLastError(null);
        uploads.save(upload);
        audit(upload, upload.getNewNode() ? FileOperationType.UPLOAD : FileOperationType.CREATE_VERSION,
                "version=" + version.getVersionNo() + ",size=" + version.getSizeBytes());
        return result(upload, version);
    }

    /** 把对象写入失败收敛为可重试清理状态。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCleanupPending(Reservation reservation, Throwable error) {
        FileUploadRecord upload = ownedUpload(reservation);
        if (upload.getUploadState() != FileUploadState.PREPARED) {
            return;
        }
        upload.setUploadState(FileUploadState.CLEANUP_PENDING);
        upload.setNextAttemptAt(Instant.now());
        upload.setLastError(limit(message(error)));
        uploads.save(upload);
    }

    /** 在对象已确认不存在后释放元数据与配额预留。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(Reservation reservation) {
        FileUploadRecord upload = ownedUpload(reservation);
        if (upload.getUploadState() == FileUploadState.CANCELLED) {
            return;
        }
        if (upload.getUploadState() != FileUploadState.CLEANUP_PENDING
                && upload.getUploadState() != FileUploadState.PREPARED) {
            throw new BizException("当前上传状态不允许取消");
        }
        FileSpace space = lockedSpace(upload);
        FileNode node = lockedNode(upload.getNodeId(), upload.getTenantId());
        verifyActiveUpload(node, upload);
        FileVersion version = requireVersion(upload);
        version.setVersionState(FileVersionState.CANCELLED);
        versions.save(version);

        node.setActiveUploadId(null);
        if (Boolean.TRUE.equals(upload.getNewNode())) {
            node.setDeleted(true);
        }
        nodes.save(node);
        settleSpace(space, upload.getReservedBytes(), false);
        upload.setUploadState(FileUploadState.CANCELLED);
        upload.setLeaseExpiresAt(Instant.now());
        uploads.save(upload);
    }

    /** 超时后领取一条未完成记录，并为本次恢复生成新执行令牌。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Recovery claim(String uploadId, String tenantId) {
        FileUploadRecord upload = uploads.findLockedByIdAndTenantId(uploadId, tenantId).orElse(null);
        Instant now = Instant.now();
        if (upload == null || terminal(upload.getUploadState())
                || upload.getLeaseExpiresAt().isAfter(now) || upload.getNextAttemptAt().isAfter(now)) {
            return null;
        }
        String token = id();
        upload.setExecutionToken(token);
        upload.setLeaseExpiresAt(now.plus(properties.resolvedUploadLease()));
        upload.setAttemptCount(upload.getAttemptCount() + 1);
        uploads.save(upload);
        return new Recovery(
                new Reservation(upload.getId(), upload.getTenantId(), token, upload.getObjectKey()),
                upload.getUploadState());
    }

    /** 保留恢复状态并记录下次可重试时间。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRecoveryFailure(Reservation reservation, Throwable error) {
        FileUploadRecord upload = ownedUpload(reservation);
        if (terminal(upload.getUploadState())) {
            return;
        }
        Instant now = Instant.now();
        upload.setLeaseExpiresAt(now);
        upload.setNextAttemptAt(now.plus(properties.resolvedReconcileRetryDelay()));
        upload.setLastError(limit(message(error)));
        uploads.save(upload);
    }

    private FileNode lockedParent(FileSpace space, String parentId, String tenantId) {
        if (!StringUtils.hasText(parentId)) {
            return null;
        }
        FileNode parent = lockedNode(parentId.trim(), tenantId);
        if (!space.getId().equals(parent.getSpaceId()) || parent.getNodeType() != FileNodeType.FOLDER
                || parent.getNodeState() != FileNodeState.ACTIVE) {
            throw new BizException("父节点不是当前空间中的活动文件夹");
        }
        return parent;
    }

    private FileNode lockedNode(FileNode node, String tenantId) {
        return lockedNode(node.getId(), tenantId);
    }

    private FileNode lockedNode(String nodeId, String tenantId) {
        return nodes.findLockedByIdAndTenantId(nodeId, tenantId)
                .orElseThrow(() -> new BizException("文件节点不存在"));
    }

    private FileSpace lockedSpace(FileUploadRecord upload) {
        return spaces.findLockedByIdAndTenantId(upload.getSpaceId(), upload.getTenantId())
                .orElseThrow(() -> new BizException("文件空间不存在"));
    }

    private FileNode newFileNode(FileSpace space, FileNode parent, UploadCommand command) {
        FileNode node = new FileNode();
        node.setId(id());
        node.setTenantId(command.tenantId());
        node.setSpaceId(space.getId());
        node.setParentId(parentId(parent));
        node.setNodeType(FileNodeType.FILE);
        node.setNodeName(command.originalName());
        node.setDisplayPath(parent == null
                ? "/" + command.originalName()
                : parent.getDisplayPath() + "/" + command.originalName());
        node.setNodeState(FileNodeState.UPLOADING);
        node.setStatus(FileRecordStatus.ACTIVE.name());
        return node;
    }

    private void verifyEditLock(FileSpace space, FileNode node, String editToken, String userId) {
        if (node.getNodeState() == FileNodeState.UPLOADING || space.getSpaceType() == FileSpaceType.SYSTEM) {
            return;
        }
        locks.findByNodeIdAndDeletedFalse(node.getId()).ifPresent(lock -> verifyEditLock(lock, editToken, userId));
    }

    private void verifyEditLock(FileEditLock lock, String editToken, String userId) {
        if (!lock.getExpiresAt().isAfter(Instant.now())) {
            locks.delete(lock);
            return;
        }
        boolean tokenMatches = StringUtils.hasText(editToken)
                && FileChecksum.sha256(editToken.trim()).equals(lock.getTokenHash());
        if (!userId.equals(lock.getOwnerUserId()) || !tokenMatches) {
            throw new BizException("文件存在有效编辑锁，必须由锁持有人提交新版本");
        }
    }

    private FileUploadRecord ownedUpload(Reservation reservation) {
        FileUploadRecord upload = uploads
                .findLockedByIdAndTenantId(reservation.uploadId(), reservation.tenantId())
                .orElseThrow(() -> new BizException("文件上传记录不存在"));
        if (!Objects.equals(upload.getExecutionToken(), reservation.executionToken())) {
            throw new BizException("文件上传执行权已变更");
        }
        return upload;
    }

    private FileVersion requireVersion(FileUploadRecord upload) {
        return versions.findByIdAndTenantIdAndDeletedFalse(upload.getVersionId(), upload.getTenantId())
                .orElseThrow(() -> new BizException("文件上传版本不存在"));
    }

    private void verifyActiveUpload(FileNode node, FileUploadRecord upload) {
        if (!Objects.equals(node.getActiveUploadId(), upload.getId())) {
            throw new BizException("文件节点的活动上传与记录不一致");
        }
    }

    private void settleSpace(FileSpace space, long bytes, boolean completed) {
        long reserved = nonNegative(space.getReservedBytes());
        if (reserved < bytes) {
            throw new BizException("文件空间预留容量不足");
        }
        space.setReservedBytes(reserved - bytes);
        if (completed) {
            space.setUsedBytes(nonNegative(space.getUsedBytes()) + bytes);
        }
        spaces.save(space);
    }

    private void requireState(FileUploadRecord upload, FileUploadState expected) {
        if (upload.getUploadState() != expected) {
            throw new BizException("文件上传状态不允许当前操作");
        }
    }

    private FileUploadResultDTO result(FileUploadRecord upload) {
        return result(upload, requireVersion(upload));
    }

    private FileUploadResultDTO result(FileUploadRecord upload, FileVersion version) {
        return new FileUploadResultDTO(
                upload.getNodeId(), version.getId(), version.getOriginalName(), upload.getObjectKey(),
                version.getContentType(), version.getSizeBytes(),
                "/files/" + upload.getNodeId(), version.getSha256());
    }

    private void audit(FileUploadRecord upload, FileOperationType operation, String detail) {
        FileOperationLog log = new FileOperationLog();
        log.setId(id());
        log.setTenantId(upload.getTenantId());
        log.setOperatorUserId(upload.getOperatorUserId());
        log.setOperation(operation);
        log.setSpaceId(upload.getSpaceId());
        log.setNodeId(upload.getNodeId());
        log.setDetail(detail);
        log.setStatus(FileRecordStatus.SUCCESS.name());
        operationLogs.save(log);
    }

    private boolean terminal(FileUploadState state) {
        return state == FileUploadState.COMPLETED || state == FileUploadState.CANCELLED;
    }

    private long nonNegative(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private String parentId(FileNode parent) {
        return parent == null ? null : parent.getId();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String message(Throwable error) {
        return StringUtils.hasText(error.getMessage()) ? error.getMessage() : error.getClass().getSimpleName();
    }

    private String limit(String value) {
        return value.length() <= ERROR_MAX_LENGTH ? value : value.substring(0, ERROR_MAX_LENGTH);
    }

    private static String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    record UploadCommand(
            String tenantId,
            String userId,
            String spaceId,
            String parentId,
            String originalName,
            String contentType,
            long sizeBytes,
            String sha256,
            String businessType,
            String businessId,
            String editToken
    ) {
    }

    record Reservation(String uploadId, String tenantId, String executionToken, String objectKey) {
    }

    record Recovery(Reservation reservation, FileUploadState state) {
    }
}
