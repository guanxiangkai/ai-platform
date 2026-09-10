package com.ai.files.service;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.api.files.dto.FileAccessUrlDTO;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileBusinessRootDTO;
import com.ai.api.files.dto.FileBusinessRootNodeDTO;
import com.ai.api.files.dto.FileBusinessRootNodePageDTO;
import com.ai.api.files.dto.FileBusinessUploadDTO;
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
import com.ai.files.domain.entity.FileBusinessRoot;
import com.ai.files.domain.entity.FileGrant;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileOperationLog;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.domain.entity.FileVersion;
import com.ai.files.domain.vo.FileViews;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileBusinessRootRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final FileBusinessRootRepository businessRootRepository;
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
            if (businessRootRepository.existsByRootNodeIdAndDeletedFalse(node.getId())) {
                throw new BizException("业务根目录只能由业务根目录接口重命名");
            }
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
            if (businessRootRepository.existsByRootNodeIdAndDeletedFalse(node.getId())) {
                throw new BizException("业务根目录不能移动");
            }
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
            if (businessRootRepository.existsByRootNodeIdAndDeletedFalse(node.getId())) {
                throw new BizException("业务根目录不能移入回收站");
            }
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

    /** 确保当前租户业务记录拥有唯一、稳定的系统空间根目录。 */
    public FileBusinessRootDTO ensureBusinessRoot(String businessType, String businessId, String displayName) {
        requireInternalService();
        String type = businessScope(businessType, 64, "业务类型无效");
        String id = businessScope(businessId, 128, "业务标识无效");
        String name = safeBusinessRootName(displayName);
        FileSpace systemSpace = systemSpace();
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileSpace lockedSpace = spaceRepository.findLockedByIdAndTenantId(systemSpace.getId(), requireTenantId())
                    .orElseThrow(() -> new BizException("系统文件空间不存在"));
            FileBusinessRoot binding = businessRootRepository
                    .findByTenantIdAndBusinessTypeAndBusinessIdAndDeletedFalse(requireTenantId(), type, id)
                    .orElse(null);
            if (binding != null) {
                FileNode root = requireBusinessRootNode(binding, lockedSpace);
                String expectedName = businessRootName(name, type, id);
                if (!expectedName.equals(root.getNodeName())) {
                    renameBusinessRoot(root, expectedName);
                }
                return businessRootView(root, lockedSpace);
            }
            FileNode root = nodeRepository.save(newNode(lockedSpace, null,
                    businessRootName(name, type, id), FileNodeType.FOLDER));
            FileBusinessRoot created = new FileBusinessRoot();
            created.setId(id());
            created.setTenantId(requireTenantId());
            created.setStatus(FileRecordStatus.ACTIVE.name());
            created.setBusinessType(type);
            created.setBusinessId(id);
            created.setRootNodeId(root.getId());
            businessRootRepository.save(created);
            audit(FileOperationType.CREATE_FOLDER, lockedSpace.getId(), root.getId(), "业务根目录");
            return businessRootView(root, lockedSpace);
        }));
    }

    /** 在既有业务根目录中创建受限的空目录；不允许隐式创建业务根。 */
    public void ensureBusinessDirectories(String rootBusinessType, String rootBusinessId, List<String> relativePaths) {
        requireInternalService();
        String type=businessScope(rootBusinessType,64,"根业务类型无效");
        String id=businessScope(rootBusinessId,128,"根业务标识无效");
        if(relativePaths==null||relativePaths.isEmpty()||relativePaths.size()>8000)throw new BizException("业务目录数量无效");
        List<String> paths=relativePaths.stream().map(this::normalizeRelativePath).toList();
        if(paths.stream().anyMatch(String::isEmpty))throw new BizException("业务目录路径不能为空");
        transactionTemplate.executeWithoutResult(status->{
            FileSpace space=spaceRepository.findLockedByIdAndTenantId(systemSpace().getId(),requireTenantId()).orElseThrow(()->new BizException("系统文件空间不存在"));
            FileNode root=requireBusinessRootNode(requireBusinessRoot(type,id),space);
            for(String path:paths)resolveBusinessPath(space,root,path);
        });
    }

    /** 上传文件到业务根目录内的相对路径，目录由服务端创建并校验。 */
    public Mono<FileBusinessUploadDTO> uploadToBusinessRoot(FilePart filePart, String rootBusinessType,
                                                             String rootBusinessId, String relativePath,
                                                             String businessType, String businessId) {
        requireInternalService();
        String type = businessScope(rootBusinessType, 64, "根业务类型无效");
        String id = businessScope(rootBusinessId, 128, "根业务标识无效");
        String path = normalizeRelativePath(relativePath);
        if (filePart == null) throw new BizException("上传文件不能为空");
        String filename = safeName(filePart.filename());
        if (path.isEmpty()) path = filename;
        int lastSeparator = path.lastIndexOf('/');
        if (!filename.equals(path.substring(lastSeparator + 1))) throw new BizException("相对路径的文件名与上传原件不一致");
        String directory = lastSeparator < 0 ? "" : path.substring(0, lastSeparator);
        String filePath = path;
        FileBusinessRootDTO root = businessRootForUpload(type, id);
        FileNode parent = resolveBusinessUploadParent(type, id, root.spaceId(), root.rootId(), directory);
        return upload(root.spaceId(), parent.getId(), filePart, businessType, businessId, null)
                .map(file -> new FileBusinessUploadDTO(file, root.rootId(), parent.getId(), filePath,
                        parent.getDisplayPath() + "/" + file.originName()));
    }

    /** 分页读取业务根目录或其子目录的直属活动节点。 */
    public FileBusinessRootNodePageDTO businessRootNodes(String businessType, String businessId,
                                                          String parentId, String nodeType, int page, int size) {
        requireInternalService();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(200, Math.max(1, size));
        FileBusinessRoot binding = requireBusinessRoot(businessType, businessId);
        FileSpace space = systemSpace();
        FileNode root = requireBusinessRootNode(binding, space);
        FileNode parent = resolveBusinessRootParent(space, root, parentId);
        FileNodeType type = parseNodeType(nodeType);
        Page<FileNode> result = type == null
                ? nodeRepository.findBySpaceIdAndParentIdAndNodeStateAndDeletedFalseOrderByNodeNameAsc(
                        space.getId(), parent.getId(), FileNodeState.ACTIVE, PageRequest.of(safePage - 1, safeSize))
                : nodeRepository.findBySpaceIdAndParentIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByNodeNameAsc(
                        space.getId(), parent.getId(), type, FileNodeState.ACTIVE, PageRequest.of(safePage - 1, safeSize));
        List<FileBusinessRootNodeDTO> records = result.getContent().stream()
                .map(node -> new FileBusinessRootNodeDTO(node.getId(), node.getParentId(), node.getNodeName(),
                        node.getDisplayPath(), node.getNodeType().name()))
                .toList();
        return new FileBusinessRootNodePageDTO(records, result.getTotalElements(), safePage, safeSize,
                result.hasNext());
    }

    /** 查询文件指定版本或当前版本在当前租户业务根目录中的真实位置。 */
    public FileBusinessUploadDTO fileMetadata(String fileId, String versionId) {
        requireInternalService();
        FileNode node = requireCurrentTenantFile(fileId);
        FileVersion version = availableVersion(node, versionId);
        FileBusinessRoot binding = businessRootForNode(node);
        String rootId = binding == null ? null : binding.getRootNodeId();
        String relativePath = binding == null ? null : relativePath(binding, node);
        return new FileBusinessUploadDTO(uploadResult(node, version), rootId, node.getParentId(), relativePath,
                node.getDisplayPath());
    }

    /** 在锁定空间和文件节点后将既有文件迁入业务根目录，不重传对象也不改写版本。 */
    public FileBusinessUploadDTO placeBusinessFile(String rootBusinessType, String rootBusinessId, String fileId,
                                                    String expectedCurrentVersionId, String relativePath) {
        requireInternalService();
        String type = businessScope(rootBusinessType, 64, "根业务类型无效");
        String id = businessScope(rootBusinessId, 128, "根业务标识无效");
        String path = normalizeRelativePath(relativePath);
        int lastSeparator = path.lastIndexOf('/');
        String filename = safeName(path.substring(lastSeparator + 1));
        String directory = lastSeparator < 0 ? "" : path.substring(0, lastSeparator);
        String expectedVersion = businessScope(expectedCurrentVersionId, 64, "预期当前版本标识无效");
        FileSpace systemSpace = systemSpace();
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileSpace space = spaceRepository.findLockedByIdAndTenantId(systemSpace.getId(), requireTenantId())
                    .orElseThrow(() -> new BizException("系统文件空间不存在"));
            FileNode root = requireBusinessRootNode(requireBusinessRoot(type, id), space);
            FileNode node = nodeRepository.findLockedByIdAndTenantId(requireText(fileId, "文件标识不能为空"), requireTenantId())
                    .orElseThrow(() -> new BizException("文件节点不存在"));
            if (!requireTenantId().equals(node.getTenantId()) || node.getNodeType() != FileNodeType.FILE
                    || node.getNodeState() != FileNodeState.ACTIVE) {
                throw new BizException("文件不属于当前租户的活动文件节点");
            }
            if (!space.getId().equals(node.getSpaceId())) {
                throw new BizException("文件不在当前租户系统空间");
            }
            FileBusinessRoot existingRoot = businessRootForNode(node);
            if (existingRoot != null && !root.getId().equals(existingRoot.getRootNodeId())) {
                throw new BizException("文件已归属其他业务根目录，不能迁入当前业务根目录");
            }
            if (!expectedVersion.equals(node.getCurrentVersionId())) {
                throw new BizException("文件当前版本已变更");
            }
            if (StringUtils.hasText(node.getActiveUploadId())) {
                throw new BizException("文件正在上传，暂不能迁移目录");
            }
            if (!filename.equals(node.getNodeName())) {
                throw new BizException("相对路径的文件名与当前文件名不一致");
            }
            FileVersion version = availableVersion(node, expectedVersion);
            FileNode parent = resolveBusinessPath(space, root, directory);
            ensureNameAvailable(space.getId(), parent.getId(), node.getNodeName(), node.getId());
            node.setParentId(parent.getId());
            node.setDisplayPath(parent.getDisplayPath() + "/" + node.getNodeName());
            nodeRepository.save(node);
            audit(FileOperationType.MOVE, space.getId(), node.getId(), "迁入业务根目录=" + root.getId());
            return new FileBusinessUploadDTO(uploadResult(node, version), root.getId(), parent.getId(), path,
                    node.getDisplayPath());
        }));
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

    /** 下载当前租户指定节点的指定可用版本。 */
    public Mono<ResponseEntity<Resource>> internalDownloadVersion(String nodeId, String versionId) {
        if (!accessService.isInternalService()) {
            return Mono.error(new BizException("仅可信内部服务可以调用该接口"));
        }
        FileContext context = readableVersion(nodeId, versionId);
        auditInTransaction(FileOperationType.DOWNLOAD, context.space().getId(), context.node().getId(),
                "version=" + context.version().getVersionNo());
        return Mono.fromCallable(() -> objectStorage.download(context.version())).subscribeOn(Schedulers.boundedElastic());
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

    private FileContext readableVersion(String nodeId, String versionId) {
        FileNode node = requireActiveNode(nodeId);
        if (!requireTenantId().equals(node.getTenantId())) {
            throw new BizException("文件节点不属于当前租户");
        }
        if (node.getNodeType() != FileNodeType.FILE) {
            throw new BizException("目标节点不是文件");
        }
        FileSpace space = requireSpace(node.getSpaceId());
        accessService.require(space, node, FileRole.VIEWER);
        FileVersion version = versionRepository.findByIdAndNodeIdAndTenantIdAndVersionStateAndDeletedFalse(
                        requireText(versionId, "文件版本标识不能为空"), node.getId(), requireTenantId(),
                        FileVersionState.AVAILABLE)
                .orElseThrow(() -> new BizException("文件版本不存在"));
        return new FileContext(space, node, version);
    }

    private FileBusinessRoot requireBusinessRoot(String businessType, String businessId) {
        return businessRootRepository.findByTenantIdAndBusinessTypeAndBusinessIdAndDeletedFalse(
                        requireTenantId(), businessScope(businessType, 64, "业务类型无效"),
                        businessScope(businessId, 128, "业务标识无效"))
                .orElseThrow(() -> new BizException("业务根目录不存在"));
    }

    private FileNode requireBusinessRootNode(FileBusinessRoot binding, FileSpace space) {
        FileNode root = requireActiveNode(binding.getRootNodeId());
        if (!space.getId().equals(root.getSpaceId()) || StringUtils.hasText(root.getParentId())
                || root.getNodeType() != FileNodeType.FOLDER
                || !requireTenantId().equals(root.getTenantId())) {
            throw new BizException("业务根目录绑定无效");
        }
        return root;
    }

    private FileBusinessRootDTO businessRootView(FileNode root, FileSpace space) {
        return new FileBusinessRootDTO(root.getId(), space.getId(), root.getDisplayPath(), root.getNodeName());
    }

    private FileBusinessRootDTO businessRootForUpload(String businessType, String businessId) {
        FileBusinessRoot binding = businessRootRepository
                .findByTenantIdAndBusinessTypeAndBusinessIdAndDeletedFalse(requireTenantId(), businessType, businessId)
                .orElse(null);
        if (binding == null) {
            return ensureBusinessRoot(businessType, businessId, businessType + "-" + businessId);
        }
        FileSpace space = systemSpace();
        return businessRootView(requireBusinessRootNode(binding, space), space);
    }

    private FileNode resolveBusinessUploadParent(String businessType, String businessId, String spaceId,
                                                  String rootId, String relativePath) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            FileSpace space = spaceRepository.findLockedByIdAndTenantId(spaceId, requireTenantId())
                    .orElseThrow(() -> new BizException("系统文件空间不存在"));
            FileNode parent = requireBusinessRootNode(requireBusinessRoot(businessType, businessId), space);
            if (!rootId.equals(parent.getId())) {
                throw new BizException("业务根目录绑定已变更");
            }
            return resolveBusinessPath(space, parent, relativePath);
        }));
    }

    private FileNode resolveBusinessPath(FileSpace space, FileNode root, String relativePath) {
        FileNode parent = root;
        for (String segment : relativePath.isEmpty() ? List.<String>of() : List.of(relativePath.split("/"))) {
            FileNode current = parent;
            parent = nodeRepository.findBySpaceIdAndParentIdAndNodeNameAndNodeStateAndDeletedFalse(
                            space.getId(), current.getId(), segment, FileNodeState.ACTIVE)
                    .map(node -> {
                        if (node.getNodeType() != FileNodeType.FOLDER) {
                            throw new BizException("业务目录名称冲突");
                        }
                        return node;
                    })
                    .orElseGet(() -> nodeRepository.save(newNode(space, current, segment, FileNodeType.FOLDER)));
        }
        return parent;
    }

    private FileNode requireCurrentTenantFile(String fileId) {
        FileNode node = requireActiveNode(fileId);
        if (!requireTenantId().equals(node.getTenantId()) || node.getNodeType() != FileNodeType.FILE) {
            throw new BizException("文件不属于当前租户的活动文件节点");
        }
        return node;
    }

    private FileVersion availableVersion(FileNode node, String versionId) {
        String selectedVersionId = StringUtils.hasText(versionId) ? versionId.trim() : node.getCurrentVersionId();
        return versionRepository.findByIdAndNodeIdAndTenantIdAndVersionStateAndDeletedFalse(
                        requireText(selectedVersionId, "文件版本标识不能为空"), node.getId(), requireTenantId(),
                        FileVersionState.AVAILABLE)
                .orElseThrow(() -> new BizException("文件版本不存在"));
    }

    FileBusinessRoot businessRootForNode(FileNode node) {
        FileNode current = node;
        while (current != null) {
            FileBusinessRoot root = businessRootRepository
                    .findByTenantIdAndRootNodeIdAndDeletedFalse(requireTenantId(), current.getId()).orElse(null);
            if (root != null) {
                return root;
            }
            current = StringUtils.hasText(current.getParentId()) ? requireCurrentTenantNode(current.getParentId()) : null;
        }
        return null;
    }

    private String relativePath(FileBusinessRoot binding, FileNode node) {
        FileNode root = requireBusinessRootNode(binding, systemSpace());
        String prefix = root.getDisplayPath() + "/";
        return node.getDisplayPath().startsWith(prefix) ? node.getDisplayPath().substring(prefix.length()) : null;
    }

    private FileNode requireCurrentTenantNode(String nodeId) {
        FileNode node = requireActiveNode(nodeId);
        if (!requireTenantId().equals(node.getTenantId())) {
            throw new BizException("文件节点不属于当前租户");
        }
        return node;
    }

    /** 公开元数据须走用户文件ACL，不暴露对象存储键。 */
    public FileUploadResultDTO publicMetadata(String nodeId) {
        FileContext context = readableFile(nodeId);
        FileVersion version = context.version();
        return new FileUploadResultDTO(context.node().getId(), version.getId(), version.getOriginalName(), null,
                version.getContentType(), version.getSizeBytes(), "/files/" + context.node().getId(), version.getSha256());
    }

    private FileUploadResultDTO uploadResult(FileNode node, FileVersion version) {
        return new FileUploadResultDTO(node.getId(), version.getId(), version.getOriginalName(), version.getObjectKey(),
                version.getContentType(), version.getSizeBytes(), "/files/" + node.getId(), version.getSha256());
    }

    private FileNode resolveBusinessRootParent(FileSpace space, FileNode root, String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return root;
        }
        FileNode parent = requireActiveNode(parentId);
        if (!space.getId().equals(parent.getSpaceId()) || parent.getNodeType() != FileNodeType.FOLDER
                || (!parent.getId().equals(root.getId())
                && !parent.getDisplayPath().startsWith(root.getDisplayPath() + "/"))) {
            throw new BizException("父目录不属于业务根目录");
        }
        return parent;
    }

    private void renameBusinessRoot(FileNode root, String name) {
        ensureNameAvailable(root.getSpaceId(), root.getParentId(), name, root.getId());
        String oldPath = root.getDisplayPath();
        root.setNodeName(name);
        root.setDisplayPath("/" + name);
        updateDescendantPaths(root, oldPath);
        nodeRepository.save(root);
        audit(FileOperationType.RENAME, root.getSpaceId(), root.getId(), oldPath + " -> " + root.getDisplayPath());
    }

    private String businessRootName(String displayName, String businessType, String businessId) {
        String suffix = "-" + FileChecksum.sha256(businessType.length() + ":" + businessType + businessId).substring(0, 24);
        return displayName.substring(0, Math.min(displayName.length(), 256 - suffix.length())) + suffix;
    }

    private String normalizeRelativePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return "";
        }
        String value = rawPath.trim().replace('\\', '/');
        if (value.startsWith("/") || value.matches("^[A-Za-z]:.*") || value.indexOf('\0') >= 0 || value.length() > 1_024) {
            throw new BizException("业务相对路径非法");
        }
        String[] segments = value.split("/", -1);
        if (segments.length > 16) {
            throw new BizException("业务相对路径层级过深");
        }
        List<String> normalized = new ArrayList<>();
        for (String segment : segments) {
            String name = segment.trim();
            if (!StringUtils.hasText(name) || ".".equals(name) || "..".equals(name) || name.length() > 128
                    || name.chars().anyMatch(Character::isISOControl)) {
                throw new BizException("业务相对路径非法");
            }
            normalized.add(name);
        }
        return String.join("/", normalized);
    }

    private String safeBusinessRootName(String displayName) {
        String name = safeName(displayName);
        if (name.chars().anyMatch(Character::isISOControl)) {
            throw new BizException("业务根目录名称非法");
        }
        return name;
    }

    FileSpace systemSpace() {
        if (!accessService.isInternalService()) {
            throw new BizException("仅可信内部服务可以访问系统文件空间");
        }
        String tenantId = requireTenantId();
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> spaceRepository
                    .findByTenantIdAndSpaceCodeAndDeletedFalse(tenantId, "system")
                    .orElseGet(() -> createSpace("system", "业务附件", FileSpaceType.SYSTEM,
                            null, null, properties.systemQuotaBytes()))));
        } catch (DataIntegrityViolationException conflict) {
            // 唯一约束使并发首建只有一个获胜者；失败事务已回滚，必须在新事务重查。
            return Objects.requireNonNull(transactionTemplate.execute(status -> spaceRepository
                    .findByTenantIdAndSpaceCodeAndDeletedFalse(tenantId, "system")
                    .orElseThrow(() -> conflict)));
        }
    }

    private static String businessScope(String value, int maxLength, String message) {
        if (!StringUtils.hasText(value) || value.trim().length() > maxLength
                || value.chars().anyMatch(Character::isISOControl)) throw new BizException(message);
        return value.trim();
    }

    private static FileNodeType parseNodeType(String rawType) {
        if (!StringUtils.hasText(rawType)) return null;
        try {
            return FileNodeType.valueOf(rawType.trim());
        } catch (IllegalArgumentException exception) {
            throw new BizException("文件节点类型无效");
        }
    }

    FileNode systemBusinessFolder(FileSpace space, String businessType, String businessId) {
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
        space.setTenantId(requireTenantId());
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
        node.setTenantId(requireTenantId());
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
