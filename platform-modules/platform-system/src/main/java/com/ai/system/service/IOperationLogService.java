package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import com.ai.system.domain.dto.OperationLogDTO;
import com.ai.system.domain.dto.OperationLogPageDTO;
import com.ai.system.domain.entity.OperationLog;
import com.ai.system.domain.vo.OperationLogPageVO;
import com.ai.system.domain.vo.OperationLogVO;

import java.util.Map;

/**
 * 操作日志服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IOperationLogService extends IBaseService<OperationLogPageDTO, OperationLogPageVO, OperationLogVO, OperationLogDTO, OperationLogDTO, OperationLog> {

    /**
     * 直接持久化已构建完成的操作日志实体
     *
     * @param entity 操作日志实体
     */
    void createEntity(OperationLog entity);

    /**
     * 清空操作日志（保留最近 30 天）
     *
     * @return 是否成功
     */
    Boolean clear();

    /**
     * 操作日志统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果（totalCount / successCount / failCount）
     */
    Map<String, Object> statistics(String startTime, String endTime);
}
