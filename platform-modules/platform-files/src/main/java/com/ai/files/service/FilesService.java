package com.ai.files.service;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.api.files.dto.FileAccessUrlDTO;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileUploadResultDTO;
import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileNodeState;
import com.ai.files.domain.FileOperationType;
import com.ai.files.domain.FileRecordStatus;
import com.ai.files.domain.FileNodeType;
import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileRole;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.FileVersionState;
import com.ai.files.domain.dto.FileRequests;
import com.ai.files.domain.entity.FileEditLock;
import com.ai.files.domain.entity.FileGrant;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileOperationLog;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.domain.entity.FileVersion;
import com.ai.files.domain.vo.FileViews;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileGrantRepository;
import com.ai.files.repository.FileNodeRepository;
import com.ai.files.repository.FileOperationLogRepository;
import com.ai.files.repository.FileSpaceRepository;
import com.ai.files.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 平台文件空间、版本、共享、回收站和编辑锁的统一领域服务。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class FilesService {

    private static final Pattern SAFE_SUFFIX_PATTERN = Pattern.compile("\\.[A-Za-z0-9]{1,12}");

    private final FileSpaceRepository spaceRepository;
    private final FileNodeRepository nodeRepository;
    private final FileVersionRepository versionRepository;
    private final FileGrantRepository grantRepository;
    private final FileEditLockRepository lockRepository;
    private final FileOperationLogRepository operationLogRepository;
    private final FileAccessService accessService;
    private final FileObjectStorage objectStorage;
    private final FileUploadService uploadService;
    private final FilesProperties properties;
    private final TransactionTemplate transactionTemplate;

    /**
     * 获取当前用户的个人空间，不存在时自动创建。
     *
     * @return 个人文件空间
     */
    public FileSpace personalSpace() {
        String userId = requireUserId();
        return Objects.requireNonNull(transactionTemplate.execute(status -> spaceRepository
                .findBySpaceTypeAndOwnerUserIdAndDeletedFalse(FileSpaceType.PERSONAL, userId)
                .orElseGet(() -> createSpace(
                        "personal-" + FileChecksum.sha256(userId).substring(0, 24),
                        "我的文件", FileSpaceType.PERSONAL, userId, null,
                        properties.personalQuotaBytes()))));
    }

    /**
     * 获取或创建当前用户所属部门的空间。
     *
     * @param request 部门空间参数
     * @return 部门文件空间
     */
    public FileSpace departmentSpace(FileRequests.DepartmentSpace request) {
        String deptId = requireText(request.deptId(), "部门标识不能为空");
        Set<String> currentDeptIds = SecurityUtils.getDeptIds();
        if (!SecurityUtils.isSuperAdmin() && !currentDeptIds.contains(deptId)) {
            throw new BizException("只能创建当前用户所属部门的文件空间");
        }
        return Objects.requireNonNull(transactionTemplate.execute(status -> spaceRepository
                .findBySpaceTypeAndOwnerDeptIdAndDeletedFalse(FileSpaceType.DEPARTMENT, deptId)
                .orElseGet(() -> createSpace(
                        "department-" + FileChecksum.sha256(deptId).substring(0, 24),
                        firstText(request.spaceName(), "部门文件"), FileSpaceType.DEPARTMENT,
                        null, deptId, positive(request.quotaBytes(), properties.departmentQuotaBytes())))));
    }

    /**
     * 返回当前用户可访问的全部空间。
     *
     * @return 去重后的空间列表
     */
    public List<FileSpace> accessibleSpaces() {
        Map<String, FileSpace> spaces = new LinkedHashMap<>();
        FileSpace personalSpace = personalSpace();
        spaces.put(personalSpace.getId(), personalSpace);
        String userId = requireUserId();
        Set<String> currentDeptIds = SecurityUtils.getDeptIds();
        boolean hasDepartments = !currentDeptIds.isEmpty();
        List<String> departmentIds = hasDepartments ? List.copyOf(currentDeptIds) : List.of("");
        for (FileSpace space : spaceRepository.findAccessibleSpaces(
                userId,
                departmentIds,
                hasDepartments,
                Instant.now(),
                SecurityUtils.isSuperAdmin(),
                accessService.isInternalService(),
                FileSpaceType.DEPARTMENT,
                FileSpaceType.SYSTEM,
                FilePrincipalType.USER,
                FilePrincipalType.DEPARTMENT)) {
            spaces.put(space.getId(), space);
        }
        return List.copyOf(spaces.values());
    }

    /**
     * 返回空间配额使用情况。
     *
     * @param spaceId 空间标识
     * @return 配额使用情况
     */
    public FileViews.SpaceUsage usage(String spaceId) {
        FileSpace space = requireSpace(spaceId);
        accessService.require(space, null, FileRole.VIEWER);
        long used = positive(space.getUsedBytes(), 0L);
        long quota = positive(space.getQuotaBytes(), 0L);
        return new FileViews.SpaceUsage(space.getId(), quota, used, Math.max(0, quota - used));
    }

    /**
     * 创建文件夹。
     *
     * @param request 文件夹参数
     * @return 新文件夹
     */
    public FileNode createFolder(FileRequests.CreateFolder request) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileSpace space = requireSpace(request.spaceId());
            FileNode parent = resolveParent(space, request.parentId());
            accessService.require(space, parent, FileRole.EDITOR);
            String name = safeName(request.nodeName());
            ensureNameAvailable(space.getId(), parentId(parent), name, null);
            FileNode folder = newNode(space, parent, name, FileNodeType.FOLDER);
            FileNode saved = nodeRepository.save(folder);
            audit(FileOperationType.CREATE_FOLDER, space.getId(), saved.getId(), saved.getDisplayPath());
            return saved;
        }));
    }

    /**
     * 查询目录中的活动节点。
     *
     * @param spaceId 空间标识
     * @param parentId 父文件夹标识；根目录为空
     * @return 节点列表
     */
    public List<FileNode> children(String spaceId, String parentId) {
        FileSpace space = requireSpace(spaceId);
        FileNode parent = resolveParent(space, parentId);
        accessService.require(space, parent, FileRole.VIEWER);
        return nodeRepository.findBySpaceIdAndParentIdAndNodeStateAndDeletedFalseOrderByNodeNameAsc(
                space.getId(), parentId(parent), FileNodeState.ACTIVE);
    }

    /**
     * 返回通过用户或部门授权共享给当前用户的活动节点。
     *
     * @return 去重后的共享节点列表
     */
    public List<FileNode> sharedNodes() {
        String userId = requireUserId();
        Set<String> deptIds = SecurityUtils.getDeptIds();
        List<FileGrant> grants = new ArrayList<>(grantRepository
                .findByPrincipalTypeAndPrincipalIdInAndDeletedFalse(
                        FilePrincipalType.USER, List.of(userId)));
        if (!deptIds.isEmpty()) {
            grants.addAll(grantRepository.findByPrincipalTypeAndPrincipalIdInAndDeletedFalse(
                    FilePrincipalType.DEPARTMENT, deptIds));
        }
        Instant now = Instant.now();
        Map<String, FileNode> nodes = new LinkedHashMap<>();
        for (FileGrant grant : grants) {
            if (!StringUtils.hasText(grant.getNodeId())
                    || (grant.getExpiresAt() != null && !grant.getExpiresAt().isAfter(now))) {
                continue;
            }
            nodeRepository.findByIdAndDeletedFalse(grant.getNodeId())
                    .filter(node -> node.getNodeState() == FileNodeState.ACTIVE)
                    .filter(node -> accessService.role(requireSpace(node.getSpaceId()), node) != null)
                    .ifPresent(node -> nodes.put(node.getId(), node));
        }
        return List.copyOf(nodes.values());
    }

    /**
     * 上传文件；同目录同名文件会创建新版本，文件夹重名会被拒绝。
     *
     * @param spaceId 空间标识
     * @param parentId 父文件夹标识
     * @param filePart 上传文件
     * @param businessType 来源业务类型
     * @param businessId 来源业务标识
     * @param editToken 已存在编辑锁时必须提供的锁令牌
     * @return 上传结果
     */
    public Mono<FileUploadResultDTO> upload(String spaceId, String parentId, FilePart filePart,
                                            String businessType, String businessId, String editToken) {
        if (filePart == null) {
            return Mono.error(new BizException("上传文件不能为空"));
        }
        String originalName = safeName(filePart.filename());
        String contentType = filePart.headers().getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : filePart.headers().getContentType().toString();
        Mono<Path> temporaryFile = Mono.fromCallable(() ->
                        Files.createTempFile("ai-platform-files-", safeSuffix(originalName)))
                .subscribeOn(Schedulers.boundedElastic());
        return Mono.usingWhen(
                temporaryFile,
                path -> filePart.transferTo(path)
                        .then(Mono.fromCallable(() -> upload(path, spaceId, parentId, originalName,
                                        contentType, businessType, businessId, editToken))
                                .subscribeOn(Schedulers.boundedElastic())),
                this::deleteTemporaryFile,
                (path, error) -> deleteTemporaryFile(path),
                this::deleteTemporaryFile
        );
    }

    /**
     * 下载当前文件版本。
     *
     * @param nodeId 文件节点标识
     * @return 文件流
     */
    public Mono<ResponseEntity<Resource>> download(String nodeId) {
        FileContext context = readableFile(nodeId);
        auditInTransaction(FileOperationType.DOWNLOAD, context.space().getId(), context.node().getId(), null);
        return Mono.fromCallable(() -> objectStorage.download(context.version()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 生成当前文件版本的短时下载地址。
     *
     * @param nodeId 文件节点标识
     * @return 预签名地址
     */
    public Mono<FileAccessUrlDTO> accessUrl(String nodeId) {
        FileContext context = readableFile(nodeId);
        auditInTransaction(FileOperationType.CREATE_ACCESS_URL, context.space().getId(), context.node().getId(), null);
        return Mono.fromCallable(() -> objectStorage.accessUrl(context.version()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询文件的全部历史版本。
     *
     * @param nodeId 文件节点标识
     * @return 按版本号倒序排列的版本列表
     */
    public List<FileVersion> versions(String nodeId) {
        FileContext context = readableFile(nodeId);
        return versionRepository.findByNodeIdAndVersionStateAndDeletedFalseOrderByVersionNoDesc(
                context.node().getId(), FileVersionState.AVAILABLE);
    }

    /**
     * 将指定历史版本设为当前版本，内容本身保持不可变。
     *
     * @param nodeId 文件节点标识
     * @param versionId 历史版本标识
     * @return 更新后的文件节点
     */
    public FileNode restoreVersion(String nodeId, String versionId) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileNode node = requireNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.EDITOR);
            FileVersion version = versionRepository.findById(versionId)
                    .filter(item -> node.getId().equals(item.getNodeId())
                            && !Boolean.TRUE.equals(item.getDeleted())
                            && item.getVersionState() == FileVersionState.AVAILABLE)
                    .orElseThrow(() -> new BizException("文件版本不存在"));
            node.setCurrentVersionId(version.getId());
            FileNode saved = nodeRepository.save(node);
            audit(FileOperationType.RESTORE_VERSION, space.getId(), node.getId(), "version=" + version.getVersionNo());
            return saved;
        }));
    }

    /**
     * 重命名文件或文件夹，并同步后代展示路径。
     *
     * @param nodeId 节点标识
     * @param request 新名称
     * @return 更新后的节点
     */
    public FileNode rename(String nodeId, FileRequests.RenameNode request) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileNode node = requireActiveNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.EDITOR);
            String newName = safeName(request.nodeName());
            ensureNameAvailable(space.getId(), node.getParentId(), newName, node.getId());
            String oldPath = node.getDisplayPath();
            FileNode parent = resolveParent(space, node.getParentId());
            node.setNodeName(newName);
            node.setDisplayPath(displayPath(parent, newName));
            updateDescendantPaths(node, oldPath);
            FileNode saved = nodeRepository.save(node);
            audit(FileOperationType.RENAME, space.getId(), node.getId(), oldPath + " -> " + node.getDisplayPath());
            return saved;
        }));
    }

    /**
     * 移动文件或文件夹，并同步后代展示路径。
     *
     * @param nodeId 节点标识
     * @param request 目标父目录
     * @return 更新后的节点
     */
    public FileNode move(String nodeId, FileRequests.MoveNode request) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileNode node = requireActiveNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            FileNode target = resolveParent(space, request.parentId());
            accessService.require(space, node, FileRole.EDITOR);
            accessService.require(space, target, FileRole.EDITOR);
            if (target != null && (target.getId().equals(node.getId())
                    || target.getDisplayPath().startsWith(node.getDisplayPath() + "/"))) {
                throw new BizException("文件夹不能移动到自身或其子目录");
            }
            ensureNameAvailable(space.getId(), parentId(target), node.getNodeName(), node.getId());
            String oldPath = node.getDisplayPath();
            node.setParentId(parentId(target));
            node.setDisplayPath(displayPath(target, node.getNodeName()));
            updateDescendantPaths(node, oldPath);
            FileNode saved = nodeRepository.save(node);
            audit(FileOperationType.MOVE, space.getId(), node.getId(), oldPath + " -> " + node.getDisplayPath());
            return saved;
        }));
    }

    /**
     * 将节点及其后代放入回收站。
     *
     * @param nodeId 节点标识
     */
    public void trash(String nodeId) {
        transactionTemplate.executeWithoutResult(status -> {
            FileNode node = requireActiveNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.EDITOR);
            Instant now = Instant.now();
            String userId = requireUserId();
            List<FileNode> nodes = subtree(node);
            nodes.forEach(item -> {
                item.setNodeState(FileNodeState.TRASHED);
                item.setTrashedAt(now);
                item.setTrashedBy(userId);
            });
            nodeRepository.saveAll(nodes);
            audit(FileOperationType.TRASH, space.getId(), node.getId(), node.getDisplayPath());
        });
    }

    /**
     * 从回收站恢复节点及其后代。
     *
     * @param nodeId 节点标识
     */
    public void restore(String nodeId) {
        transactionTemplate.executeWithoutResult(status -> {
            FileNode node = requireNode(nodeId);
            if (node.getNodeState() != FileNodeState.TRASHED) {
                throw new BizException("文件节点不在回收站");
            }
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.EDITOR);
            if (StringUtils.hasText(node.getParentId())) {
                FileNode parent = requireActiveNode(node.getParentId());
                if (!space.getId().equals(parent.getSpaceId())) {
                    throw new BizException("原父目录不可用");
                }
            }
            ensureNameAvailable(space.getId(), node.getParentId(), node.getNodeName(), node.getId());
            List<FileNode> nodes = subtree(node);
            nodes.forEach(item -> {
                item.setNodeState(FileNodeState.ACTIVE);
                item.setTrashedAt(null);
                item.setTrashedBy(null);
            });
            nodeRepository.saveAll(nodes);
            audit(FileOperationType.RESTORE, space.getId(), node.getId(), node.getDisplayPath());
        });
    }

    /**
     * 查询节点的共享授权清单。
     *
     * @param nodeId 节点标识
     * @return 授权列表
     */
    public List<FileGrant> grants(String nodeId) {
        FileNode node = requireNode(nodeId);
        FileSpace space = requireSpace(node.getSpaceId());
        accessService.require(space, node, FileRole.MANAGER);
        return grantRepository.findBySpaceIdAndNodeIdAndDeletedFalseOrderByCreateTimeAsc(space.getId(), node.getId());
    }

    /**
     * 创建或更新节点共享授权。
     *
     * @param nodeId 节点标识
     * @param request 授权参数
     * @return 保存后的授权
     */
    public FileGrant upsertGrant(String nodeId, FileRequests.UpsertGrant request) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileNode node = requireNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.MANAGER);
            if (request.principalType() == null || request.role() == null) {
                throw new BizException("授权主体类型和授权角色不能为空");
            }
            FilePrincipalType principalType = request.principalType();
            FileRole role = request.role();
            String principalId = requireText(request.principalId(), "授权主体标识不能为空");
            FileGrant grant = grantRepository
                    .findBySpaceIdAndNodeIdAndPrincipalTypeAndPrincipalIdAndDeletedFalse(
                            space.getId(), node.getId(), principalType, principalId)
                    .orElseGet(() -> {
                        FileGrant created = new FileGrant();
                        created.setId(id());
                        created.setSpaceId(space.getId());
                        created.setNodeId(node.getId());
                        created.setPrincipalType(principalType);
                        created.setPrincipalId(principalId);
                        return created;
                    });
            grant.setGrantRole(role);
            grant.setInherited(request.inherited() == null || request.inherited());
            grant.setExpiresAt(request.expiresAt());
            FileGrant saved = grantRepository.save(grant);
            audit(FileOperationType.UPSERT_GRANT, space.getId(), node.getId(), principalType + ":" + principalId + ":" + role);
            return saved;
        }));
    }

    /**
     * 删除节点共享授权。
     *
     * @param nodeId 节点标识
     * @param grantId 授权标识
     */
    public void deleteGrant(String nodeId, String grantId) {
        transactionTemplate.executeWithoutResult(status -> {
            FileNode node = requireNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.MANAGER);
            FileGrant grant = grantRepository.findById(grantId)
                    .filter(item -> node.getId().equals(item.getNodeId()) && !Boolean.TRUE.equals(item.getDeleted()))
                    .orElseThrow(() -> new BizException("共享授权不存在"));
            grantRepository.delete(grant);
            audit(FileOperationType.DELETE_GRANT, space.getId(), node.getId(), "grant=" + grantId);
        });
    }

    /**
     * 获取或续期当前用户的编辑锁。
     *
     * @param nodeId 文件节点标识
     * @return 仅本次返回的明文编辑令牌
     */
    public FileViews.EditLock acquireLock(String nodeId) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileNode node = requireActiveNode(nodeId);
            if (node.getNodeType() != FileNodeType.FILE) {
                throw new BizException("只有文件可以获取编辑锁");
            }
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.EDITOR);
            String userId = requireUserId();
            Instant now = Instant.now();
            FileEditLock lock = lockRepository.findByNodeIdAndDeletedFalse(node.getId())
                    .orElseGet(() -> {
                        FileEditLock created = new FileEditLock();
                        created.setId(id());
                        created.setNodeId(node.getId());
                        return created;
                    });
            if (lock.getExpiresAt() != null && lock.getExpiresAt().isAfter(now)
                    && !userId.equals(lock.getOwnerUserId())) {
                throw new BizException("文件正在被其他用户编辑");
            }
            String token = UUID.randomUUID().toString();
            Instant expiresAt = now.plus(properties.resolvedEditLockTtl());
            lock.setOwnerUserId(userId);
            lock.setTokenHash(FileChecksum.sha256(token));
            lock.setExpiresAt(expiresAt);
            lockRepository.save(lock);
            audit(FileOperationType.ACQUIRE_LOCK, space.getId(), node.getId(), "expiresAt=" + expiresAt);
            return new FileViews.EditLock(node.getId(), token, expiresAt);
        }));
    }

    /**
     * 使用明文编辑令牌释放当前用户的编辑锁。
     *
     * @param nodeId 文件节点标识
     * @param token 编辑锁令牌
     */
    public void releaseLock(String nodeId, String token) {
        transactionTemplate.executeWithoutResult(status -> {
            FileNode node = requireNode(nodeId);
            FileSpace space = requireSpace(node.getSpaceId());
            accessService.require(space, node, FileRole.EDITOR);
            FileEditLock lock = lockRepository.findByNodeIdAndDeletedFalse(node.getId())
                    .orElseThrow(() -> new BizException("文件编辑锁不存在"));
            if (!requireUserId().equals(lock.getOwnerUserId())
                    || !FileChecksum.sha256(requireText(token, "编辑锁令牌不能为空"))
                            .equals(lock.getTokenHash())) {
                throw new BizException("编辑锁令牌无效");
            }
            lockRepository.delete(lock);
            audit(FileOperationType.RELEASE_LOCK, space.getId(), node.getId(), null);
        });
    }

    /**
     * 上传可信内部服务的业务附件。
     *
     * @param filePart 上传文件
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @return 上传结果
     */
    public Mono<FileUploadResultDTO> internalUpload(FilePart filePart, String businessType, String businessId) {
        if (!accessService.isInternalService()) {
            return Mono.error(new BizException("仅可信内部服务可以调用该接口"));
        }
        FileSpace systemSpace = systemSpace();
        FileNode businessFolder = systemBusinessFolder(systemSpace, businessType, businessId);
        return upload(systemSpace.getId(), businessFolder.getId(), filePart, businessType, businessId, null);
    }

    /**
     * 下载可信内部服务的业务附件。
     *
     * @param nodeId 文件节点标识
     * @return 文件流
     */
    public Mono<ResponseEntity<Resource>> internalDownload(String nodeId) {
        if (!accessService.isInternalService()) {
            return Mono.error(new BizException("仅可信内部服务可以调用该接口"));
        }
        return download(nodeId);
    }

    /**
     * 查询当前租户业务记录关联的活动文件。
     *
     * @param businessType 业务类型
     * @param businessId 业务记录标识
     * @return 活动业务文件
     */
    public List<FileBusinessFileDTO> internalActiveBusinessFiles(String businessType, String businessId) {
        requireInternalService();
        return activeBusinessFiles(businessType, businessId).stream()
                .map(node -> new FileBusinessFileDTO(node.getId(), node.getNodeName()))
                .toList();
    }

    /**
     * 幂等回收当前租户业务记录关联的活动文件。
     *
     * <p>只改变文件节点状态，不删除文件版本或对象内容，以保留审计和恢复能力。</p>
     *
     * @param businessType 业务类型
     * @param businessId 业务记录标识
     */
    public void internalRecycleBusinessFiles(String businessType, String businessId) {
        requireInternalService();
        String type = requireText(businessType, "业务类型不能为空");
        String id = requireText(businessId, "业务标识不能为空");
        transactionTemplate.executeWithoutResult(status -> {
            List<FileNode> nodes = activeBusinessFiles(type, id);
            if (nodes.isEmpty()) {
                return;
            }
            Instant now = Instant.now();
            String userId = requireUserId();
            nodes.forEach(node -> {
                node.setNodeState(FileNodeState.TRASHED);
                node.setTrashedAt(now);
                node.setTrashedBy(userId);
            });
            nodeRepository.saveAll(nodes);
            nodes.forEach(node -> audit(FileOperationType.TRASH, node.getSpaceId(), node.getId(), "内部业务附件回收"));
        });
    }

    private FileUploadResultDTO upload(Path path, String spaceId, String parentId, String originalName,
                                       String contentType, String businessType, String businessId,
                                       String editToken) throws IOException {
        long size = Files.size(path);
        if (size <= 0) {
            throw new BizException("上传文件不能为空");
        }
        if (size > properties.resolvedMaxFileSizeBytes()) {
            throw new BizException("上传文件超过单文件大小限制");
        }
        return uploadService.upload(path, new FileUploadTransactionService.UploadCommand(
                requireTenantId(), requireUserId(), requireText(spaceId, "空间标识不能为空"),
                trimToNull(parentId), originalName, contentType, size, FileChecksum.sha256(path),
                businessType, businessId, editToken));
    }

    private List<FileNode> activeBusinessFiles(String businessType, String businessId) {
        return nodeRepository
                .findByTenantIdAndBusinessTypeAndBusinessIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByCreateTimeDesc(
                        requireTenantId(),
                        requireText(businessType, "业务类型不能为空"),
                        requireText(businessId, "业务标识不能为空"),
                        FileNodeType.FILE,
                        FileNodeState.ACTIVE);
    }

    private void requireInternalService() {
        if (!accessService.isInternalService()) {
            throw new BizException("仅可信内部服务可以调用该接口");
        }
    }

    private FileContext readableFile(String nodeId) {
        FileNode node = requireActiveNode(nodeId);
        if (node.getNodeType() != FileNodeType.FILE) {
            throw new BizException("目标节点不是文件");
        }
        FileSpace space = requireSpace(node.getSpaceId());
        accessService.require(space, node, FileRole.VIEWER);
        FileVersion version = versionRepository.findById(node.getCurrentVersionId())
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted())
                        && item.getVersionState() == FileVersionState.AVAILABLE)
                .orElseThrow(() -> new BizException("当前文件版本不存在"));
        return new FileContext(space, node, version);
    }

    private FileSpace systemSpace() {
        if (!accessService.isInternalService()) {
            throw new BizException("仅可信内部服务可以访问系统文件空间");
        }
        return Objects.requireNonNull(transactionTemplate.execute(status -> spaceRepository
                .findBySpaceCodeAndDeletedFalse("system")
                .orElseGet(() -> createSpace("system", "业务附件", FileSpaceType.SYSTEM,
                        null, null, properties.systemQuotaBytes()))));
    }

    private FileNode systemBusinessFolder(FileSpace space, String businessType, String businessId) {
        String typeScope = firstText(businessType, "general");
        String idScope = firstText(businessId, id());
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileNode typeFolder = getOrCreateSystemFolder(space, null,
                    "type-" + FileChecksum.sha256(typeScope).substring(0, 24));
            return getOrCreateSystemFolder(space, typeFolder,
                    "item-" + FileChecksum.sha256(idScope).substring(0, 24));
        }));
    }

    private FileNode getOrCreateSystemFolder(FileSpace space, FileNode parent, String name) {
        return nodeRepository.findBySpaceIdAndParentIdAndNodeNameAndNodeStateAndDeletedFalse(
                        space.getId(), parentId(parent), name, FileNodeState.ACTIVE)
                .map(node -> {
                    if (node.getNodeType() != FileNodeType.FOLDER) {
                        throw new BizException("内部业务目录名称冲突");
                    }
                    return node;
                })
                .orElseGet(() -> nodeRepository.save(newNode(space, parent, name, FileNodeType.FOLDER)));
    }

    private FileSpace createSpace(String code, String name, FileSpaceType type,
                                  String ownerUserId, String ownerDeptId, long quotaBytes) {
        FileSpace space = new FileSpace();
        space.setId(id());
        space.setSpaceCode(code);
        space.setSpaceName(name);
        space.setSpaceType(type);
        space.setOwnerUserId(ownerUserId);
        space.setOwnerDeptId(ownerDeptId);
        space.setQuotaBytes(quotaBytes);
        space.setUsedBytes(0L);
        space.setStatus(FileRecordStatus.ACTIVE.name());
        FileSpace saved = spaceRepository.save(space);
        audit(FileOperationType.CREATE_SPACE, saved.getId(), null, type.name());
        return saved;
    }

    private FileNode newNode(FileSpace space, FileNode parent, String name, FileNodeType type) {
        FileNode node = new FileNode();
        node.setId(id());
        node.setSpaceId(space.getId());
        node.setParentId(parentId(parent));
        node.setNodeType(type);
        node.setNodeName(name);
        node.setDisplayPath(displayPath(parent, name));
        node.setNodeState(FileNodeState.ACTIVE);
        node.setStatus(FileRecordStatus.ACTIVE.name());
        return node;
    }

    private void updateDescendantPaths(FileNode node, String oldPath) {
        if (node.getNodeType() != FileNodeType.FOLDER) {
            return;
        }
        List<FileNode> descendants = nodeRepository
                .findBySpaceIdAndDisplayPathStartingWithAndDeletedFalseOrderByDisplayPathAsc(
                        node.getSpaceId(), oldPath + "/");
        descendants.forEach(child -> child.setDisplayPath(
                node.getDisplayPath() + child.getDisplayPath().substring(oldPath.length())));
        nodeRepository.saveAll(descendants);
    }

    private List<FileNode> subtree(FileNode root) {
        List<FileNode> nodes = new ArrayList<>();
        nodes.add(root);
        if (root.getNodeType() == FileNodeType.FOLDER) {
            nodes.addAll(nodeRepository.findBySpaceIdAndDisplayPathStartingWithAndDeletedFalseOrderByDisplayPathAsc(
                    root.getSpaceId(), root.getDisplayPath() + "/"));
        }
        return nodes;
    }

    private FileSpace requireSpace(String spaceId) {
        return spaceRepository.findById(requireText(spaceId, "空间标识不能为空"))
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new BizException("文件空间不存在"));
    }

    private FileNode requireNode(String nodeId) {
        return nodeRepository.findByIdAndDeletedFalse(requireText(nodeId, "文件节点标识不能为空"))
                .orElseThrow(() -> new BizException("文件节点不存在"));
    }

    private FileNode requireActiveNode(String nodeId) {
        FileNode node = requireNode(nodeId);
        if (node.getNodeState() != FileNodeState.ACTIVE) {
            throw new BizException("文件节点不处于活动状态");
        }
        return node;
    }

    private FileNode resolveParent(FileSpace space, String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return null;
        }
        FileNode parent = requireActiveNode(parentId);
        if (!space.getId().equals(parent.getSpaceId()) || parent.getNodeType() != FileNodeType.FOLDER) {
            throw new BizException("父节点不是当前空间中的活动文件夹");
        }
        return parent;
    }

    private void ensureNameAvailable(String spaceId, String parentId, String nodeName, String currentNodeId) {
        nodeRepository.findBySpaceIdAndParentIdAndNodeNameAndNodeStateAndDeletedFalse(
                        spaceId, parentId, nodeName, FileNodeState.ACTIVE)
                .filter(item -> !item.getId().equals(currentNodeId))
                .ifPresent(item -> {
                    throw new BizException("目录中已存在同名文件或文件夹");
                });
    }

    private void auditInTransaction(FileOperationType operation, String spaceId, String nodeId, String detail) {
        transactionTemplate.executeWithoutResult(status -> audit(operation, spaceId, nodeId, detail));
    }

    private void audit(FileOperationType operation, String spaceId, String nodeId, String detail) {
        FileOperationLog log = new FileOperationLog();
        log.setId(id());
        log.setOperatorUserId(requireUserId());
        log.setOperation(operation);
        log.setSpaceId(spaceId);
        log.setNodeId(nodeId);
        log.setDetail(detail);
        log.setStatus(FileRecordStatus.SUCCESS.name());
        operationLogRepository.save(log);
    }

    private String requireTenantId() {
        return requireText(SecurityUtils.getTenantId(), "未获取到当前租户");
    }

    private String requireUserId() {
        return requireText(SecurityUtils.getUserId(), "未获取到当前用户");
    }

    private String safeName(String rawName) {
        String value = requireText(rawName, "文件名称不能为空").replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(value) || ".".equals(value) || "..".equals(value)
                || value.indexOf('\0') >= 0 || value.length() > 256) {
            throw new BizException("文件名称非法");
        }
        return value;
    }

    private String displayPath(FileNode parent, String name) {
        return parent == null ? "/" + name : parent.getDisplayPath() + "/" + name;
    }

    private String parentId(FileNode parent) {
        return parent == null ? null : parent.getId();
    }

    private String safeSuffix(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        String suffix = filename.substring(dotIndex);
        return SAFE_SUFFIX_PATTERN.matcher(suffix).matches() ? suffix.toLowerCase(Locale.ROOT) : "";
    }

    private Mono<Void> deleteTemporaryFile(Path path) {
        return Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // 临时目录由操作系统兜底清理，清理失败不能覆盖文件操作结果。
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private static String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private static String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static long positive(Long value, long fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private record FileContext(FileSpace space, FileNode node, FileVersion version) {
    }
}
