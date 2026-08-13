package com.ai.files.service;

import com.ai.api.files.dto.FileUploadResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadServiceTest {
    private FileUploadTransactionService transactions;
    private FileObjectStorage storage;
    private FileUploadService service;

    @BeforeEach
    void setUp() {
        transactions = mock(FileUploadTransactionService.class);
        storage = mock(FileObjectStorage.class);
        service = new FileUploadService(transactions, storage);
    }

    @Test
    void shouldCallObjectStorageOutsideReservationAndCompletionTransactions() {
        var command = command();
        var reservation = reservation();
        var result = new FileUploadResultDTO(
                "node-1", "report.txt", "object-key", "text/plain", 128L,
                "/files/node-1", "hash");
        when(transactions.reserve(command)).thenReturn(reservation);
        when(transactions.complete(reservation)).thenReturn(result);

        assertThat(service.upload(Path.of("/tmp/upload.bin"), command)).isSameAs(result);

        var ordered = inOrder(transactions, storage);
        ordered.verify(transactions).reserve(command);
        ordered.verify(storage).put(Path.of("/tmp/upload.bin"), "object-key", "text/plain");
        ordered.verify(transactions).markObjectStored(reservation);
        ordered.verify(transactions).complete(reservation);
    }

    @Test
    void failedPutAndFailedDeleteShouldRemainDurablyCleanupPending() {
        var command = command();
        var reservation = reservation();
        IllegalStateException uploadFailure = new IllegalStateException("put failed");
        IllegalStateException cleanupFailure = new IllegalStateException("delete failed");
        when(transactions.reserve(command)).thenReturn(reservation);
        org.mockito.Mockito.doThrow(uploadFailure).when(storage)
                .put(Path.of("/tmp/upload.bin"), "object-key", "text/plain");
        org.mockito.Mockito.doThrow(cleanupFailure).when(storage).delete("object-key");

        assertThatThrownBy(() -> service.upload(Path.of("/tmp/upload.bin"), command))
                .isSameAs(uploadFailure)
                .satisfies(error -> assertThat(error.getSuppressed()).contains(cleanupFailure));

        verify(transactions).markCleanupPending(reservation, uploadFailure);
        verify(transactions, never()).cancel(reservation);
    }

    @Test
    void metadataCompletionFailureShouldNotDeleteStoredObject() {
        var command = command();
        var reservation = reservation();
        IllegalStateException databaseFailure = new IllegalStateException("database unavailable");
        when(transactions.reserve(command)).thenReturn(reservation);
        when(transactions.complete(reservation)).thenThrow(databaseFailure);

        assertThatThrownBy(() -> service.upload(Path.of("/tmp/upload.bin"), command))
                .isSameAs(databaseFailure);

        verify(transactions).markObjectStored(reservation);
        verify(storage, never()).delete("object-key");
    }

    private FileUploadTransactionService.UploadCommand command() {
        return new FileUploadTransactionService.UploadCommand(
                "tenant-1", "user-1", "space-1", null, "report.txt", "text/plain",
                128L, "hash", null, null, null);
    }

    private FileUploadTransactionService.Reservation reservation() {
        return new FileUploadTransactionService.Reservation(
                "upload-1", "tenant-1", "token-1", "object-key");
    }
}
