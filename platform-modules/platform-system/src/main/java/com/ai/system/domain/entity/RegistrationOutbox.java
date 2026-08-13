package com.ai.system.domain.entity;

import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.RegistrationOutboxEventType;
import com.ai.system.domain.RegistrationProvisioningPolicy;
import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 注册开通的可靠出站任务。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Entity
@Table(name = "sys_outbox", comment = "系统可靠出站任务表")
public class RegistrationOutbox extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "aggregate_id", nullable = false, length = 64, comment = "注册记录ID")
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64, comment = "事件类型")
    private RegistrationOutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_state", nullable = false, length = 20, comment = "投递状态")
    private OutboxDeliveryState deliveryState = OutboxDeliveryState.PENDING;

    @Column(name = "attempt_count", nullable = false, comment = "已尝试次数")
    private Integer attemptCount = 0;

    @Column(name = "available_at", nullable = false, comment = "下次可处理时间")
    private LocalDateTime availableAt;

    @Column(name = "last_error", length = RegistrationProvisioningPolicy.ERROR_MAX_LENGTH,
            comment = "最近失败原因")
    private String lastError;
}
