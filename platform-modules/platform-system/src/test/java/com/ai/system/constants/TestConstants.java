package com.ai.system.constants;

/**
 * System 模块测试常量
 */
public final class TestConstants {

    private TestConstants() {
        throw new UnsupportedOperationException("这是一个效用类，无法实例化");
    }

    /**
     * 部门服务测试常量
     */
    public static final class DeptConstants {

        public static final String USER_ID = "user-1";
        public static final String POST_DEPT_ID = "post-dept";
        public static final String USER_DEPT_ID = "user-dept";

        private DeptConstants() {
        }
    }
}
