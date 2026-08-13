package com.ai.api.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 天气信息 DTO（API 契约层）。
 *
 * @param weatherDate 天气日期
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param province 省份
 * @param weatherCondition 天气状况
 * @param tempLow 最低温度，单位为摄氏度
 * @param tempHigh 最高温度，单位为摄氏度
 * @param temperature 当前温度，单位为摄氏度
 * @param humidity 湿度百分比
 * @param windDirection 风向
 * @param windPower 风力等级
 * @param weatherIcon 天气图标编码
 * @param aqi 空气质量指数
 * @param aqiLevel 空气质量等级
 * @param collectTime 数据采集时间，接口统一使用 {@code yyyy-MM-dd HH:mm:ss} 格式
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record WeatherInfoDTO(
        LocalDate weatherDate,
        String cityCode,
        String cityName,
        String province,
        String weatherCondition,
        BigDecimal tempLow,
        BigDecimal tempHigh,
        BigDecimal temperature,
        String humidity,
        String windDirection,
        String windPower,
        String weatherIcon,
        Integer aqi,
        String aqiLevel,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime collectTime
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
