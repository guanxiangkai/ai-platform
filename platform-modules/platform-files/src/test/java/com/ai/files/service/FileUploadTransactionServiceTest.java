package com.ai.files.service;

import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileNodeState;
import com.ai.files.domain.FileNodeType;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.FileUploadState;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.domain.entity.FileUploadRecord;
import com.ai.files.domain.entity.FileVersion;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileNodeRepository;
import com.ai.files.repository.FileOperationLogRepository;
import com.ai.files.repository.FileSpaceRepository;
import com.ai.files.repository.FileUploadRecordRepository;
import com.ai.files.repository.FileVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadTransactionServiceTest {
    private FileSpaceRepository spaces;
    private FileNodeRepository nodes;
    private FileVersionRepository versions;
    private FileUploadRecordRepository uploads;
    private FileEditLockRepository locks;
    private FileUploadTransactionService service;

    @BeforeEach
    void setUp() {
        spaces = mock(FileSpaceRepository.class);
        nodes = mock(FileNodeRepository.class);
        versions = mock(FileVersionRepository.class);
        uploads = mock(FileUploadRecordRepository.class);
        locks = mock(FileEditLockRepository.class);
        service = new FileUploadTransactionService(
                spaces, nodes, versions, uploads, locks,
                mock(FileOperationLogRepository.class), mock(FileAccessService.class),
                new FilesProperties(null, null, null, null, null, null,
                        null, null, null, null, null));
    }

    @Test
    void reservationShouldAllocateVersionUnderLockedNodeAndReserveQuota() {
        FileSpace space = space();
        FileNode node = node();
        when(spaces.findLockedByIdAndTenantId("space-1", "tenant-1")).thenReturn(Optional.of(space));
        when(nodes.findBySpaceIdAndParentIdAndNodeNameAndNodeStateInAndDeletedFalse(
                eq("space-1"), isNull(), eq("report.txt"), any())).thenReturn(Optional.of(node));
        when(nodes.findLockedByIdAndTenantId("node-1", "tenant-1")).thenReturn(Optional.of(node));
        when(locks.findByNodeIdAndDeletedFalse("node-1")).thenReturn(Optional.empty());
        when(versions.findMaxVersionNo("node-1", "tenant-1")).thenReturn(Optional.of(7));

        var reservation = service.reserve(command());

        assertThat(reservation.uploadId()).isEqualTo(node.getActiveUploadId());
        assertThat(space.getReservedBytes()).isEqualTo(128L);
        ArgumentCaptor<FileVersion> version = ArgumentCaptor.forClass(FileVersion.class);
        verify(versions).saveAndFlush(version.capture());
        assertThat(version.getValue().getVersionNo()).isEqualTo(8);
        ArgumentCaptor<FileUploadRecord> upload = ArgumentCaptor.forClass(FileUploadRecord.class);
        verify(uploads).save(upload.capture());
        assertThat(upload.getValue().getVersionId()).isEqualTo(version.getValue().getId());
        var ordered = inOrder(spaces, nodes, versions);
        ordered.verify(spaces).findLockedByIdAndTenantId("space-1", "tenant-1");
        ordered.verify(nodes).findBySpaceIdAndParentIdAndNodeNameAndNodeStateInAndDeletedFalse(
                eq("space-1"), isNull(), eq("report.txt"), any());
        ordered.verify(nodes).findLockedByIdAndTenantId("node-1", "tenant-1");
        ordered.verify(versions).findMaxVersionNo("node-1", "tenant-1");
    }

    @Test
    void secondUploadShouldBeRejectedWhileNodeHasActiveUpload() {
        FileSpace space = space();
        FileNode node = node();
        node.setActiveUploadId("upload-active");
        when(spaces.findLockedByIdAndTenantId("space-1", "tenant-1")).thenReturn(Optional.of(space));
        when(nodes.findBySpaceIdAndParentIdAndNodeNameAndNodeStateInAndDeletedFalse(
                eq("space-1"), isNull(), eq("report.txt"), any())).thenReturn(Optional.of(node));
        when(nodes.findLockedByIdAndTenantId("node-1", "tenant-1")).thenReturn(Optional.of(node));

        assertThatThrownBy(() -> service.reserve(command()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("当前文件已存在进行中的上传");
        verify(versions, never()).saveAndFlush(any());
    }

    @Test
    void cleanupStateShouldNotPersistInfrastructureErrorDetails() {
        FileUploadRecord upload = new FileUploadRecord();
        upload.setId("upload-1");
        upload.setTenantId("tenant-1");
        upload.setExecutionToken("token-1");
        upload.setUploadState(FileUploadState.PREPARED);
        when(uploads.findLockedByIdAndTenantId("upload-1", "tenant-1"))
                .thenReturn(Optional.of(upload));
        var reservation = new FileUploadTransactionService.Reservation(
                "upload-1", "tenant-1", "token-1", "object-key");

        service.markCleanupPending(
                reservation, new IllegalStateException("endpoint=https://private.example, token=secret"));

        assertThat(upload.getUploadState()).isEqualTo(FileUploadState.CLEANUP_PENDING);
        assertThat(upload.getLastError())
                .isEqualTo("IllegalStateException")
                .doesNotContain("private.example", "secret");
        verify(uploads).save(upload);
    }

    private FileUploadTransactionService.UploadCommand command() {
        return new FileUploadTransactionService.UploadCommand(
                "tenant-1", "user-1", "space-1", null, "report.txt", "text/plain",
                128L, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                null, null, null);
    }

    private FileSpace space() {
        FileSpace space = new FileSpace();
        space.setId("space-1");
        space.setTenantId("tenant-1");
        space.setSpaceType(FileSpaceType.PERSONAL);
        space.setQuotaBytes(1_024L);
        space.setUsedBytes(0L);
        space.setReservedBytes(0L);
        return space;
    }

    private FileNode node() {
        FileNode node = new FileNode();
        node.setId("node-1");
        node.setTenantId("tenant-1");
        node.setSpaceId("space-1");
        node.setNodeName("report.txt");
        node.setNodeType(FileNodeType.FILE);
        node.setNodeState(FileNodeState.ACTIVE);
        return node;
    }
}
