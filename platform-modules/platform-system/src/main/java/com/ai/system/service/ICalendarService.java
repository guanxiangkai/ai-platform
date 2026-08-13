package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.CalendarDTO;
import com.ai.system.domain.dto.CalendarPageDTO;
import com.ai.system.domain.entity.Calendar;
import com.ai.system.domain.vo.CalendarPageVO;
import com.ai.system.domain.vo.CalendarVO;

/**
 * 年度日历服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ICalendarService extends IBaseService<CalendarPageDTO, CalendarPageVO, CalendarVO, CalendarDTO, CalendarDTO, Calendar> {

    /**
     * 根据年份查询年度日历
     *
     * @param year 年份
     * @return 年度日历
     */
    CalendarVO getByYear(Integer year);
}
