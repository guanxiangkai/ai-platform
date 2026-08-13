package com.ai.system.controller.internal;

import com.ai.api.system.dto.WeatherInfoDTO;
import com.ai.system.domain.vo.WeatherVO;
import com.ai.system.service.IWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 天气内部接口（仅供微服务间 RPC 调用）。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/weather")
@RequiredArgsConstructor
public class InternalWeatherController {

    private final IWeatherService weatherService;

    /**
     * 根据部门关联区域查询今日最新天气。
     */
    @GetMapping("/today-by-dept")
    public WeatherInfoDTO todayByDept(@RequestParam("deptId") String deptId) {
        return toDto(weatherService.getTodayByDept(deptId));
    }

    private WeatherInfoDTO toDto(WeatherVO weather) {
        return new WeatherInfoDTO(
                weather.getWeatherDate(),
                weather.getCityCode(),
                weather.getCityName(),
                weather.getProvince(),
                weather.getWeatherCondition(),
                weather.getTempLow(),
                weather.getTempHigh(),
                weather.getTemperature(),
                weather.getHumidity(),
                weather.getWindDirection(),
                weather.getWindPower(),
                weather.getWeatherIcon(),
                weather.getAqi(),
                weather.getAqiLevel(),
                weather.getCollectTime()
        );
    }
}
