package com.ai.files.service;

import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileRecordStatus;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.entity.FileBusinessRoot;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.repository.FileBusinessRootRepository;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileGrantRepository;
import com.ai.files.repository.FileNodeRepository;
import com.ai.files.repository.FileOperationLogRepository;
import com.ai.files.repository.FileSpaceRepository;
import com.ai.files.repository.FileVersionRepository;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilesServiceBusinessRootsTest {

    private final Map<String, FileBusinessRoot> roots = new HashMap<>();
    private final Map<String, FileNode> nodesById = new HashMap<>();
    private FileBusinessRootRepository rootRepository;
    private FileNodeRepository nodeRepository;
    private FileUploadService uploadService;
    private FilesService service;
    private FileSpaceRepository spaces;
    private FileSpace systemSpace;

    @BeforeEach
    void setUp() {
        FileSpace space = new FileSpace();
        space.setId("system-space");
        space.setTenantId("tenant-a");
        space.setSpaceCode("system");
        space.setSpaceType(FileSpaceType.SYSTEM);
        space.setSpaceName("业务附件");
        space.setQuotaBytes(100L);
        space.setUsedBytes(0L);
        space.setReservedBytes(0L);
        space.setStatus(FileRecordStatus.ACTIVE.name());
        systemSpace = space;
        spaces = mock(FileSpaceRepository.class);
        when(spaces.findByTenantIdAndSpaceCodeAndDeletedFalse("tenant-a", "system")).thenReturn(Optional.of(space));
        when(spaces.findLockedByIdAndTenantId("system-space", "tenant-a")).thenReturn(Optional.of(space));

        rootRepository = mock(FileBusinessRootRepository.class);
        when(rootRepository.findByTenantIdAndBusinessTypeAndBusinessIdAndDeletedFalse(any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(roots.get(key(
                        invocation.getArgument(1), invocation.getArgument(2)))));
        when(rootRepository.save(any(FileBusinessRoot.class))).thenAnswer(invocation -> {
            FileBusinessRoot root = invocation.getArgument(0);
            roots.put(key(root.getBusinessType(), root.getBusinessId()), root);
            return root;
        });

        nodeRepository = mock(FileNodeRepository.class);
        when(nodeRepository.save(any(FileNode.class))).thenAnswer(invocation -> {
            FileNode node = invocation.getArgument(0);
            nodesById.put(node.getId(), node);
            return node;
        });
        when(nodeRepository.findByIdAndDeletedFalse(any())).thenAnswer(invocation ->
                Optional.ofNullable(nodesById.get(invocation.getArgument(0))));

        FileAccessService access = mock(FileAccessService.class);
        when(access.isInternalService()).thenReturn(true);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        uploadService = mock(FileUploadService.class);
        FilesProperties properties = mock(FilesProperties.class);
        when(properties.resolvedMaxFileSizeBytes()).thenReturn(100L);
        service = new FilesService(spaces, nodeRepository, rootRepository, mock(FileVersionRepository.class),
                mock(FileGrantRepository.class), mock(FileEditLockRepository.class), mock(FileOperationLogRepository.class),
                access, mock(FileObjectStorage.class), uploadService, properties, transactions);
        UserContextHolder.set(new UserContext("internal-service", "tenant-a", false, null,
                Set.of(), Set.of(), Set.of(), Map.of()));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void firstSpaceCreationConflictShouldReloadTheWinningTenantSpace() {
        when(spaces.findByTenantIdAndSpaceCodeAndDeletedFalse("tenant-a", "system"))
                .thenReturn(Optional.empty(), Optional.of(systemSpace));
        when(spaces.save(any(FileSpace.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique tenant space"));
        var root = service.ensureBusinessRoot("finance-project", "project-1", "项目甲");
        assertThat(root.spaceId()).isEqualTo("system-space");
        assertThat(roots).hasSize(1);
    }

    @Test
    void businessTupleHashAndScopeValidationShouldBeUnambiguous() {
        var left = service.ensureBusinessRoot("a:b", "c", "同名");
        var right = service.ensureBusinessRoot("a", "b:c", "同名");
        assertThat(left.rootId()).isNotEqualTo(right.rootId());
        assertThat(left.displayPath()).isNotEqualTo(right.displayPath());
        assertThatThrownBy(() -> service.ensureBusinessRoot("x".repeat(65), "id", "名称"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.ensureBusinessRoot("type", "id\n", "名称"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.uploadToBusinessRoot(null, "type", "id", "C:\\files\\source.json", "input", "1"))
                .isInstanceOf(BizException.class).hasMessage("业务相对路径非法");
    }

    @Test
    void ensureBusinessRootShouldReuseOneBindingAndKeepDifferentBusinessesIsolated() {
        var first = service.ensureBusinessRoot("finance-project", "project-1", "项目甲");
        var repeated = service.ensureBusinessRoot("finance-project", "project-1", "项目甲");
        var other = service.ensureBusinessRoot("finance-project", "project-2", "项目甲");

        assertThat(repeated.rootId()).isEqualTo(first.rootId());
        assertThat(other.rootId()).isNotEqualTo(first.rootId());
        assertThat(roots).hasSize(2);
        assertThat(nodesById.get(first.rootId()).getParentId()).isNull();
    }

    @Test
    void ensureBusinessRootShouldRenameBoundFolderWithoutReplacingItsStableId() {
        var first = service.ensureBusinessRoot("finance-project", "project-1", "项目甲");
        var renamed = service.ensureBusinessRoot("finance-project", "project-1", "项目乙");

        assertThat(renamed.rootId()).isEqualTo(first.rootId());
        assertThat(renamed.displayPath()).contains("项目乙");
    }

    @Test
    void uploadPathValidationShouldRejectTraversalBeforeStorageSagaStarts() {
        assertThatThrownBy(() -> service.uploadToBusinessRoot(null, "finance-project", "project-1",
                        "source/../escape", "invoice", "invoice-1"))
                .isInstanceOf(BizException.class)
                .hasMessage("业务相对路径非法");
    }

    @Test
    void uploadToBusinessRootShouldCreateAndUseTheValidatedRelativeParent() {
        var root = service.ensureBusinessRoot("finance-project", "project-1", "项目甲");
        FilePart part = mock(FilePart.class);
        when(part.filename()).thenReturn("凭证.txt");
        service = org.mockito.Mockito.spy(service);
        // 本用例验证根目录解析与委托；上传事务、对象失败恢复由 FileUploadServiceTest 单独验证。
        org.mockito.Mockito.doReturn(Mono.just(new com.ai.api.files.dto.FileUploadResultDTO(
                "file-1", "version-1", "凭证.txt", "object-1", "text/plain", 1L, "/files/file-1", "hash")))
                .when(service).upload(org.mockito.ArgumentMatchers.eq("system-space"), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.same(part), org.mockito.ArgumentMatchers.eq("invoice"),
                        org.mockito.ArgumentMatchers.eq("invoice-1"), org.mockito.ArgumentMatchers.isNull());

        var uploaded = service.uploadToBusinessRoot(part, "finance-project", "project-1",
                "原件/凭证/凭证.txt", "invoice", "invoice-1").block();

        assertThat(uploaded.rootId()).isEqualTo(root.rootId());
        assertThat(uploaded.parentId()).isNotEqualTo(root.rootId());
        assertThat(uploaded.relativePath()).isEqualTo("原件/凭证/凭证.txt");
        assertThat(uploaded.displayPath()).endsWith("/原件/凭证/凭证.txt");
        assertThat(uploaded.file().versionId()).isEqualTo("version-1");
        org.mockito.Mockito.verify(service).upload("system-space", uploaded.parentId(), part, "invoice", "invoice-1", null);
    }

    private static String key(String businessType, String businessId) {
        return businessType.length() + ":" + businessType + businessId;
    }

    @Test
    void realMultipartPreparationShouldPreserveTenantAndVersionAcrossWorkerThreads() {
        String hook = "business-root-upload-test";
        UserContext context = new UserContext("internal-service", "tenant-a", false, null,
                Set.of(), Set.of(), Set.of(), Map.of());
        // 手动构造的服务没有 Web 请求上下文桥；只在本测试内复现生产请求的线程上下文传递。
        reactor.core.scheduler.Schedulers.onScheduleHook(hook, task -> () -> {
            UserContextHolder.set(context);
            try { task.run(); } finally { UserContextHolder.clear(); }
        });
        try {
            var root = service.ensureBusinessRoot("finance-project", "multipart-project", "项目原件");
            FilePart part = mock(FilePart.class);
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.TEXT_PLAIN);
            when(part.filename()).thenReturn("原件.txt"); when(part.headers()).thenReturn(headers);
            when(part.transferTo(any(java.nio.file.Path.class))).thenAnswer(invocation -> Mono.fromRunnable(() -> {
                try { java.nio.file.Files.writeString(invocation.getArgument(0), "x"); }
                catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            }));
            when(uploadService.upload(any(), any())).thenAnswer(invocation -> {
                FileUploadTransactionService.UploadCommand command = invocation.getArgument(1);
                assertThat(command.tenantId()).isEqualTo("tenant-a");
                assertThat(command.userId()).isEqualTo("internal-service");
                assertThat(command.originalName()).isEqualTo("原件.txt");
                assertThat(command.sizeBytes()).isEqualTo(1);
                return new com.ai.api.files.dto.FileUploadResultDTO("file-actual", "version-actual", "原件.txt",
                        "object-actual", "text/plain", 1L, "/files/file-actual", command.sha256());
            });
            var uploaded = service.uploadToBusinessRoot(part, "finance-project", "multipart-project",
                    "输入/原件.txt", "input", "input-1").block(java.time.Duration.ofSeconds(10));
            assertThat(uploaded.rootId()).isEqualTo(root.rootId());
            assertThat(uploaded.displayPath()).endsWith("/输入/原件.txt");
            assertThat(uploaded.file().versionId()).isEqualTo("version-actual");
        } finally {
            reactor.core.scheduler.Schedulers.resetOnScheduleHook(hook);
        }
    }
}
