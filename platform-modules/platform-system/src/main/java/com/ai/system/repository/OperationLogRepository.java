package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.OperationLog;
import com.ai.system.domain.vo.OperationLogPageVO;
import com.ai.system.domain.vo.OperationLogVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 操作日志 Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface OperationLogRepository extends BaseRepository<OperationLogPageVO, OperationLogVO, OperationLog> {

    /** 判断操作唯一标识是否已持久化。 */
    boolean existsByOperationId(String operationId);

    @Modifying
    @Transactional
    @Query("UPDATE OperationLog o SET o.deleted = true WHERE o.logTime < :cutoff AND o.deleted = false")
    int deleteByLogTimeBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OperationLog o SET o.deleted = true
            WHERE o.logTime < :cutoff AND o.deleted = false
              AND (o.userId IS NULL OR o.userId <> :excludedUserId)
            """)
    int deleteByLogTimeBeforeExcludingUser(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("excludedUserId") String excludedUserId);
}
