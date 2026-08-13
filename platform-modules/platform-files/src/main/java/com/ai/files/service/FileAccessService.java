package com.ai.files.service;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileRole;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.entity.FileGrant;
import com.ai.files.domain.entity.FileNode;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.repository.FileGrantRepository;
import com.ai.files.repository.FileNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** 统一计算空间所有权、部门成员关系和共享授权。 */
@Service
@RequiredArgsConstructor
public class FileAccessService {

    private final FileGrantRepository grantRepository;
    private final FileNodeRepository nodeRepository;

    /**
     * 校验当前主体是否具备目标角色。
     *
     * @param space 文件空间
     * @param node 文件节点；校验空间级权限时为空
     * @param required 所需最低角色
     */
    public void require(FileSpace space, FileNode node, FileRole required) {
        FileRole actual = role(space, node);
        if (actual == null || !actual.includes(required)) {
            throw new BizException("无权执行该文件操作");
        }
    }

    /**
     * 计算当前主体在空间或节点上的最高角色。
     *
     * @param space 文件空间
     * @param node 文件节点；为空时仅计算空间级授权
     * @return 最高角色，无访问权时为空
     */
    public FileRole role(FileSpace space, FileNode node) {
        if (SecurityUtils.isSuperAdmin()) {
            return FileRole.MANAGER;
        }
        String userId = SecurityUtils.getUserId();
        Set<String> deptIds = SecurityUtils.getDeptIds();
        FileRole role = ownerRole(space, userId, deptIds);
        role = FileRole.max(role, grantedRole(space, node, FilePrincipalType.USER,
                StringUtils.hasText(userId) ? List.of(userId) : List.of()));
        role = FileRole.max(role, grantedRole(space, node, FilePrincipalType.DEPARTMENT, deptIds));
        return role;
    }

    /** 判断当前调用者是否为可信内部服务主体。 */
    public boolean isInternalService() {
        return AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID.equals(SecurityUtils.getUserId());
    }

    private FileRole ownerRole(FileSpace space, String userId, Set<String> deptIds) {
        if (space.getSpaceType() == FileSpaceType.PERSONAL
                && StringUtils.hasText(userId)
                && userId.equals(space.getOwnerUserId())) {
            return FileRole.MANAGER;
        }
        if (space.getSpaceType() == FileSpaceType.DEPARTMENT
                && deptIds.contains(space.getOwnerDeptId())) {
            return FileRole.MANAGER;
        }
        if (space.getSpaceType() == FileSpaceType.SYSTEM && isInternalService()) {
            return FileRole.MANAGER;
        }
        return null;
    }

    private FileRole grantedRole(FileSpace space, FileNode target, FilePrincipalType type,
                                 Iterable<String> principals) {
        List<String> principalIds = new java.util.ArrayList<>();
        principals.forEach(principalIds::add);
        if (principalIds.isEmpty()) {
            return null;
        }
        Instant now = Instant.now();
        FileRole role = null;
        for (FileGrant grant : grantRepository
                .findBySpaceIdAndPrincipalTypeAndPrincipalIdInAndDeletedFalse(
                        space.getId(), type, principalIds)) {
            if (grant.getExpiresAt() != null && !grant.getExpiresAt().isAfter(now)) {
                continue;
            }
            if (appliesTo(grant, target)) {
                role = FileRole.max(role, grant.getGrantRole());
            }
        }
        return role;
    }

    private boolean appliesTo(FileGrant grant, FileNode target) {
        if (!StringUtils.hasText(grant.getNodeId())) {
            return true;
        }
        if (target == null) {
            return false;
        }
        if (grant.getNodeId().equals(target.getId())) {
            return true;
        }
        if (!Boolean.TRUE.equals(grant.getInherited())) {
            return false;
        }
        return nodeRepository.findByIdAndDeletedFalse(grant.getNodeId())
                .filter(parent -> target.getDisplayPath().startsWith(parent.getDisplayPath() + "/"))
                .isPresent();
    }
}
