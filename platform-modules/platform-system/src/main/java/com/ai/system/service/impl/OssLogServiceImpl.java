package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.OssLogDTO;
import com.ai.system.domain.dto.OssLogPageDTO;
import com.ai.system.domain.entity.OssLog;
import com.ai.system.domain.vo.OssLogPageVO;
import com.ai.system.domain.vo.OssLogVO;
import com.ai.system.repository.OssLogRepository;
import com.ai.system.service.IOssLogService;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;

/**
 * OSS 文件上传日志服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OssLogServiceImpl
        extends BaseServiceImpl<OssLogPageDTO, OssLogPageVO, OssLogVO, OssLogDTO, OssLogDTO, OssLog>
        implements IOssLogService {


    private final OssLogRepository repository;

    @Override
    protected BaseRepository<OssLogPageVO, OssLogVO, OssLog> getRepository() {
        return this.repository;
    }

    @Override
    protected Sort buildSort(OssLogPageDTO pageDTO) {
        return Sort.by(Sort.Order.desc("logTime"));
    }

    @Override
    protected Specification<OssLog> buildQuerySpec(OssLogPageDTO pageDTO) {
        Specification<OssLog> filters = pageDTO == null
                ? (root, query, cb) -> cb.conjunction()
                : SpecUtils.<OssLog>builder()
                .likeIfPresent(OssLog::getUsername, pageDTO.getUsername())
                .likeIfPresent(OssLog::getClientIp, pageDTO.getClientIp())
                .eqIfPresent(OssLog::getStatus, pageDTO.getStatus())
                .likeIfPresent(OssLog::getOriginalName, pageDTO.getOriginalName())
                .eqIfPresent(OssLog::getFileSuffix, pageDTO.getFileSuffix())
                .eqIfPresent(OssLog::getBizModule, pageDTO.getBizModule())
                .geTimeIfPresent(OssLog::getLogTime, pageDTO.getStartTime())
                .leTimeIfPresent(OssLog::getLogTime, pageDTO.getEndTime())
                .build();
        return filters;
    }

    @Override
    public PageResponse<OssLogPageVO> list(OssLogPageDTO query) {
        PageResponse<OssLogPageVO> page = repository.findPageVo(
                query,
                buildQuerySpec(query).and(SuperAdminLogVisibility.specification()),
                buildSort(query)
        );
        translateList(page.records());
        return page;
    }

    @Override
    protected OssLog requireEntity(String id) {
        OssLog entity = super.requireEntity(id);
        SuperAdminLogVisibility.requireVisible(entity, getEntityName(), id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEntity(OssLog entity) {
        LogEntityIds.ensureRequiredFields(entity);
        repository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clear() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = repository.deleteByLogTimeBeforeExcludingUser(
                cutoff, PlatformSuperAdmin.USER_ID);
        log.info("清空上传日志完成，删除 {} 条（保留最近30天）", deleted);
        return true;
    }
}
