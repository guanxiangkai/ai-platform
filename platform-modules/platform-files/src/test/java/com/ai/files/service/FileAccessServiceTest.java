package com.ai.files.service;

import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileRole;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.entity.FileGrant;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.repository.FileGrantRepository;
import com.ai.files.repository.FileNodeRepository;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileAccessServiceTest {

    private FileGrantRepository grants;
    private FileAccessService service;

    @BeforeEach
    void setUp() {
        grants = mock(FileGrantRepository.class);
        service = new FileAccessService(grants, mock(FileNodeRepository.class));
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void ordinaryUserShouldEnterOwnPersonalSpace() {
        setUser("user-1", false, Set.of());

        assertThat(service.role(space(FileSpaceType.PERSONAL, "user-1", null), null))
                .isEqualTo(FileRole.MANAGER);
    }

    @Test
    void departmentMemberShouldEnterOwnedDepartmentSpace() {
        setUser("user-1", false, Set.of("dept-1"));

        assertThat(service.role(space(FileSpaceType.DEPARTMENT, null, "dept-1"), null))
                .isEqualTo(FileRole.MANAGER);
    }

    @Test
    void expiredSpaceGrantMustNotGrantSpaceEntry() {
        setUser("user-1", false, Set.of());
        FileGrant grant = spaceGrant();
        grant.setExpiresAt(Instant.now().minusSeconds(1));
        when(grants.findBySpaceIdAndPrincipalTypeAndPrincipalIdInAndDeletedFalse(
                "space-1", FilePrincipalType.USER, List.of("user-1"))).thenReturn(List.of(grant));

        assertThat(service.role(space(FileSpaceType.SYSTEM, null, null), null)).isNull();
    }

    @Test
    void nodeGrantMustNotGrantSpaceEntry() {
        setUser("user-1", false, Set.of());
        FileGrant grant = spaceGrant();
        grant.setNodeId("node-1");
        when(grants.findBySpaceIdAndPrincipalTypeAndPrincipalIdInAndDeletedFalse(
                "space-1", FilePrincipalType.USER, List.of("user-1"))).thenReturn(List.of(grant));

        assertThat(service.role(space(FileSpaceType.SYSTEM, null, null), null)).isNull();
    }

    @Test
    void superAdminShouldEnterEverySpaceWithoutGrantLookup() {
        setUser("admin-1", true, Set.of());

        assertThat(service.role(space(FileSpaceType.SYSTEM, null, null), null))
                .isEqualTo(FileRole.MANAGER);
        verifyNoInteractions(grants);
    }

    @Test
    void internalServiceShouldEnterSystemSpaceOnlyBySystemOwnershipRule() {
        setUser(AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID, false, Set.of());

        assertThat(service.role(space(FileSpaceType.SYSTEM, null, null), null))
                .isEqualTo(FileRole.MANAGER);
    }

    private void setUser(String userId, boolean superAdmin, Set<String> deptIds) {
        UserContextHolder.set(new UserContext(
                userId, "tenant-1", superAdmin, null, deptIds, Set.of(), Set.of(), Map.of()));
    }

    private FileSpace space(FileSpaceType type, String ownerUserId, String ownerDeptId) {
        FileSpace space = new FileSpace();
        space.setId("space-1");
        space.setTenantId("tenant-1");
        space.setSpaceType(type);
        space.setOwnerUserId(ownerUserId);
        space.setOwnerDeptId(ownerDeptId);
        return space;
    }

    private FileGrant spaceGrant() {
        FileGrant grant = new FileGrant();
        grant.setSpaceId("space-1");
        grant.setPrincipalType(FilePrincipalType.USER);
        grant.setPrincipalId("user-1");
        grant.setGrantRole(FileRole.VIEWER);
        return grant;
    }
}
