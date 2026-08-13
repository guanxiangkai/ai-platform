package com.ai.sse.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * SSE 连接记录实体
 * <p>
 * 记录每次 SSE 连接的完整生命周期（连接 → 断开），
 * 即使未发送任何消息也会留下连接痕迹，用于审计和运维分析。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Entity
@Table(name = "sse_connection_record", comment = "SSE 连接记录表", indexes = {
        @Index(name = "idx_conn_user_id", columnList = "user_id"),
        @Index(name = "idx_conn_status", columnList = "connection_status"),
        @Index(name = "idx_conn_connect_time", columnList = "connect_time")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SseConnectionRecord extends TenantEntity {


    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 连接唯一标识（对应 SseConnection.connectionId）
     */
    @Column(name = "connection_id", nullable = false, length = 64, comment = "连接唯一标识")
    private String connectionId;

    /**
     * 用户 ID
     */
    @Column(name = "user_id", nullable = false, length = 64, comment = "用户ID")
    private String userId;

    /**
     * 连接建立时间
     */
    @Column(name = "connect_time", nullable = false, comment = "连接建立时间")
    private LocalDateTime connectTime;

    /**
     * 连接断开时间
     */
    @Column(name = "disconnect_time", comment = "连接断开时间")
    private LocalDateTime disconnectTime;

    /**
     * 连接持续时长（秒）
     */
    @Column(name = "duration_seconds", comment = "连接持续时长(秒)")
    private Long durationSeconds;

    /**
     * 断开原因
     */
    @Column(name = "disconnect_reason", length = 50, comment = "断开原因(disconnected/error/timeout)")
    private String disconnectReason;

    /**
     * 客户端 IP
     */
    @Column(name = "client_ip", length = 64, comment = "客户端IP")
    private String clientIp;

    /**
     * 服务实例标识（多实例时区分连接在哪个节点）
     */
    @Column(name = "server_instance", length = 128, comment = "服务实例标识")
    private String serverInstance;

    @Column(name = "connection_status", length = 20, comment = "连接状态")
    private String connectionStatus;
}
