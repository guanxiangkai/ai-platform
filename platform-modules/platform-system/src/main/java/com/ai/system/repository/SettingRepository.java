package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import com.ai.system.domain.entity.Setting;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 应用设置Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface SettingRepository extends JpaPlusRepository<Setting, String> {

    /**
     * 根据所属人ID查询设置（过滤逻辑删除）
     *
     * @param ownerId 所属人ID
     * @return 设置信息
     */
    Optional<Setting> findByOwnerIdAndDeletedFalse(String ownerId);

    /**
     * 批量查询当前租户内的用户设置。
     *
     * @param ownerIds 所属人标识集合
     * @return 已保存的用户设置
     */
    List<Setting> findAllByOwnerIdInAndDeletedFalse(Collection<String> ownerIds);
}
