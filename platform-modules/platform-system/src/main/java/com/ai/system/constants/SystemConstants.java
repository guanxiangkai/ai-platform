package com.ai.system.constants;

/**
 * System 模块常量
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class SystemConstants {

    private SystemConstants() {
        throw new UnsupportedOperationException("这是一个效用类，无法实例化");
    }

    /**
     * 注册相关常量
     */
    public static final class RegisterConstants {

        /**
         * 用户名随机后缀字符集（小写字母 + 数字）
         */
        public static final String USERNAME_SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

        private RegisterConstants() {
        }
    }

    /**
     * 缓存相关常量
     */
    public static final class CacheConstants {

        /**
         * 系统字典三级缓存名称
         */
        public static final String DICT_CACHE_NAME = "sys:dict";

        /**
         * 用户授权范围三级缓存名称
         */
        public static final String AUTHORIZATION_SCOPE_CACHE_NAME = "sys:authorization:scope";

        private CacheConstants() {
        }
    }

    /**
     * 字典类型常量
     */
    public static final class DictTypeConstants {

        public static final String SYS_GENDER = "SYS_GENDER";
        public static final String SYS_USER_TYPE = "SYS_USER_TYPE";
        public static final String SYS_COMMON_STATUS = "sys_common_status";
        public static final String SYS_OPERATION_TYPE = "sys_operation_type";
        public static final String SYS_MESSAGE_TYPE = "sys_message_type";
        public static final String SYS_MESSAGE_PRIORITY = "sys_message_priority";
        public static final String SYS_BUSINESS_TYPE = "sys_business_type";
        public static final String SYS_DATA_SCOPE = "sys_data_scope";

        private DictTypeConstants() {
        }
    }

    /**
     * 用户相关常量
     */
    public static final class UserConstants {

        public static final String TYPE_ADMIN = "ADMIN";
        public static final String TYPE_USER = "USER";
        private UserConstants() {
        }
    }

    /**
     * 日志相关常量
     */
    public static final class LogConstants {

        public static final String DEFAULT_TENANT_ID = "0";

        private LogConstants() {
        }
    }

    /**
     * 平台接口日期时间格式。
     */
    public static final class DateTimeConstants {

        /** 日期时间序列化格式。 */
        public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

        /** 平台默认业务时区。 */
        public static final String TIME_ZONE = "Asia/Shanghai";

        private DateTimeConstants() {
        }
    }

    /**
     * SSE 相关常量
     */
    public static final class SseConstants {

        public static final String TOPIC_NOTIFICATION = "sse.notification";
        public static final String TARGET_TYPE_USER = "USER";
        public static final String TYPE_UNREAD_COUNT = "unreadCount";
        public static final String TYPE_NOTICES = "notices";

        private SseConstants() {
        }
    }

    /**
     * 区域相关常量
     */
    public static final class RegionConstants {

        public static final String REGION_TYPE_CITY = "city";
        public static final String REGION_TYPE_PROVINCE = "province";

        private RegionConstants() {
        }
    }

    /**
     * 菜单相关常量
     */
    public static final class MenuConstants {

        public static final String ROOT_PARENT_ID = "0";

        private MenuConstants() {
        }
    }
}
