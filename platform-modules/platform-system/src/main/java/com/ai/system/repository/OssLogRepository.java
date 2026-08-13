package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.OssLog;
import com.ai.system.domain.vo.OssLogPageVO;
import com.ai.system.domain.vo.OssLogVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * OSS 文件上传日志 Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface OssLogRepository extends BaseRepository<OssLogPageVO, OssLogVO, OssLog> {

    @Modifying
    @Transactional
    @Query("UPDATE OssLog o SET o.deleted = true WHERE o.logTime < :cutoff AND o.deleted = false")
    int deleteByLogTimeBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OssLog o SET o.deleted = true
            WHERE o.logTime < :cutoff AND o.deleted = false
              AND (o.userId IS NULL OR o.userId <> :excludedUserId)
            """)
    int deleteByLogTimeBeforeExcludingUser(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("excludedUserId") String excludedUserId);
}
