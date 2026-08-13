package com.ai.files.service;

import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileUploadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FileUploadReconcilerTest {
    private FileUploadTransactionService transactions;
    private FileObjectStorage storage;
    private FileUploadReconciler reconciler;

    @BeforeEach
    void setUp() {
        transactions = mock(FileUploadTransactionService.class);
        storage = mock(FileObjectStorage.class);
        reconciler = new FileUploadReconciler(
                mock(JdbcTemplate.class), transactions, storage,
                new FilesProperties(null, null, null, null, null, null,
                        null, null, null, null, null));
    }

    @Test
    void preparedUploadWithExistingObjectShouldResumeMetadataCompletion() {
        var reservation = reservation();
        var recovery = new FileUploadTransactionService.Recovery(
                reservation, FileUploadState.PREPARED);
        org.mockito.Mockito.when(storage.exists("object-key")).thenReturn(true);

        reconciler.recover(recovery);

        var ordered = inOrder(storage, transactions);
        ordered.verify(storage).exists("object-key");
        ordered.verify(transactions).markObjectStored(reservation);
        ordered.verify(transactions).complete(reservation);
    }

    @Test
    void cleanupFailureShouldRetainStateAndScheduleRetry() {
        var reservation = reservation();
        var recovery = new FileUploadTransactionService.Recovery(
                reservation, FileUploadState.CLEANUP_PENDING);
        IllegalStateException failure = new IllegalStateException("storage unavailable");
        org.mockito.Mockito.doThrow(failure).when(storage).delete("object-key");

        reconciler.recover(recovery);

        verify(transactions).recordRecoveryFailure(reservation, failure);
    }

    private FileUploadTransactionService.Reservation reservation() {
        return new FileUploadTransactionService.Reservation(
                "upload-1", "tenant-1", "token-1", "object-key");
    }
}
