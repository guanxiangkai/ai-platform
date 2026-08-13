package com.ai.scheduler.repository;

import com.ai.scheduler.domain.SchedulerTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 通用调度任务持久化接口。 */
@Repository
public interface SchedulerTaskRepository extends JpaRepository<SchedulerTask, String>,
        JpaSpecificationExecutor<SchedulerTask> {

    /** 按租户和主键查询未删除任务。 */
    Optional<SchedulerTask> findByIdAndTenantIdAndDeletedFalse(String id, String tenantId);

    /** 判断租户内任务编码是否已存在。 */
    boolean existsByTenantIdAndTaskCodeAndDeletedFalse(String tenantId, String taskCode);

    /** 更新时排除当前记录判断任务编码是否已存在。 */
    boolean existsByTenantIdAndTaskCodeAndIdNotAndDeletedFalse(String tenantId, String taskCode, String id);
}
