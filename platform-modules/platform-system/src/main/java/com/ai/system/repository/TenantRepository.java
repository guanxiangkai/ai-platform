package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Tenant;
import com.ai.system.domain.vo.TenantPageVO;
import com.ai.system.domain.vo.TenantVO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 租户Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface TenantRepository extends BaseRepository<TenantPageVO, TenantVO, Tenant> {

    /**
     * 根据租户编码查询租户（过滤逻辑删除）
     *
     * @param tenantCode 租户编码
     * @return 租户信息
     */
    Optional<Tenant> findByTenantCodeAndDeletedFalse(String tenantCode);

    /** 查询可供选择的启用租户（过滤逻辑删除）。 */
    List<Tenant> findByEnabledTrueAndDeletedFalse(Pageable pageable);

    /**
     * 检查租户编码是否存在（过滤逻辑删除）
     *
     * @param tenantCode 租户编码
     * @return 是否存在
     */
    boolean existsByTenantCodeAndDeletedFalse(String tenantCode);
}
