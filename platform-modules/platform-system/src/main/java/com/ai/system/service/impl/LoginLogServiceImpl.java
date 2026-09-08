package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.LoginLogDTO;
import com.ai.system.domain.dto.LoginLogPageDTO;
import com.ai.system.domain.entity.LoginLog;
import com.ai.system.domain.vo.LoginLogPageVO;
import com.ai.system.domain.vo.LoginLogVO;
import com.ai.system.repository.LoginLogRepository;
import com.ai.system.service.ILoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录日志服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLogServiceImpl
        extends BaseServiceImpl<LoginLogPageDTO, LoginLogPageVO, LoginLogVO, LoginLogDTO, LoginLogDTO, LoginLog>
        implements ILoginLogService {


    private final LoginLogRepository repository;

    @Override
    protected BaseRepository<LoginLogPageVO, LoginLogVO, LoginLog> getRepository() {
        return this.repository;
    }

    @Override
    protected Sort buildSort(LoginLogPageDTO pageDTO) {
        return Sort.by(Sort.Order.desc("logTime"));
    }

    @Override
    protected Specification<LoginLog> buildQuerySpec(LoginLogPageDTO pageDTO) {
        Specification<LoginLog> filters = pageDTO == null
                ? (root, query, cb) -> cb.conjunction()
                : SpecUtils.<LoginLog>builder()
                .eqIfPresent(LoginLog::getAction, pageDTO.getAction())
                .likeIfPresent(LoginLog::getUsername, pageDTO.getUsername())
                .likeIfPresent(LoginLog::getClientIp, pageDTO.getClientIp())
                .eqIfPresent(LoginLog::getStatus, pageDTO.getStatus())
                .geTimeIfPresent(LoginLog::getLogTime, pageDTO.getStartTime())
                .leTimeIfPresent(LoginLog::getLogTime, pageDTO.getEndTime())
                .build();
        return filters;
    }

    @Override
    public PageResponse<LoginLogPageVO> list(LoginLogPageDTO query) {
        PageResponse<LoginLogPageVO> page = repository.findPageVo(
                query,
                buildQuerySpec(query).and(SuperAdminLogVisibility.specification()),
                buildSort(query)
        );
        translateList(page.records());
        return page;
    }

    @Override
    protected LoginLog requireEntity(String id) {
        LoginLog entity = super.requireEntity(id);
        SuperAdminLogVisibility.requireVisible(entity, getEntityName(), id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEntity(LoginLog entity) {
        LogEntityIds.ensureRequiredFields(entity);
        normalizeLengths(entity);
        repository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clear() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = repository.deleteByLogTimeBeforeExcludingUser(
                cutoff, PlatformSuperAdmin.USER_ID);
        log.info("清空登录日志完成，删除 {} 条（保留最近30天）", deleted);
        return true;
    }

    private void normalizeLengths(LoginLog entity) {
        entity.setTraceId(truncate(entity.getTraceId(), 64));
        entity.setUserId(truncate(entity.getUserId(), 64));
        entity.setUsername(truncate(entity.getUsername(), 64));
        entity.setClientIp(truncate(entity.getClientIp(), 50));
        entity.setLocation(truncate(entity.getLocation(), 100));
        entity.setStatus(truncate(entity.getStatus(), 20));
        entity.setMessage(truncate(entity.getMessage(), 500));
        entity.setTenantId(truncate(entity.getTenantId(), 64));
        entity.setCreateBy(truncate(entity.getCreateBy(), 64));
        entity.setUpdateBy(truncate(entity.getUpdateBy(), 64));
        entity.setAction(truncate(entity.getAction(), 50));
        entity.setUserAgent(truncate(entity.getUserAgent(), 500));
        entity.setBrowser(truncate(entity.getBrowser(), 100));
        entity.setOs(truncate(entity.getOs(), 100));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
