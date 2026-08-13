package com.ai.sse.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.sse.domain.entity.SsePushRecord;
import com.ai.sse.domain.vo.SsePushRecordPageVO;
import com.ai.sse.domain.vo.SsePushRecordVO;
import org.springframework.stereotype.Repository;

/**
 * SSE 推送日志数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface SsePushRecordRepository extends BaseRepository<SsePushRecordPageVO, SsePushRecordVO, SsePushRecord> {
}
