package com.ai.files.domain;

/** 文件访问角色，权限从查看到管理逐级增加。 */
public enum FileRole {
    /** 可查看和下载。 */
    VIEWER(10),
    /** 可查看并上传原件，不可重命名、移动、回收或管理目录。 */
    UPLOADER(15),
    /** 可查看、上传版本、创建、重命名和移动。 */
    EDITOR(20),
    /** 可执行全部编辑操作并管理授权。 */
    MANAGER(30);

    private final int permissionLevel;

    FileRole(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    /**
     * 判断当前角色是否覆盖目标角色。
     *
     * @param required 目标角色
     * @return 当前角色权限是否足够
     */
    public boolean includes(FileRole required) {
        return permissionLevel >= required.permissionLevel;
    }

    /**
     * 返回权限更高的角色。
     *
     * @param left 左侧角色，可为空
     * @param right 右侧角色，可为空
     * @return 权限较高的角色
     */
    public static FileRole max(FileRole left, FileRole right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.permissionLevel >= right.permissionLevel ? left : right;
    }
}
