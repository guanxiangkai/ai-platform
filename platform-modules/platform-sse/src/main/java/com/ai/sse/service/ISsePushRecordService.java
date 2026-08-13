package com.ai.sse.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.sse.domain.dto.SsePushRecordDTO;
import com.ai.sse.domain.dto.SsePushRecordPageDTO;
import com.ai.sse.domain.entity.SsePushRecord;
import com.ai.sse.domain.vo.SsePushRecordPageVO;
import com.ai.sse.domain.vo.SsePushRecordVO;

/**
 * SSE 推送日志服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ISsePushRecordService extends IBaseService<SsePushRecordPageDTO, SsePushRecordPageVO, SsePushRecordVO, SsePushRecordDTO, SsePushRecordDTO, SsePushRecord> {

    /**
     * 直接持久化已构建完成的推送日志实体
     *
     * @param entity SSE 推送日志实体
     */
    void createEntity(SsePushRecord entity);

    /**
     * 清理历史推送日志（保留指定天数）
     *
     * @param retainDays 保留天数
     * @return 清理数量
     */
    int cleanupHistory(int retainDays);
}
