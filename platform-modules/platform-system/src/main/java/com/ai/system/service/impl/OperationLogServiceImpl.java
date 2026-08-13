package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.OperationLogDTO;
import com.ai.system.domain.dto.OperationLogPageDTO;
import com.ai.system.domain.entity.OperationLog;
import com.ai.system.domain.vo.OperationLogPageVO;
import com.ai.system.domain.vo.OperationLogVO;
import com.ai.system.repository.OperationLogRepository;
import com.ai.system.service.IOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl
        extends BaseServiceImpl<OperationLogPageDTO, OperationLogPageVO, OperationLogVO, OperationLogDTO, OperationLogDTO, OperationLog>
        implements IOperationLogService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OperationLogServiceImpl.class);


    private final OperationLogRepository repository;

    @Override
    protected BaseRepository<OperationLogPageVO, OperationLogVO, OperationLog> getRepository() {
        return this.repository;
    }

    @Override
    protected Sort buildSort(OperationLogPageDTO pageDTO) {
        return Sort.by(Sort.Order.desc("logTime"));
    }

    @Override
    protected Specification<OperationLog> buildQuerySpec(OperationLogPageDTO pageDTO) {
        Specification<OperationLog> filters = pageDTO == null
                ? (root, query, cb) -> cb.conjunction()
                : SpecUtils.<OperationLog>builder()
                .eqIfPresent(OperationLog::getOperationTypeCode, pageDTO.getOperationTypeCode())
                .likeIfPresent(OperationLog::getUsername, pageDTO.getUsername())
                .likeIfPresent(OperationLog::getClientIp, pageDTO.getClientIp())
                .eqIfPresent(OperationLog::getStatus, pageDTO.getStatus())
                .likeIfPresent(OperationLog::getModule, pageDTO.getModule())
                .geTimeIfPresent(OperationLog::getLogTime, pageDTO.getStartTime())
                .leTimeIfPresent(OperationLog::getLogTime, pageDTO.getEndTime())
                .build();
        return filters;
    }

    @Override
    public PageResponse<OperationLogPageVO> list(OperationLogPageDTO query) {
        PageResponse<OperationLogPageVO> page = repository.findPageVo(
                query,
                buildQuerySpec(query).and(SuperAdminLogVisibility.specification()),
                buildSort(query)
        );
        translateList(page.records());
        return page;
    }

    @Override
    public OperationLogVO detail(String id) {
        OperationLog entity = requireEntity(id);
        SuperAdminLogVisibility.requireVisible(entity, getEntityName(), id);
        return super.detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEntity(OperationLog entity) {
        if (entity == null || !StringUtils.hasText(entity.getOperationId())) {
            throw new IllegalArgumentException("操作日志必须包含 operationId");
        }
        if (repository.existsByOperationId(entity.getOperationId())) {
            return;
        }
        LogEntityIds.ensureRequiredFields(entity);
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            if (!repository.existsByOperationId(entity.getOperationId())) {
                throw exception;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clear() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = repository.deleteByLogTimeBeforeExcludingUser(
                cutoff, PlatformSuperAdmin.USER_ID);
        log.info("清空操作日志完成，删除 {} 条（保留最近30天）", deleted);
        return true;
    }

    @Override
    public Map<String, Object> statistics(
            String startTime,
            String endTime
    ) {
        OperationLogPageDTO query = new OperationLogPageDTO();
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        Specification<OperationLog> visibleLogs = buildQuerySpec(query)
                .and(SuperAdminLogVisibility.specification());
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", repository.count(visibleLogs));
        result.put("successCount", repository.count(visibleLogs.and(statusIs("SUCCESS"))));
        result.put("failCount", repository.count(visibleLogs.and(statusIs("FAIL"))));
        return result;
    }

    private Specification<OperationLog> statusIs(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
