package com.ai.files.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 文件角色权限层级测试。 */
class FileRoleTest {

    @Test
    void managerShouldIncludeAllRoles() {
        assertTrue(FileRole.MANAGER.includes(FileRole.MANAGER));
        assertTrue(FileRole.MANAGER.includes(FileRole.EDITOR));
        assertTrue(FileRole.MANAGER.includes(FileRole.VIEWER));
    }

    @Test
    void viewerShouldNotIncludeEditPermission() {
        assertFalse(FileRole.VIEWER.includes(FileRole.EDITOR));
    }

    @Test
    void maxShouldHandleNullAndReturnHigherRole() {
        assertNull(FileRole.max(null, null));
        assertEquals(FileRole.EDITOR, FileRole.max(null, FileRole.EDITOR));
        assertEquals(FileRole.MANAGER, FileRole.max(FileRole.MANAGER, FileRole.VIEWER));
    }
}
