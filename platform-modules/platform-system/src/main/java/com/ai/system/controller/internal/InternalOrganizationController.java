package com.ai.system.controller.internal;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import com.ai.api.system.dto.DeptIdentityDTO;
import com.ai.api.system.dto.UserOrganizationDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.User;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台组织身份内部接口。
 *
 * <p>调用方只能通过可信转发头携带当前租户，数据库租户拦截器负责最终隔离。
 * 业务服务不得再直接读取平台的 {@code sys_user} 和 {@code sys_dept} 表。</p>
 */
@RestController
@RequestMapping("/internal/organization")
@RequiredArgsConstructor
public class InternalOrganizationController {

    private final DeptRepository deptRepository;
    private final UserRepository userRepository;

    /** 查询当前租户内的部门身份信息。 */
    @GetMapping("/dept")
    public DeptIdentityDTO dept(@RequestParam("deptId") String deptId) {
        Dept dept = requireDept(deptId);
        String organizationUnitName = dept.getDeptName();
        if (StringUtils.hasText(dept.getParentId())) {
            organizationUnitName = deptRepository.findByIdAndDeletedFalse(dept.getParentId())
                    .map(Dept::getDeptName)
                    .filter(StringUtils::hasText)
                    .orElse(organizationUnitName);
        }
        return new DeptIdentityDTO(dept.getId(), dept.getDeptName(), dept.getParentId(), organizationUnitName);
    }

    /** 查询当前租户内的用户组织归属。 */
    @GetMapping("/user")
    public UserOrganizationDTO user(@RequestParam("userId") String userId) {
        User user = requireUser(userId);
        return new UserOrganizationDTO(user.getId(), user.getDeptId());
    }

    /** 校验用户是否属于当前租户的指定部门。 */
    @GetMapping("/user-in-dept")
    public boolean userInDept(
            @RequestParam("userId") String userId,
            @RequestParam("deptId") String deptId
    ) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(deptId)) {
            return false;
        }
        return userRepository.findById(userId.trim())
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .map(User::getDeptId)
                .filter(StringUtils::hasText)
                .map(deptId.trim()::equals)
                .orElse(false);
    }

    private Dept requireDept(String deptId) {
        if (!StringUtils.hasText(deptId)) {
            throw new BizException("部门ID不能为空");
        }
        return deptRepository.findByIdAndDeletedFalse(deptId.trim())
                .orElseThrow(() -> BizException.notFound("部门"));
    }

    private User requireUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BizException("用户ID不能为空");
        }
        return userRepository.findById(userId.trim())
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> BizException.notFound("用户"));
    }
}
