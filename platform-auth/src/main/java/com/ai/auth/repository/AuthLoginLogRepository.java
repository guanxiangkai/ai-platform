package com.ai.auth.repository;

import com.ai.auth.log.AuthLoginLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Auth 登录日志持久化仓库。 */
@Repository
public interface AuthLoginLogRepository extends JpaRepository<AuthLoginLogRecord, String> {

    @Query(value = """
            SELECT id AS "userId", tenant_id AS "tenantId"
            FROM sys_user
            WHERE username = :username
            ORDER BY update_time DESC NULLS LAST
            LIMIT 1
            """, nativeQuery = true)
    Optional<LoginUserContext> findUserContextByUsername(@Param("username") String username);

    @Query(value = """
            SELECT id
            FROM sys_tenant
            WHERE deleted = false
            ORDER BY create_time ASC NULLS LAST, id
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findDefaultTenantId();

    interface LoginUserContext {
        String getUserId();

        String getTenantId();
    }
}
