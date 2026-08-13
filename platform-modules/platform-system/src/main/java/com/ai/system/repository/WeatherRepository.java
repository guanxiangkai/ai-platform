package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import com.ai.system.domain.entity.Weather;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 天气信息数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface WeatherRepository extends JpaPlusRepository<Weather, String> {

    /**
     * 查询指定城市当日最新天气（按采集时间倒序）
     *
     * @param cityCode    城市编码
     * @param weatherDate 天气日期
     * @return 天气信息
     */
    Optional<Weather> findTopByCityCodeAndWeatherDateAndDeletedFalseOrderByCollectTimeDesc(String cityCode, LocalDate weatherDate);


    /**
     * 查询指定城市日期范围内的天气
     *
     * @param cityCode  城市编码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 天气列表
     */
    List<Weather> findByCityCodeAndWeatherDateBetweenAndDeletedFalseOrderByWeatherDateAsc(
            String cityCode, LocalDate startDate, LocalDate endDate);
}
