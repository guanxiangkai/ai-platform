package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Calendar;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 年度日历创建/更新 DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "年度日历DTO")
@AutoMapper(target = Calendar.class)
public record CalendarDTO(
        @Schema(description = "年份") Integer year,
        @Schema(description = "日历JSON") String calendar
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
