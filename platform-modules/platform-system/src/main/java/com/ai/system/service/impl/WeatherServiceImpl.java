package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.entity.Weather;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.domain.vo.WeatherVO;
import com.ai.system.repository.RegionRepository;
import com.ai.system.repository.WeatherRepository;
import com.ai.system.service.IDeptService;
import com.ai.system.service.IWeatherService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * 天气信息服务实现
 * <p>
 * 提供天气数据查询能力，天气数据由外部同步任务写入。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements IWeatherService {


    private final WeatherRepository repository;
    private final RegionRepository regionRepository;
    private final IDeptService deptService;
    private final Converter converter;

    @Override
    public WeatherVO getToday(String cityCode) {
        LocalDate today = LocalDate.now();
        Weather weather = findTodayByCodeWithAliases(cityCode, today)
                .or(() -> findTodayFromAncestors(cityCode, today))
                .orElseThrow(() -> new BizException("未查询到城市今日天气数据：" + cityCode));
        return converter.convert(weather, WeatherVO.class);
    }

    @Override
    public WeatherVO getTodayByDept(String deptId) {
        if (!StringUtils.hasText(deptId)) {
            throw new BizException("部门ID不能为空");
        }
        RegionVO region = deptService.getDeptRegion(deptId);
        WeatherVO weather = getToday(region.getRegionCode());
        String displayName = regionDisplayName(region);
        if (StringUtils.hasText(displayName)) {
            weather.setCityName(displayName);
        }
        return weather;
    }

    @Override
    public List<WeatherVO> getForecast(String cityCode, int days) {
        if (days <= 0 || days > 15) {
            days = 7;
        }
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days - 1);
        List<Weather> weathers = repository.findByCityCodeAndWeatherDateBetweenAndDeletedFalseOrderByWeatherDateAsc(
                cityCode, today, endDate);
        if (weathers.isEmpty()) {
            weathers = findAncestorForecast(cityCode, today, endDate);
        }
        return converter.convert(weathers, WeatherVO.class);
    }


    private List<Weather> findAncestorForecast(String cityCode, LocalDate startDate, LocalDate endDate) {
        Region current = regionRepository.findByRegionCodeAndDeletedFalse(cityCode).orElse(null);
        while (current != null && isBelowCity(current)) {
            current = findParent(current);
            if (current == null) {
                return List.of();
            }

            List<Weather> weathers = repository.findByCityCodeAndWeatherDateBetweenAndDeletedFalseOrderByWeatherDateAsc(
                    current.getRegionCode(), startDate, endDate);
            if (!weathers.isEmpty()) {
                return weathers;
            }

            if (isCityLevel(current)) {
                break;
            }
        }
        return List.of();
    }

    private Optional<Weather> findTodayFromAncestors(String cityCode, LocalDate weatherDate) {
        Region current = regionRepository.findByRegionCodeAndDeletedFalse(cityCode).orElse(null);
        if (current == null || isProvinceLevel(current)) {
            return Optional.empty();
        }

        Set<String> visitedRegionIds = new HashSet<>();
        while (current != null && !isProvinceLevel(current)) {
            if (!visitedRegionIds.add(current.getId())) {
                log.warn("区域层级存在循环，停止天气回溯，cityCode={}", cityCode);
                return Optional.empty();
            }

            current = findParent(current);
            if (current == null) {
                return Optional.empty();
            }

            Optional<Weather> weather = findTodayByCodeWithAliases(current.getRegionCode(), weatherDate);
            if (weather.isPresent()) {
                return weather;
            }
        }
        return Optional.empty();
    }

    private Optional<Weather> findTodayByCodeWithAliases(String cityCode, LocalDate weatherDate) {
        if (!StringUtils.hasText(cityCode)) {
            return Optional.empty();
        }
        for (String code : buildWeatherCodeCandidates(cityCode)) {
            Optional<Weather> weather = repository
                    .findTopByCityCodeAndWeatherDateAndDeletedFalseOrderByCollectTimeDesc(code, weatherDate);
            if (weather.isPresent()) {
                return weather;
            }
        }
        return Optional.empty();
    }

    private List<String> buildWeatherCodeCandidates(String cityCode) {
        String code = cityCode.trim();
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(code);

        // Some weather sources store prefecture-level code as 4 digits, e.g. 320200 -> 3202.
        if (code.length() >= 6) {
            candidates.add(code.substring(0, 4));
        }
        return new ArrayList<>(candidates);
    }

    private String regionDisplayName(RegionVO region) {
        return firstText(region.getFullName(), region.getRegionName());
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Region findParent(Region region) {
        if (!StringUtils.hasText(region.getParentId()) || "0".equals(region.getParentId())) {
            return null;
        }
        return regionRepository.findById(region.getParentId()).orElse(null);
    }

    private boolean isBelowCity(Region region) {
        String type = region.getRegionLevel();
        if (!StringUtils.hasText(type)) {
            return false;
        }
        String normalized = type.trim().toLowerCase();
        return !SystemConstants.RegionConstants.REGION_TYPE_CITY.equals(normalized)
                && !SystemConstants.RegionConstants.REGION_TYPE_PROVINCE.equals(normalized);
    }

    private boolean isCityLevel(Region region) {
        return StringUtils.hasText(region.getRegionLevel())
                && SystemConstants.RegionConstants.REGION_TYPE_CITY.equalsIgnoreCase(region.getRegionLevel().trim());
    }

    private boolean isProvinceLevel(Region region) {
        return StringUtils.hasText(region.getRegionLevel())
                && SystemConstants.RegionConstants.REGION_TYPE_PROVINCE.equalsIgnoreCase(region.getRegionLevel().trim());
    }
}
