package com.ai.sse.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.domain.vo.SseConnectionRecordPageVO;
import com.ai.sse.domain.vo.SseConnectionRecordVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SSE 连接记录数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface SseConnectionRecordRepository
        extends BaseRepository<SseConnectionRecordPageVO, SseConnectionRecordVO, SseConnectionRecord> {

    /**
     * 根据连接 ID 查询（用于断开时更新）
     */
    Optional<SseConnectionRecord> findByConnectionIdAndDeletedFalse(String connectionId);

    /**
     * 查询用户的连接记录（按连接时间倒序）
     */
    List<SseConnectionRecord> findByUserIdAndDeletedFalseOrderByConnectTimeDesc(String userId);

    /**
     * 查询当前活跃连接（connectionStatus = connected）
     */
    List<SseConnectionRecord> findByConnectionStatusAndDeletedFalse(String connectionStatus);

    /**
     * 按状态统计记录数
     */
    Long countByConnectionStatusAndDeletedFalse(String connectionStatus);
}
