package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.LoginLogDTO;
import com.ai.system.domain.dto.LoginLogPageDTO;
import com.ai.system.domain.entity.LoginLog;
import com.ai.system.domain.vo.LoginLogPageVO;
import com.ai.system.domain.vo.LoginLogVO;

/**
 * 登录日志服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ILoginLogService extends IBaseService<LoginLogPageDTO, LoginLogPageVO, LoginLogVO, LoginLogDTO, LoginLogDTO, LoginLog> {

    /**
     * 直接持久化已构建完成的登录日志实体
     *
     * @param entity 登录日志实体
     */
    void createEntity(LoginLog entity);

    /**
     * 清空登录日志（保留最近 30 天）
     */
    Boolean clear();

}
