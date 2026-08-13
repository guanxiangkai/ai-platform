package com.ai.files.repository;

import com.ai.files.domain.entity.FileOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 文件操作审计持久化接口。 */
@Repository
public interface FileOperationLogRepository extends JpaRepository<FileOperationLog, String> {
}
