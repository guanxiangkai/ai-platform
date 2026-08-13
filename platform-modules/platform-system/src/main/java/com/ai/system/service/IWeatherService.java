package com.ai.system.service;

import com.ai.system.domain.vo.WeatherVO;

import java.util.List;

/**
 * 天气信息服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IWeatherService {

    /**
     * 获取指定城市当天天气
     *
     * @param cityCode 城市编码
     * @return 天气VO
     */
    WeatherVO getToday(String cityCode);

    /**
     * 根据部门关联区域获取当天天气。
     *
     * @param deptId 部门ID
     * @return 天气VO
     */
    WeatherVO getTodayByDept(String deptId);

    /**
     * 获取指定城市未来几天天气预报
     *
     * @param cityCode 城市编码
     * @param days     天数
     * @return 天气VO列表
     */
    List<WeatherVO> getForecast(String cityCode, int days);
}
