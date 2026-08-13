package com.ai.sse.service.impl;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.sse.config.SseProperties;
import com.ai.sse.domain.dto.SsePushRecordDTO;
import com.ai.sse.domain.dto.SsePushRecordPageDTO;
import com.ai.sse.domain.entity.SsePushRecord;
import com.ai.sse.domain.vo.SsePushRecordPageVO;
import com.ai.sse.domain.vo.SsePushRecordVO;
import com.ai.sse.repository.SsePushRecordRepository;
import com.ai.sse.service.ISsePushRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SSE 推送日志服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsePushRecordServiceImpl
        extends BaseServiceImpl<SsePushRecordPageDTO, SsePushRecordPageVO, SsePushRecordVO, SsePushRecordDTO, SsePushRecordDTO, SsePushRecord>
        implements ISsePushRecordService {


    private final SsePushRecordRepository repository;
    private final SseProperties properties;

    @Override
    protected BaseRepository<SsePushRecordPageVO, SsePushRecordVO, SsePushRecord> getRepository() {
        return this.repository;
    }

    @Override
    protected Sort buildSort(SsePushRecordPageDTO pageDTO) {
        return Sort.by(Sort.Order.desc("pushTime"));
    }

    @Override
    protected Specification<SsePushRecord> buildQuerySpec(SsePushRecordPageDTO pageDTO) {
        if (pageDTO == null) return (root, query, cb) -> cb.conjunction();
        return SpecUtils.<SsePushRecord>builder()
                .eqIfPresent(SsePushRecord::getMessageId, pageDTO.getMessageId())
                .eqIfPresent(SsePushRecord::getTargetType, pageDTO.getTargetType())
                .eqIfPresent(SsePushRecord::getMessageType, pageDTO.getMessageType())
                .eqIfPresent(SsePushRecord::getUserId, pageDTO.getUserId())
                .eqIfPresent(SsePushRecord::getPushStatus, pageDTO.getPushStatus())
                .geTimeIfPresent(SsePushRecord::getPushTime, pageDTO.getStartTime())
                .leTimeIfPresent(SsePushRecord::getPushTime, pageDTO.getEndTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEntity(SsePushRecord entity) {
        repository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupHistory(int retainDays) {
        LocalDateTime cutoff = LocalDate.now().minusDays(retainDays).atStartOfDay();
        Page<SsePushRecord> page = repository.findAll(
                (root, query, cb) -> cb.lessThan(root.get("pushTime"), cutoff),
                PageRequest.of(0, properties.historyCleanupBatchSize()));
        List<SsePushRecord> oldRecords = page.getContent();
        if (!oldRecords.isEmpty()) {
            repository.deleteAll(oldRecords);
            log.info("[SSE-PushLog] 清理历史推送日志: count={}, before={}", oldRecords.size(), cutoff);
        }
        return oldRecords.size();
    }
}
