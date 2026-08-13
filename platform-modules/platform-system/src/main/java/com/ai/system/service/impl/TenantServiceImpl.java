package com.ai.system.service.impl;

import com.ai.system.config.SystemQueryProperties;
import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import com.ai.system.domain.dto.TenantDTO;
import com.ai.system.domain.dto.TenantPageDTO;
import com.ai.system.domain.entity.Tenant;
import com.ai.system.domain.vo.TenantPageVO;
import com.ai.system.domain.vo.TenantVO;
import com.ai.system.repository.TenantRepository;
import com.ai.system.service.ITenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 租户服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl extends BaseServiceImpl<TenantPageDTO, TenantPageVO, TenantVO, TenantDTO, TenantDTO, Tenant> implements ITenantService {


    private final TenantRepository repository;
    private final SystemQueryProperties queryProperties;

    @Override
    protected BaseRepository<TenantPageVO, TenantVO, Tenant> getRepository() {
        return this.repository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(TenantDTO dto) {
        // 检查租户编码是否重复
        if (StringUtils.hasText(dto.tenantCode()) &&
                repository.existsByTenantCodeAndDeletedFalse(dto.tenantCode())) {
            throw new BizException("租户编码已存在");
        }

        Tenant tenant = EntityConverter.toEntity(dto, Tenant.class);
        Tenant saved = repository.save(tenant);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, TenantDTO dto) {
        TenantVO existingVO = detail(id);

        // 检查租户编码是否重复（排除自己）
        if (StringUtils.hasText(dto.tenantCode()) &&
                !dto.tenantCode().equals(existingVO.getTenantCode()) &&
                repository.existsByTenantCodeAndDeletedFalse(dto.tenantCode())) {
            throw new BizException("租户编码已存在");
        }

        Tenant existing = repository.findById(id)
                .orElseThrow(() -> new BizException("租户不存在"));

        EntityConverter.copyProperties(dto, existing);
        repository.save(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(String id, Boolean enabled) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new BizException("租户不存在"));
        tenant.setEnabled(enabled);
        repository.save(tenant);
    }

    @Override
    public List<OptionItem> options() {
        return repository.findByEnabledTrueAndDeletedFalse(PageRequest.of(0, queryProperties.optionLimit())).stream()
                .map(tenant -> OptionItem.of(tenant.getTenantName(), tenant.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean checkTenantCode(String tenantCode) {
        return !repository.existsByTenantCodeAndDeletedFalse(tenantCode);
    }
}
