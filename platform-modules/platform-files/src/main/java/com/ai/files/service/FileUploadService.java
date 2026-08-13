package com.ai.files.service;

import com.ai.api.files.dto.FileUploadResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * 在数据库短事务之间编排对象存储操作的文件上传 saga。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
class FileUploadService {
    private final FileUploadTransactionService transactions;
    private final FileObjectStorage objectStorage;

    /**
     * 执行一次可恢复上传。
     *
     * <p>对象存储始终在事务外调用；数据库失败时保留 saga 记录，
     * 对象写入失败时则进入可重试清理状态。</p>
     */
    public FileUploadResultDTO upload(Path path, FileUploadTransactionService.UploadCommand command) {
        FileUploadTransactionService.Reservation reservation = transactions.reserve(command);
        try {
            objectStorage.put(path, reservation.objectKey(), command.contentType());
        } catch (RuntimeException uploadFailure) {
            cleanupFailedPut(reservation, uploadFailure);
            throw uploadFailure;
        }
        transactions.markObjectStored(reservation);
        return transactions.complete(reservation);
    }

    private void cleanupFailedPut(
            FileUploadTransactionService.Reservation reservation, RuntimeException uploadFailure) {
        try {
            transactions.markCleanupPending(reservation, uploadFailure);
        } catch (RuntimeException stateFailure) {
            uploadFailure.addSuppressed(stateFailure);
            return;
        }
        try {
            objectStorage.delete(reservation.objectKey());
            transactions.cancel(reservation);
        } catch (RuntimeException cleanupFailure) {
            uploadFailure.addSuppressed(cleanupFailure);
        }
    }
}
