package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.LoginLog;
import com.ai.system.domain.vo.LoginLogPageVO;
import com.ai.system.domain.vo.LoginLogVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录日志 Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface LoginLogRepository extends BaseRepository<LoginLogPageVO, LoginLogVO, LoginLog> {

    @Modifying
    @Transactional
    @Query("UPDATE LoginLog l SET l.deleted = true WHERE l.logTime < :cutoff AND l.deleted = false")
    int deleteByLogTimeBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("""
            UPDATE LoginLog l SET l.deleted = true
            WHERE l.logTime < :cutoff AND l.deleted = false
              AND (l.userId IS NULL OR l.userId <> :excludedUserId)
            """)
    int deleteByLogTimeBeforeExcludingUser(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("excludedUserId") String excludedUserId);
}
