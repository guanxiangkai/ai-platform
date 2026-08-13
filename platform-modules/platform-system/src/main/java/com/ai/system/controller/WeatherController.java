package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.system.domain.vo.WeatherVO;
import com.ai.system.service.IWeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 天气信息控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "天气信息", description = "天气数据查询（数据由同步任务写入）")
@RestController
@RequestMapping("/system/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final IWeatherService service;

    /**
     * 获取指定城市今日天气
     */
    @Operation(summary = "获取今日天气", description = "根据城市编码获取今日最新一条天气记录")
    @GetMapping("/today")
    public ApiResponse<WeatherVO> getToday(
            @Parameter(description = "城市编码", required = true) @RequestParam String cityCode) {
        return ApiResponse.ok(service.getToday(cityCode));
    }

    /**
     * 获取天气预报
     */
    @Operation(summary = "获取天气预报", description = "获取指定城市未来N天天气预报")
    @GetMapping("/forecast")
    public ApiResponse<List<WeatherVO>> getForecast(
            @Parameter(description = "城市编码", required = true) @RequestParam String cityCode,
            @Parameter(description = "天数（默认8天，最多15天）") @RequestParam(defaultValue = "8") int days) {
        return ApiResponse.ok(service.getForecast(cityCode, days));
    }
}
