package com.ai.files.service;

import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileNodeState;
import com.ai.files.domain.FileNodeType;
import com.ai.files.domain.FileOperationType;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileOperationLog;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileGrantRepository;
import com.ai.files.repository.FileNodeRepository;
import com.ai.files.repository.FileOperationLogRepository;
import com.ai.files.repository.FileSpaceRepository;
import com.ai.files.repository.FileVersionRepository;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilesServiceBusinessFilesTest {

    private FileNodeRepository nodes;
    private FileOperationLogRepository operationLogs;
    private FilesService service;

    @BeforeEach
    void setUp() {
        nodes = mock(FileNodeRepository.class);
        operationLogs = mock(FileOperationLogRepository.class);
        FileAccessService access = mock(FileAccessService.class);
        when(access.isInternalService()).thenReturn(true);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            invocation.<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>getArgument(0)
                    .accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
        service = new FilesService(
                mock(FileSpaceRepository.class), nodes, mock(FileVersionRepository.class),
                mock(FileGrantRepository.class), mock(FileEditLockRepository.class), operationLogs,
                access, mock(FileObjectStorage.class), mock(FileUploadService.class), mock(FilesProperties.class),
                transactions);
        UserContextHolder.set(new UserContext(
                "internal-service", "tenant-a", false, null, Set.of(), Set.of(), Set.of(), Map.of()));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void shouldQueryOnlyCurrentTenantActiveBusinessFiles() {
        FileNode node = businessFile("file-1", "report.pdf");
        when(nodes.findByTenantIdAndBusinessTypeAndBusinessIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByCreateTimeDesc(
                "tenant-a", "report", "record-1", FileNodeType.FILE, FileNodeState.ACTIVE))
                .thenReturn(List.of(node));

        assertThat(service.internalActiveBusinessFiles("report", "record-1"))
                .extracting(item -> item.fileId() + ":" + item.filename())
                .containsExactly("file-1:report.pdf");

        verify(nodes).findByTenantIdAndBusinessTypeAndBusinessIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByCreateTimeDesc(
                "tenant-a", "report", "record-1", FileNodeType.FILE, FileNodeState.ACTIVE);
    }

    @Test
    void shouldRecycleActiveBusinessFilesAndAllowRetryWithoutAdditionalWrites() {
        FileNode node = businessFile("file-1", "report.pdf");
        when(nodes.findByTenantIdAndBusinessTypeAndBusinessIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByCreateTimeDesc(
                "tenant-a", "report", "record-1", FileNodeType.FILE, FileNodeState.ACTIVE))
                .thenReturn(List.of(node), List.of());

        service.internalRecycleBusinessFiles("report", "record-1");
        service.internalRecycleBusinessFiles("report", "record-1");

        assertThat(node.getNodeState()).isEqualTo(FileNodeState.TRASHED);
        assertThat(node.getTrashedBy()).isEqualTo("internal-service");
        verify(nodes).saveAll(List.of(node));
        ArgumentCaptor<FileOperationLog> operationLog = ArgumentCaptor.forClass(FileOperationLog.class);
        verify(operationLogs).save(operationLog.capture());
        assertThat(operationLog.getValue().getOperation()).isEqualTo(FileOperationType.TRASH);
    }

    @Test
    void shouldRejectBusinessFileOperationsForNonInternalCaller() {
        FileAccessService access = mock(FileAccessService.class);
        when(access.isInternalService()).thenReturn(false);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        FilesService denied = new FilesService(
                mock(FileSpaceRepository.class), nodes, mock(FileVersionRepository.class),
                mock(FileGrantRepository.class), mock(FileEditLockRepository.class), operationLogs,
                access, mock(FileObjectStorage.class), mock(FileUploadService.class), mock(FilesProperties.class),
                transactions);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> denied.internalActiveBusinessFiles("report", "record-1"))
                .hasMessage("仅可信内部服务可以调用该接口");
        verify(nodes, never()).findByTenantIdAndBusinessTypeAndBusinessIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByCreateTimeDesc(
                any(), any(), any(), any(), any());
    }

    private static FileNode businessFile(String id, String filename) {
        FileNode node = new FileNode();
        node.setId(id);
        node.setSpaceId("space-1");
        node.setNodeName(filename);
        node.setNodeType(FileNodeType.FILE);
        node.setNodeState(FileNodeState.ACTIVE);
        return node;
    }
}
