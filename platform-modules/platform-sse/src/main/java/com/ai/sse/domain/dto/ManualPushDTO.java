package com.ai.sse.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 手动推送 DTO
 * <p>
 * 运维人员通过管理后台手动构建消息并发送到 MQ，
 * 由 SSE 消费端推送给目标用户。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "手动推送DTO")
public record ManualPushDTO(
        @NotBlank(message = "推送目标类型不能为空")
        @Schema(description = "推送目标类型(USER/USERS/TENANT/BROADCAST)", requiredMode = Schema.RequiredMode.REQUIRED)
        String targetType,

        @Schema(description = "目标用户ID(单用户推送时必填)")
        String userId,

        @Schema(description = "目标用户ID列表(多用户推送时必填)")
        List<String> userIds,

        @Schema(description = "租户ID(租户广播时必填)")
        String tenantId,

        @NotBlank(message = "消息类型不能为空")
        @Schema(description = "消息类型(字典: sse_message_type)", requiredMode = Schema.RequiredMode.REQUIRED)
        String messageType,

        @Schema(description = "消息标题")
        String title,

        @NotBlank(message = "消息内容不能为空")
        @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
        String content
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
