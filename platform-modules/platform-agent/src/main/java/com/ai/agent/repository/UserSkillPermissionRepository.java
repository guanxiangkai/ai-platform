package com.ai.agent.repository;

import com.ai.agent.domain.entity.UserSkillPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 用户技能白名单持久化接口。 */
public interface UserSkillPermissionRepository extends JpaRepository<UserSkillPermission, String> {
    @Query("""
            select permission.skillId
            from UserSkillPermission permission
            where permission.tenantId = :tenantId
              and permission.userId = :userId
              and permission.enabled = true
              and permission.deleted = false
            order by permission.sortInfo.sortOrder asc, permission.skillId asc
            """)
    List<String> findEnabledSkillIds(@Param("tenantId") String tenantId, @Param("userId") String userId);

    List<UserSkillPermission> findByTenantIdAndUserIdAndDeletedFalse(String tenantId, String userId);
}
