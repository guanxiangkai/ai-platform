package com.ai.agent.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 智能体调用的会话历史与变量资源上限。
 *
 * <p>这些限制同时约束上游模型请求大小和服务端校验成本，调整时应结合模型上下文窗口、
 * 并发量与内存预算评估，不应作为业务参数由请求方传入。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.agent.invocation")
public class AgentInvocationProperties {

    /** 单次提供方调用占用会话的执行租约。 */
    @NotNull
    private Duration executionLease = Duration.ofMinutes(15);

    /** 一次调用携带的最大历史消息数。 */
    @Min(1)
    @Max(200)
    private int maxHistoryMessages = 20;

    /** 一次调用携带的历史消息字符总数上限。 */
    @Min(1)
    @Max(1_000_000)
    private int maxHistoryCharacters = 24_000;

    /** 客户端可提交的自定义变量数量上限。 */
    @Min(0)
    @Max(256)
    private int maxVariableCount = 32;

    /** 自定义变量名称的字符数上限。 */
    @Min(1)
    @Max(256)
    private int maxVariableNameLength = 64;

    /** 自定义变量对象与数组的最大嵌套深度。 */
    @Min(1)
    @Max(16)
    private int maxVariableDepth = 4;

    /** 单个变量对象或数组的成员数量上限。 */
    @Min(1)
    @Max(1_000)
    private int maxVariableCollectionSize = 64;

    /** 单个文本变量的字符数上限。 */
    @Min(1)
    @Max(1_000_000)
    private int maxVariableTextLength = 4_000;

    /** 所有自定义变量名称和内容的累计字符数上限。 */
    @Min(1)
    @Max(4_000_000)
    private int maxVariableTotalSize = 16_000;

    /** 技能展示与授权选择器一次最多返回的记录数。 */
    @Min(1)
    @Max(1_000)
    private int skillSelectionLimit = 200;

    /** 校验执行租约可覆盖正常上游超时，且不会无限占用会话。 */
    @AssertTrue(message = "智能体调用执行租约必须不少于 12 分钟且不超过 30 分钟")
    public boolean isExecutionLeaseWithinBounds() {
        return executionLease != null
                && executionLease.compareTo(Duration.ofMinutes(12)) >= 0
                && executionLease.compareTo(Duration.ofMinutes(30)) <= 0;
    }
}
