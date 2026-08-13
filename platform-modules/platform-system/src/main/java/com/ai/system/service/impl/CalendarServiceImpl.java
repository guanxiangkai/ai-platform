package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.domain.dto.CalendarDTO;
import com.ai.system.domain.dto.CalendarPageDTO;
import com.ai.system.domain.entity.Calendar;
import com.ai.system.domain.vo.CalendarPageVO;
import com.ai.system.domain.vo.CalendarVO;
import com.ai.system.repository.CalendarRepository;
import com.ai.system.service.ICalendarService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 年度日历服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl extends BaseServiceImpl<CalendarPageDTO, CalendarPageVO, CalendarVO, CalendarDTO, CalendarDTO, Calendar>
        implements ICalendarService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CalendarServiceImpl.class);


    private final CalendarRepository repository;
    private final Converter converter;

    @Override
    protected BaseRepository<CalendarPageVO, CalendarVO, Calendar> getRepository() {
        return this.repository;
    }

    @Override
    protected Specification<Calendar> buildQuerySpec(CalendarPageDTO pageDTO) {
        if (pageDTO == null) {
            return null;
        }
        return SpecUtils.<Calendar>builder()
                .eqIfPresent(Calendar::getYear, pageDTO.getYear())
                .build();
    }

    @Override
    public CalendarVO getByYear(Integer year) {
        validateYear(year);
        Calendar calendar = repository.findByYearAndDeletedFalse(year)
                .orElseThrow(() -> new BizException("该年份日历不存在：" + year));
        return converter.convert(calendar, CalendarVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CalendarDTO dto) {
        Integer year = requireYear(dto);
        if (repository.existsByYearAndDeletedFalse(year)) {
            throw new BizException("该年份日历已存在：" + year);
        }
        Calendar calendar = converter.convert(dto, Calendar.class);
        Calendar saved = repository.save(calendar);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, CalendarDTO dto) {
        Integer year = requireYear(dto);
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new BizException("年度日历不存在"));
        if (!year.equals(existing.getYear()) && repository.existsByYearAndDeletedFalse(year)) {
            throw new BizException("该年份日历已存在：" + year);
        }
        existing.setYear(year);
        existing.setCalendar(dto.calendar());
        repository.save(existing);
    }

    private Integer requireYear(CalendarDTO dto) {
        if (dto == null) {
            throw new BizException("年度日历参数不能为空");
        }
        validateYear(dto.year());
        return dto.year();
    }

    private void validateYear(Integer year) {
        if (year == null) {
            throw new BizException("年份不能为空");
        }
    }
}
