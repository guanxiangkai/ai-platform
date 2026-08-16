package com.ai.system.integration;

/**
 * 租户业务服务返回的人员档案匹配结果。
 *
 * @param personnelId 人员档案 ID；未匹配时为空
 * @param personnelName 人员姓名
 * @param alreadyHasAccount 是否已经绑定平台账户
 */
public record WorkforceMatchResult(
        String personnelId,
        String personnelName,
        boolean alreadyHasAccount
) {
}
