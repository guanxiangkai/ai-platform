package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.DeptDTO;
import com.ai.system.domain.dto.DeptPageDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.vo.DeptPageVO;
import com.ai.system.domain.vo.DeptVO;
import com.ai.system.domain.vo.RegionVO;

import java.util.List;
import java.util.Set;

/**
 * 部门服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IDeptService extends IBaseService<DeptPageDTO, DeptPageVO, DeptVO, DeptDTO, DeptDTO, Dept> {

    /**
     * 查询部门树
     *
     * @return 部门树
     */
    List<DeptVO> tree();

    /**
     * 获取部门选择器列表（根据数据权限过滤）
     * <p>
     * 根据当前用户的数据权限返回可选择的部门列表
     *
     * @return 部门选项列表
     */
    List<OptionItem> options();

    /**
     * 获取用户当前选中的部门
     * <p>
     * 优先从三级缓存读取选中部门；缓存未命中时使用用户所属部门并回填缓存。
     * </p>
     *
     * @param userId 用户ID
     * @return 当前选中的部门，无所属部门返回 null
     */
    DeptVO getSelectedDept(String userId);

    /**
     * 获取用户当前选中部门的区域信息
     *
     * @param userId 用户ID
     * @return 当前选中部门关联区域
     */
    RegionVO getSelectedDeptRegion(String userId);

    /**
     * 获取指定部门关联区域。
     * <p>
     * 优先使用部门自身区域编码；当前部门未配置时，向上查找父部门区域编码。
     * </p>
     *
     * @param deptId 部门ID
     * @return 部门关联区域
     */
    RegionVO getDeptRegion(String deptId);

    /**
     * 获取用户当前选中的部门ID
     *
     * @param userId 用户ID
     * @return 当前选中的部门ID，无所属部门返回 null
     */
    String getSelectedDeptId(String userId);

    /**
     * 获取部门及其所有子部门的ID集合（递归）
     *
     * @param deptId 部门ID
     * @return 部门ID集合（包含自身和所有子部门）
     */
    Set<String> getChildDeptIds(String deptId);

    /**
     * 批量获取多个部门及其所有子部门的ID集合
     *
     * @param deptIds 部门ID集合
     * @return 部门ID集合（包含所有部门及其子部门）
     */
    Set<String> getChildDeptIds(Set<String> deptIds);

    /**
     * 获取用户部门ID集合
     * <p>
     * 通过用户的岗位和数据权限范围查询所有可访问的部门ID
     *
     * @param userId 用户ID
     * @return 部门ID集合
     */
    Set<String> getUserDeptIds(String userId, Boolean superAdmin);

}
