package com.ai.system.domain;

/**
 * 系统菜单类型。
 *
 * @since 1.0.0
 */
public enum MenuType {
    /** 只承载层级的目录。 */
    DIRECTORY("目录"),
    /** 可导航的页面菜单。 */
    MENU("菜单"),
    /** 只参与权限判断的按钮节点。 */
    BUTTON("按钮");

    private final String label;

    MenuType(String label) {
        this.label = label;
    }

    /** 返回中文展示名称。 */
    public String label() {
        return label;
    }

    /** 判断当前类型是否可进入导航树。 */
    public boolean isNavigation() {
        return this != BUTTON;
    }
}
