package com.ai.system.service.impl;

import com.ai.system.domain.entity.Weather;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.domain.vo.WeatherVO;
import com.ai.system.repository.RegionRepository;
import com.ai.system.repository.WeatherRepository;
import com.ai.system.service.IDeptService;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherServiceImplTest {

    @Test
    void getTodayByDeptShouldUseDeptRegionAndDisplayFullRegionName() {
        WeatherRepository weatherRepository = mock(WeatherRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IDeptService deptService = mock(IDeptService.class);
        Converter converter = mock(Converter.class);
        WeatherServiceImpl service = new WeatherServiceImpl(
                weatherRepository,
                regionRepository,
                deptService,
                converter
        );

        RegionVO region = new RegionVO();
        region.setRegionCode("101190206");
        region.setRegionName("滨湖区");
        region.setFullName("示例市示例区");
        Weather weather = new Weather();
        weather.setCityCode("101190206");
        weather.setCityName("滨湖区");
        WeatherVO weatherVO = new WeatherVO();
        weatherVO.setCityCode("101190206");
        weatherVO.setCityName("滨湖区");

        when(deptService.getDeptRegion("dept-1")).thenReturn(region);
        when(weatherRepository.findTopByCityCodeAndWeatherDateAndDeletedFalseOrderByCollectTimeDesc(
                eq("101190206"), eq(LocalDate.now())
        )).thenReturn(Optional.of(weather));
        when(converter.convert(weather, WeatherVO.class)).thenReturn(weatherVO);

        WeatherVO result = service.getTodayByDept("dept-1");

        assertThat(result.getCityCode()).isEqualTo("101190206");
        assertThat(result.getCityName()).isEqualTo("示例市示例区");
    }
}
