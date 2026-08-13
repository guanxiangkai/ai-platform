package com.ai.system.service.impl;

import io.github.guanxiangkai.jpa.plus.interceptor.permission.enums.DataScopeType;
import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.domain.dto.DeptDTO;
import com.ai.system.domain.dto.DeptPageDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.Post;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.vo.DeptPageVO;
import com.ai.system.domain.vo.DeptVO;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.RegionRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.service.IDeptService;
import com.ai.system.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeptServiceImpl extends BaseServiceImpl<DeptPageDTO, DeptPageVO, DeptVO, DeptDTO, DeptDTO, Dept> implements IDeptService {


    private final DeptRepository repository;
    private final RegionRepository regionRepository;
    private final IPostService postService;
    private final UserRepository userRepository;

    @Override
    protected BaseRepository<DeptPageVO, DeptVO, Dept> getRepository() {
        return this.repository;
    }

    @Override
    protected Specification<Dept> buildQuerySpec(DeptPageDTO pageDTO) {
        if (pageDTO == null) {
            return super.buildQuerySpec(null);
        }
        return SpecUtils.<Dept>builder()
                .likeIfPresent(Dept::getDeptName, pageDTO.getDeptName())
                .likeIfPresent(Dept::getDeptCode, pageDTO.getDeptCode())
                .likeIfPresent(Dept::getLocation, pageDTO.getLocation())
                .eqIfPresent(Dept::getRegionCode, pageDTO.getRegionCode())
                .eqIfPresent(Dept::getEnabled, pageDTO.getEnabled())
                .build();
    }

    @Override
    public List<DeptVO> tree() {
        List<DeptVO> deptVOs = EntityConverter.toVoList(repository.findByDeletedFalse(), DeptVO.class);
        return buildTree(deptVOs, null);
    }

    @Override
    public List<OptionItem> options() {
        Boolean isSuperAdmin = SecurityUtils.isSuperAdmin();

        List<Dept> depts;
        if (isSuperAdmin) {
            depts = repository.findByDeletedFalse();
        } else {
            Set<String> deptIds = SecurityUtils.getDeptIds();
            if (deptIds.isEmpty()) {
                return List.of();
            }
            depts = repository.findByIdInAndDeletedFalse(deptIds);
        }

        return depts.stream()
                .map(dept -> OptionItem.of(dept.getDeptName(), dept.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public DeptVO getSelectedDept(String userId) {
        String deptId = getSelectedDeptId(userId);
        if (!StringUtils.hasText(deptId)) {
            return null;
        }
        return findDeptVo(deptId)
                .orElseGet(() -> fallbackToUserDept(userId, deptId));
    }

    @Override
    public RegionVO getSelectedDeptRegion(String userId) {
        String deptId = getSelectedDeptId(userId);
        if (!StringUtils.hasText(deptId)) {
            throw new BizException("当前用户未选择部门");
        }
        return getDeptRegion(deptId);
    }

    @Override
    public RegionVO getDeptRegion(String deptId) {
        String regionCode = resolveDeptRegionCode(deptId)
                .orElseThrow(() -> new BizException("当前部门未配置区域编码"));
        return regionRepository.findByRegionCodeAndDeletedFalse(regionCode)
                .map(region -> EntityConverter.toVo(region, RegionVO.class))
                .orElseThrow(() -> new BizException("区域不存在：" + regionCode));
    }

    @Override
    public String getSelectedDeptId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        String postDeptId = postService.getSelectedDeptId(userId);
        return StringUtils.hasText(postDeptId) ? postDeptId : loadUserDeptId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DeptDTO dto) {
        // 检查部门编码是否重复
        if (StringUtils.hasText(dto.deptCode()) &&
                repository.existsByDeptCodeAndDeletedFalse(dto.deptCode())) {
            throw new BizException("部门编码已存在");
        }
        validateRegionCode(dto.regionCode());

        Dept dept = EntityConverter.toEntity(dto, Dept.class);

        // 设置层级和祖级列表
        if (StringUtils.hasText(dto.parentId())) {
            DeptVO parent = detail(dto.parentId());
            dept.setAncestors(parent.getAncestors() + "," + parent.getId());
        }

        return repository.save(dept).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, DeptDTO dto) {
        DeptVO existingVO = detail(id);

        // 检查部门编码是否重复（排除自己）
        if (StringUtils.hasText(dto.deptCode()) &&
                !dto.deptCode().equals(existingVO.getDeptCode()) &&
                repository.existsByDeptCodeAndDeletedFalse(dto.deptCode())) {
            throw new BizException("部门编码已存在");
        }
        validateRegionCode(dto.regionCode());

        super.update(id, dto);
    }

    @Override
    public Set<String> getChildDeptIds(String deptId) {
        Set<String> result = new HashSet<>();
        if (deptId == null || deptId.isBlank()) {
            return result;
        }

        // 添加自身
        result.add(deptId);

        // 递归获取所有子部门
        collectChildDeptIds(deptId, result);

        return result;
    }

    @Override
    public Set<String> getChildDeptIds(Set<String> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Set.of();
        }

        Set<String> result = new HashSet<>();
        for (String deptId : deptIds) {
            result.addAll(getChildDeptIds(deptId));
        }

        return result;
    }

    /**
     * 递归收集所有子部门ID
     */
    private void collectChildDeptIds(String parentId, Set<String> result) {
        List<Dept> children = repository.findByParentIdAndDeletedFalse(parentId);
        for (Dept child : children) {
            result.add(child.getId());
            collectChildDeptIds(child.getId(), result);
        }
    }

    /**
     * 构建树形结构
     */
    private List<DeptVO> buildTree(List<DeptVO> depts, String parentId) {
        List<DeptVO> tree = new ArrayList<>();
        for (DeptVO dept : depts) {
            if ((parentId == null && dept.getParentId() == null) ||
                    (parentId != null && parentId.equals(dept.getParentId()))) {
                List<DeptVO> children = buildTree(depts, dept.getId());
                DeptVO withChildren = new DeptVO(
                        dept.getId(), dept.getCreateTime(), dept.getUpdateTime(), dept.getRemark(),
                        dept.getEnabled(), dept.getSortOrder(), dept.getDeptName(),
                        dept.getParentId(), dept.getDeptCode(), dept.getLocation(), dept.getRegionCode(),
                        dept.getLeaderId(), dept.getLeaderName(),
                        dept.getPhone(), dept.getEmail(),
                        dept.getAncestors(), children
                );
                tree.add(withChildren);
            }
        }
        return tree;
    }

    /**
     * 获取用户可访问的部门ID集合，用于填充 UserContext.deptIds
     * <p>
     * 基于用户 <strong>当前选中部门</strong> 下的所有有效岗位计算，而非单一岗位。
     * 切换岗位时只切换到该岗位所属部门，deptIds 会按该部门下岗位权限并集刷新。
     * <p>
     * 普通用户按当前部门下每个岗位的 {@link DataScopeType} 计算可访问部门并取并集。
     */
    @Override
    public Set<String> getUserDeptIds(String userId, Boolean superAdmin) {

        // 超级管理员拥有所有部门的访问权限
        if (Boolean.TRUE.equals(superAdmin)) {
            return repository.findByDeletedFalse().stream()
                    .map(Dept::getId)
                    .collect(Collectors.toSet());
        }

        String selectedDeptId = getSelectedDeptId(userId);
        if (!StringUtils.hasText(selectedDeptId)) {
            return Set.of();
        }

        List<Post> posts = postService.getUserPostsByDept(userId, selectedDeptId);
        if (posts.isEmpty()) {
            return Set.of();
        }

        Set<String> readableDeptIds = new LinkedHashSet<>();
        for (Post post : posts) {
            String postDeptId = post.getDeptId();
            if (!StringUtils.hasText(postDeptId)) {
                continue;
            }
            DataScopeType dataScope = post.getDataScope() == null ? DataScopeType.DEPT : post.getDataScope();
            switch (dataScope) {
                case ALL -> {
                    return repository.findByDeletedFalse().stream()
                            .map(Dept::getId)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                }
                case DEPT_AND_CHILD -> readableDeptIds.addAll(getChildDeptIds(postDeptId));
                case DEPT, SELF, CUSTOM -> readableDeptIds.add(postDeptId);
                default -> readableDeptIds.add(postDeptId);
            }
        }
        return readableDeptIds;
    }

    private DeptVO fallbackToUserDept(String userId, String candidateDeptId) {
        String userDeptId = loadUserDeptId(userId);
        if (!StringUtils.hasText(userDeptId) || userDeptId.equals(candidateDeptId)) {
            return null;
        }
        return findDeptVo(userDeptId).orElse(null);
    }

    private Optional<DeptVO> findDeptVo(String deptId) {
        return repository.findByIdAndDeletedFalse(deptId)
                .map(dept -> EntityConverter.toVo(dept, DeptVO.class));
    }

    private Optional<String> resolveDeptRegionCode(String deptId) {
        if (!StringUtils.hasText(deptId)) {
            return Optional.empty();
        }
        Set<String> visitedDeptIds = new HashSet<>();
        String currentDeptId = deptId.trim();
        while (StringUtils.hasText(currentDeptId) && visitedDeptIds.add(currentDeptId)) {
            Optional<Dept> dept = repository.findByIdAndDeletedFalse(currentDeptId);
            if (dept.isEmpty()) {
                return Optional.empty();
            }
            String regionCode = dept.get().getRegionCode();
            if (StringUtils.hasText(regionCode)) {
                return Optional.of(regionCode.trim());
            }
            currentDeptId = dept.get().getParentId();
        }
        if (StringUtils.hasText(currentDeptId)) {
            log.warn("部门层级存在循环，停止区域编码回溯，deptId={}", deptId);
        }
        return Optional.empty();
    }

    private String loadUserDeptId(String userId) {
        return userRepository.findById(userId)
                .map(User::getDeptId)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private void validateRegionCode(String regionCode) {
        if (StringUtils.hasText(regionCode) && !regionRepository.existsByRegionCodeAndDeletedFalse(regionCode)) {
            throw new BizException("区域不存在：" + regionCode);
        }
    }

}
