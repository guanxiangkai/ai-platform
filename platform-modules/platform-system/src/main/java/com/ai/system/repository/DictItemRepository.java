package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.query.wrapper.UpdateWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.DictItem;
import com.ai.system.domain.vo.DictItemPageVO;
import com.ai.system.domain.vo.DictItemVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字典项Repository
 * <p>
 * 查询全部使用 jpa-plus {@link QueryWrapper} Lambda DSL，字段名由 Lambda 方法引用推导，
 * 重命名字段后编译器会立即报错，无需担心维护遗漏。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface DictItemRepository
        extends BaseRepository<DictItemPageVO, DictItemVO, DictItem>,
                JpaPlusRepository<DictItem, String> {

    /** 按 dictId 查询未删除项，按默认选中降序、排序号升序、创建时间升序 */
    default List<DictItem> findByDictId(String dictId) {
        return list(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictId, dictId)
                .eq(DictItem::getDeleted, false)
                .orderByDesc(DictItem::getItemSelected)
                .orderByAsc(DictItem::getSortOrder)
                .orderByAsc(DictItem::getCreateTime));
    }

    /** 按 dictCode 查询未删除项，按默认选中降序、排序号升序、创建时间升序 */
    default List<DictItem> findByDictCode(String dictCode) {
        return list(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictCode, dictCode)
                .eq(DictItem::getDeleted, false)
                .orderByDesc(DictItem::getItemSelected)
                .orderByAsc(DictItem::getSortOrder)
                .orderByAsc(DictItem::getCreateTime));
    }

    /** 按 dictCode + itemValue 精确查询单条未删除项 */
    default Optional<DictItem> findByDictCodeAndItemValue(String dictCode, String itemValue) {
        return one(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictCode, dictCode)
                .eq(DictItem::getItemValue, itemValue)
                .eq(DictItem::getDeleted, false));
    }

    /** 按 dictCode 查询已启用且未删除的字典项，按排序号升序 */
    default List<DictItem> findEnabledByDictCode(String dictCode) {
        return list(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictCode, dictCode)
                .eq(DictItem::getEnabled, true)
                .eq(DictItem::getDeleted, false)
                .orderByAsc(DictItem::getSortOrder));
    }

    /** 按 dictId 查询已启用且未删除的字典项，按排序号升序 */
    default List<DictItem> findEnabledByDictId(String dictId) {
        return list(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictId, dictId)
                .eq(DictItem::getEnabled, true)
                .eq(DictItem::getDeleted, false)
                .orderByAsc(DictItem::getSortOrder));
    }

    /** 查询指定 dictCode 下默认选中、已启用且未删除的字典项，按排序号升序 */
    default List<DictItem> findDefaultByDictCode(String dictCode) {
        return list(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictCode, dictCode)
                .eq(DictItem::getItemSelected, true)
                .eq(DictItem::getEnabled, true)
                .eq(DictItem::getDeleted, false)
                .orderByAsc(DictItem::getSortOrder));
    }

    /** 统计指定 dictId 下未删除的字典项数量 */
    default long countByDictId(String dictId) {
        return count(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDictId, dictId)
                .eq(DictItem::getDeleted, false));
    }

    /** 统计所有未删除的字典项数量 */
    default long countAll() {
        return count(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getDeleted, false));
    }

    /**
     * 清除 dictCode 下所有字典项的默认选中标记
     * <p>使用 UpdateWrapper 批量更新，无需加载实体到内存</p>
     */
    default int clearDefaultByDictCode(String dictCode) {
        UpdateWrapper<DictItem> wrapper = UpdateWrapper.from(DictItem.class);
        wrapper.set(DictItem::getItemSelected, false);
        wrapper.eq(DictItem::getDictCode, dictCode);
        wrapper.eq(DictItem::getDeleted, false);
        return update(wrapper);
    }

    /**
     * 将指定 ID 的字典项设为默认选中
     * <p>使用 UpdateWrapper 单条更新，无需加载实体到内存</p>
     */
    default int setDefaultById(String id) {
        UpdateWrapper<DictItem> wrapper = UpdateWrapper.from(DictItem.class);
        wrapper.set(DictItem::getItemSelected, true);
        wrapper.eq(DictItem::getId, id);
        wrapper.eq(DictItem::getDeleted, false);
        return update(wrapper);
    }

    /** 查询所有已启用且未删除的字典项（供 {@code SysDictWriter} 启动预热使用） */
    default List<DictItem> findEnabledAll() {
        return list(QueryWrapper.from(DictItem.class)
                .eq(DictItem::getEnabled, true)
                .eq(DictItem::getDeleted, false)
                .orderByAsc(DictItem::getDictCode)
                .orderByAsc(DictItem::getSortOrder));
    }
}
