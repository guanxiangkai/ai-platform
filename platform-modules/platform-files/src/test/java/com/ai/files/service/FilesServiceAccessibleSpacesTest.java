package com.ai.files.service;

import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.entity.FileSpace;
import com.ai.files.repository.FileEditLockRepository;
import com.ai.files.repository.FileGrantRepository;
import com.ai.files.repository.FileNodeRepository;
import com.ai.files.repository.FileOperationLogRepository;
import com.ai.files.repository.FileSpaceRepository;
import com.ai.files.repository.FileVersionRepository;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilesServiceAccessibleSpacesTest {

    private FileSpaceRepository spaces;
    private FileAccessService access;
    private FilesService service;

    @BeforeEach
    void setUp() {
        spaces = mock(FileSpaceRepository.class);
        access = mock(FileAccessService.class);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        service = new FilesService(
                spaces,
                mock(FileNodeRepository.class),
                mock(FileVersionRepository.class),
                mock(FileGrantRepository.class),
                mock(FileEditLockRepository.class),
                mock(FileOperationLogRepository.class),
                access,
                mock(FileObjectStorage.class),
                mock(FileUploadService.class),
                mock(com.ai.files.config.FilesProperties.class),
                transactions);
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void accessibleSpacesShouldUseBoundedDomainQueryForOrdinaryDepartmentMember() {
        setUser("user-1", false, Set.of("dept-1"));
        FileSpace personal = space("personal-1", FileSpaceType.PERSONAL, "user-1", null);
        FileSpace department = space("department-1", FileSpaceType.DEPARTMENT, null, "dept-1");
        when(spaces.findBySpaceTypeAndOwnerUserIdAndDeletedFalse(FileSpaceType.PERSONAL, "user-1"))
                .thenReturn(Optional.of(personal));
        when(spaces.findAccessibleSpaces(
                eq("user-1"), eq(List.of("dept-1")), eq(true), any(), eq(false), eq(false),
                eq(FileSpaceType.DEPARTMENT), eq(FileSpaceType.SYSTEM),
                eq(FilePrincipalType.USER), eq(FilePrincipalType.DEPARTMENT)))
                .thenReturn(List.of(personal, department));

        assertThat(service.accessibleSpaces()).extracting(FileSpace::getId)
                .containsExactly("personal-1", "department-1");
        verify(spaces).findAccessibleSpaces(
                eq("user-1"), eq(List.of("dept-1")), eq(true), any(), eq(false), eq(false),
                eq(FileSpaceType.DEPARTMENT), eq(FileSpaceType.SYSTEM),
                eq(FilePrincipalType.USER), eq(FilePrincipalType.DEPARTMENT));
        verify(spaces, never()).findAll();
    }

    @Test
    void accessibleSpacesShouldPreserveSuperAdminCurrentTenantQueryBoundary() {
        setUser("admin-1", true, Set.of());
        FileSpace personal = space("personal-1", FileSpaceType.PERSONAL, "admin-1", null);
        FileSpace system = space("system-1", FileSpaceType.SYSTEM, null, null);
        when(spaces.findBySpaceTypeAndOwnerUserIdAndDeletedFalse(FileSpaceType.PERSONAL, "admin-1"))
                .thenReturn(Optional.of(personal));
        when(spaces.findAccessibleSpaces(
                eq("admin-1"), eq(List.of("")), eq(false), any(), eq(true), eq(false),
                eq(FileSpaceType.DEPARTMENT), eq(FileSpaceType.SYSTEM),
                eq(FilePrincipalType.USER), eq(FilePrincipalType.DEPARTMENT)))
                .thenReturn(List.of(system));

        assertThat(service.accessibleSpaces()).extracting(FileSpace::getId)
                .containsExactly("personal-1", "system-1");
        verify(spaces, never()).findAll();
    }

    private void setUser(String userId, boolean superAdmin, Set<String> deptIds) {
        UserContextHolder.set(new UserContext(
                userId, "tenant-1", superAdmin, null, deptIds, Set.of(), Set.of(), Map.of()));
    }

    private FileSpace space(String id, FileSpaceType type, String ownerUserId, String ownerDeptId) {
        FileSpace space = new FileSpace();
        space.setId(id);
        space.setTenantId("tenant-1");
        space.setSpaceType(type);
        space.setOwnerUserId(ownerUserId);
        space.setOwnerDeptId(ownerDeptId);
        return space;
    }
}
