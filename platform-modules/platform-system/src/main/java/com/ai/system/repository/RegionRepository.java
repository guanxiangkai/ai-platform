package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.vo.RegionPageVO;
import com.ai.system.domain.vo.RegionVO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 行政区域数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface RegionRepository extends BaseRepository<RegionPageVO, RegionVO, Region> {

    /**
     * 根据父区域ID查询子区域
     *
     * @param parentId 父区域ID
     * @return 子区域列表
     */
    @Query("""
            SELECT r FROM Region r
            WHERE r.parentId = :parentId AND r.deleted = false
            ORDER BY r.sortInfo.sortOrder ASC
            """)
    List<Region> findByParentIdAndDeletedFalseOrderBySortOrderAsc(@Param("parentId") String parentId);

    /**
     * 查询全部区域（按排序号升序）
     *
     * @return 区域列表
     */
    @Query("""
            SELECT r FROM Region r
            WHERE r.deleted = false
            ORDER BY r.sortInfo.sortOrder ASC
            """)
    List<Region> findByDeletedFalseOrderBySortOrderAsc();

    /**
     * 根据区域编码查询
     *
     * @param code 区域编码
     * @return 区域信息
     */
    Optional<Region> findByRegionCodeAndDeletedFalse(String regionCode);

    /**
     * 检查编码是否存在
     */
    boolean existsByRegionCodeAndDeletedFalse(String regionCode);
}
