package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.TenantDTO;
import com.ai.system.domain.dto.TenantPageDTO;
import com.ai.system.domain.entity.Tenant;
import com.ai.system.domain.vo.TenantPageVO;
import com.ai.system.domain.vo.TenantVO;

import java.util.List;

/**
 * 租户服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ITenantService extends IBaseService<TenantPageDTO, TenantPageVO, TenantVO, TenantDTO, TenantDTO, Tenant> {

    /**
     * 更新租户状态
     *
     * @param id     租户ID
     * @param enable 是否启用
     */
    void updateEnabled(String id, Boolean enable);

    /**
     * 获取租户选项列表
     *
     * @return 租户选项列表
     */
    List<OptionItem> options();

    /**
     * 检查租户编码是否存在
     *
     * @param tenantCode 租户编码
     * @return 是否存在
     */
    Boolean checkTenantCode(String tenantCode);
}
