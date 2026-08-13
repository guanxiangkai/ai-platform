package com.ai.sse.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.sse.domain.dto.SseConnectionRecordDTO;
import com.ai.sse.domain.dto.SseConnectionRecordPageDTO;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.domain.vo.SseConnectionRecordPageVO;
import com.ai.sse.domain.vo.SseConnectionRecordVO;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SSE 连接记录服务接口
 * <p>
 * 继承 {@link IBaseService} 提供标准 CRUD 功能，
 * 同时提供连接生命周期记录和审计查询能力。
 * 即使连接期间未发送任何消息，也会留下审计痕迹。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ISseConnectionRecordService extends IBaseService<SseConnectionRecordPageDTO,
        SseConnectionRecordPageVO, SseConnectionRecordVO, SseConnectionRecordDTO,
        SseConnectionRecordDTO, SseConnectionRecord> {

    // ==================== 连接生命周期记录 ====================

    /**
     * 记录连接建立
     *
     * @param connectionId 连接唯一标识
     * @param userId       用户 ID
     * @param tenantId     租户 ID
     * @param connectTime  连接时间
     * @param clientIp     客户端 IP（可选）
     */
    void logConnect(String connectionId, String userId, String tenantId,
                    LocalDateTime connectTime, String clientIp);

    /**
     * 记录连接断开（更新已有记录）
     *
     * @param connectionId     连接唯一标识
     * @param disconnectTime   断开时间
     * @param disconnectReason 断开原因（disconnected / error / timeout）
     */
    void logDisconnect(String connectionId, LocalDateTime disconnectTime, String disconnectReason);

    // ==================== 查询统计 ====================

    /**
     * 获取连接统计信息
     *
     * @return 各状态连接数
     */
    Map<String, Object> getConnectionStats();


    /**
     * 清理历史连接记录（保留指定天数）
     *
     * @param retainDays 保留天数
     * @return 清理数量
     */
    int cleanupHistory(int retainDays);
}
