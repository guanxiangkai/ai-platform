package com.ai.agent.domain;

/**
 * 由服务端根据认证与会话上下文构造的可信智能体变量。
 *
 * <p>同名客户端变量必须丢弃，避免伪造租户、用户、部门、技能或响应模式上下文。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum AgentTrustedVariable {
    TENANT_ID("tenantId"),
    USER_ID("userId"),
    DEPT_ID("deptId"),
    DEPT_IDS("deptIds"),
    SKILL_ID("skillId"),
    SCOPE_TAG("scopeTag"),
    VOICE_INPUT_ACTIVE("voiceInputActive"),
    RESPONSE_MODE("responseMode");

    private final String key;

    AgentTrustedVariable(String key) {
        this.key = key;
    }

    /** 返回提供方请求中的稳定变量名。 */
    public String key() {
        return key;
    }

    /** 判断变量名是否由服务端管理。 */
    public static boolean contains(String key) {
        if (key == null) {
            return false;
        }
        for (AgentTrustedVariable variable : values()) {
            if (variable.key.equals(key)) {
                return true;
            }
        }
        return false;
    }
}
