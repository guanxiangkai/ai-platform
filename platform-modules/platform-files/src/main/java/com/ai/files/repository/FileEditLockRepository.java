package com.ai.files.repository;

import com.ai.files.domain.entity.FileEditLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 文件编辑锁持久化接口。 */
@Repository
public interface FileEditLockRepository extends JpaRepository<FileEditLock, String> {

    /** 查询文件节点的有效或过期锁记录。 */
    Optional<FileEditLock> findByNodeIdAndDeletedFalse(String nodeId);
}
