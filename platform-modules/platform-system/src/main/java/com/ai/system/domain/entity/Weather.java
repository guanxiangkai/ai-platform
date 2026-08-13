package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 天气信息实体
 * <p>
 * 由同步任务定时抓取并存储，前端通过工作台接口查询展示
 * </p>
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
@Table(name = "sys_weather", comment = "天气信息表")
public class Weather extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 天气日期
     */
    @Column(name = "weather_date", nullable = false, comment = "天气日期")
    private LocalDate weatherDate;

    /**
     * 城市编码
     */
    @Column(name = "city_code", nullable = false, length = 20, comment = "城市编码")
    private String cityCode;

    /**
     * 城市名称
     */
    @Column(name = "city_name", nullable = false, length = 50, comment = "城市名称")
    private String cityName;

    /**
     * 省份
     */
    @Column(name = "province", length = 50, comment = "省份")
    private String province;

    /**
     * 天气状况（晴、多云、阴、雨等）
     */
    @Column(name = "weather_condition", length = 50, comment = "天气状况")
    private String weatherCondition;

    /**
     * 最低温度（℃）
     */
    @Column(name = "temp_low", precision = 5, scale = 1, comment = "最低温度(℃)")
    private BigDecimal tempLow;

    /**
     * 最高温度（℃）
     */
    @Column(name = "temp_high", precision = 5, scale = 1, comment = "最高温度(℃)")
    private BigDecimal tempHigh;

    /**
     * 当前温度（℃）
     */
    @Column(name = "temperature", precision = 5, scale = 1, comment = "当前温度(℃)")
    private BigDecimal temperature;

    /**
     * 湿度（%）
     */
    @Column(name = "humidity", length = 10, comment = "湿度(%)")
    private String humidity;

    /**
     * 风向
     */
    @Column(name = "wind_direction", length = 20, comment = "风向")
    private String windDirection;

    /**
     * 风力等级
     */
    @Column(name = "wind_power", length = 20, comment = "风力等级")
    private String windPower;

    /**
     * 天气图标编码
     */
    @Column(name = "weather_icon", length = 50, comment = "天气图标编码")
    private String weatherIcon;

    /**
     * 空气质量指数
     */
    @Column(name = "aqi", comment = "空气质量指数")
    private Integer aqi;

    /**
     * 空气质量等级
     */
    @Column(name = "aqi_level", length = 20, comment = "空气质量等级")
    private String aqiLevel;

    /**
     * 数据来源
     */
    @Column(name = "data_source", length = 50, comment = "数据来源")
    private String dataSource;

    /**
     * 数据采集时间
     */
    @Column(name = "collect_time", comment = "数据采集时间")
    private LocalDateTime collectTime;
}
