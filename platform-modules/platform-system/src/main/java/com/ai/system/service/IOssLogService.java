package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.OssLogDTO;
import com.ai.system.domain.dto.OssLogPageDTO;
import com.ai.system.domain.entity.OssLog;
import com.ai.system.domain.vo.OssLogPageVO;
import com.ai.system.domain.vo.OssLogVO;

/**
 * OSS 文件上传日志服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IOssLogService extends IBaseService<OssLogPageDTO, OssLogPageVO, OssLogVO, OssLogDTO, OssLogDTO, OssLog> {

    /**
     * 直接持久化已构建完成的上传日志实体
     *
     * @param entity OSS 日志实体
     */
    void createEntity(OssLog entity);

    /**
     * 清空上传日志（保留最近 30 天）
     *
     * @return 是否成功
     */
    Boolean clear();

}
