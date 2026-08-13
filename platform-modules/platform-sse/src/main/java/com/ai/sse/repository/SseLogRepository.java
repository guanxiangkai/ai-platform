package com.ai.sse.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.sse.domain.entity.SseLog;
import com.ai.sse.domain.vo.SseLogPageVO;
import com.ai.sse.domain.vo.SseLogVO;
import org.springframework.stereotype.Repository;

/**
 * SSE 操作日志 Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface SseLogRepository extends BaseRepository<SseLogPageVO, SseLogVO, SseLog> {
}
