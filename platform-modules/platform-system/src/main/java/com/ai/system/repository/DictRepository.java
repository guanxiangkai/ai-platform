package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Dict;
import com.ai.system.domain.vo.DictPageVO;
import com.ai.system.domain.vo.DictVO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字典数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface DictRepository
        extends BaseRepository<DictPageVO, DictVO, Dict>,
                JpaPlusRepository<Dict, String> {

    /** 根据字典类型查询字典列表（过滤逻辑删除） */
    default List<Dict> findByDictTypeAndDeletedFalse(String dictType) {
        return list(QueryWrapper.from(Dict.class)
                .eq(Dict::getDictType, dictType)
                .eq(Dict::getDeleted, false));
    }

    /** 根据字典类型和启用状态查询字典列表（过滤逻辑删除） */
    default List<Dict> findByDictTypeAndEnabledAndDeletedFalse(String dictType, Boolean enabled) {
        return list(QueryWrapper.from(Dict.class)
                .eq(Dict::getDictType, dictType)
                .eq(Dict::getEnabled, enabled)
                .eq(Dict::getDeleted, false));
    }

    /** 查询可供选择的启用字典（过滤逻辑删除）。 */
    List<Dict> findByEnabledTrueAndDeletedFalse(Pageable pageable);

    /** 分批查询当前租户的未删除字典。 */
    Page<Dict> findByDeletedFalse(Pageable pageable);
}
