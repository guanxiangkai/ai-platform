package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.system.domain.dto.CalendarDTO;
import com.ai.system.domain.dto.CalendarPageDTO;
import com.ai.system.domain.entity.Calendar;
import com.ai.system.domain.vo.CalendarPageVO;
import com.ai.system.domain.vo.CalendarVO;
import com.ai.system.service.ICalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 年度日历控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "年度日历", description = "年度日历的增删改查与按年份查询")
@RestController
@RequestMapping("/system/calendar")
@RequiredArgsConstructor
public class CalendarController extends BaseController<CalendarPageDTO, CalendarPageVO, CalendarVO, CalendarDTO, CalendarDTO, Calendar> {

    private final ICalendarService service;

    @Override
    protected IBaseService<CalendarPageDTO, CalendarPageVO, CalendarVO, CalendarDTO, CalendarDTO, Calendar> getService() {
        return this.service;
    }

    /**
     * 根据年份查询年度日历
     */
    @Operation(summary = "根据年份查询年度日历", description = "查询指定年份的日历JSON配置")
    @GetMapping("/year")
    public ApiResponse<CalendarVO> getByYear(
            @Parameter(description = "年份", required = true) @RequestParam Integer year) {
        return ApiResponse.ok(service.getByYear(year));
    }
}
