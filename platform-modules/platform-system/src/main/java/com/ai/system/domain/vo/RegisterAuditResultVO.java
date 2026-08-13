package com.ai.system.domain.vo;

import com.ai.system.domain.RegisterState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 注册审核状态结果。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "注册审核状态结果")
public record RegisterAuditResultVO(
        @Schema(description = "注册记录ID") String id,
        @Schema(description = "注册状态；仅 ACTIVE 表示账户已可使用") RegisterState state
) {
}
