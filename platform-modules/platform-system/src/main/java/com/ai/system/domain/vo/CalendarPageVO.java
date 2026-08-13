package com.ai.system.domain.vo;

import com.ai.system.domain.entity.Calendar;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 年度日历分页 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "年度日历分页VO")
@AutoMapper(target = Calendar.class)
public class CalendarPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "日历JSON")
    private String calendar;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
