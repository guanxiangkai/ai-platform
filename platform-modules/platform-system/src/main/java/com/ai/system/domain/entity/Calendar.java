package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 年度日历实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_calendar", comment = "日历表")
@Schema(description = "年度日历")
public class Calendar extends DataEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @Schema(description = "年份")
    @Column(name = "year", nullable = false, comment = "年份")
    private Integer year;

    /**
     * 日历 JSON
     */
    @Schema(description = "日历JSON")
    @Column(name = "calendar", columnDefinition = "TEXT", comment = "日历JSON")
    private String calendar;
}
