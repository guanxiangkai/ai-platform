package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Calendar;
import com.ai.system.domain.vo.CalendarPageVO;
import com.ai.system.domain.vo.CalendarVO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 年度日历数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface CalendarRepository extends BaseRepository<CalendarPageVO, CalendarVO, Calendar> {

    /**
     * 根据年份查询日历
     *
     * @param year 年份
     * @return 年度日历
     */
    Optional<Calendar> findByYearAndDeletedFalse(Integer year);

    /**
     * 检查年份是否已存在
     *
     * @param year 年份
     * @return 是否存在
     */
    boolean existsByYearAndDeletedFalse(Integer year);
}
