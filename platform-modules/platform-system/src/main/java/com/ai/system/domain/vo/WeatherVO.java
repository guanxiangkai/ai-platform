package com.ai.system.domain.vo;

import com.ai.system.domain.entity.Weather;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 天气信息VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "天气信息VO")
@AutoMapper(target = Weather.class)
public class WeatherVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "天气日期")
    private LocalDate weatherDate;

    @Schema(description = "城市编码")
    private String cityCode;

    @Schema(description = "城市名称")
    private String cityName;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "天气状况")
    private String weatherCondition;

    @Schema(description = "最低温度(℃)")
    private BigDecimal tempLow;

    @Schema(description = "最高温度(℃)")
    private BigDecimal tempHigh;

    @Schema(description = "当前温度(℃)")
    private BigDecimal temperature;

    @Schema(description = "湿度(%)")
    private String humidity;

    @Schema(description = "风向")
    private String windDirection;

    @Schema(description = "风力等级")
    private String windPower;

    @Schema(description = "天气图标编码")
    private String weatherIcon;

    @Schema(description = "空气质量指数")
    private Integer aqi;

    @Schema(description = "空气质量等级")
    private String aqiLevel;

    @Schema(description = "数据采集时间")
    private LocalDateTime collectTime;
}
