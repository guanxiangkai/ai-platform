package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.vo.DeptPageVO;
import com.ai.system.domain.vo.DeptVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 部门Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface DeptRepository
        extends BaseRepository<DeptPageVO, DeptVO, Dept>,
                JpaPlusRepository<Dept, String> {

    /** 根据父部门ID查询子部门列表（过滤逻辑删除） */
    default List<Dept> findByParentIdAndDeletedFalse(String parentId) {
        return list(QueryWrapper.from(Dept.class)
                .eq(Dept::getParentId, parentId)
                .eq(Dept::getDeleted, false));
    }

    /** 根据部门编码查询（过滤逻辑删除） */
    default Optional<Dept> findByDeptCodeAndDeletedFalse(String deptCode) {
        return one(QueryWrapper.from(Dept.class)
                .eq(Dept::getDeptCode, deptCode)
                .eq(Dept::getDeleted, false));
    }

    /** 根据 ID 查询部门（过滤逻辑删除） */
    default Optional<Dept> findByIdAndDeletedFalse(String id) {
        return one(QueryWrapper.from(Dept.class)
                .eq(Dept::getId, id)
                .eq(Dept::getDeleted, false));
    }

    /** 检查部门编码是否存在（过滤逻辑删除） */
    default boolean existsByDeptCodeAndDeletedFalse(String deptCode) {
        return count(QueryWrapper.from(Dept.class)
                .eq(Dept::getDeptCode, deptCode)
                .eq(Dept::getDeleted, false)) > 0;
    }

    /** 查询所有未删除部门 */
    default List<Dept> findByDeletedFalse() {
        return list(QueryWrapper.from(Dept.class)
                .eq(Dept::getDeleted, false));
    }

    /** 按 ID 集合查询未删除部门 */
    default List<Dept> findByIdInAndDeletedFalse(Set<String> ids) {
        return list(QueryWrapper.from(Dept.class)
                .in(Dept::getId, ids)
                .eq(Dept::getDeleted, false));
    }
}
