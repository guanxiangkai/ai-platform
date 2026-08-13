package com.ai.system.repository;

import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.entity.RegistrationOutbox;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 注册开通可靠出站任务数据访问层。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface RegistrationOutboxRepository extends JpaPlusRepository<RegistrationOutbox, String> {

    @Query("select event from RegistrationOutbox event where event.deleted = false "
            + "and event.deliveryState in :states and event.availableAt <= :now order by event.createTime")
    List<RegistrationOutbox> findReady(List<OutboxDeliveryState> states, LocalDateTime now, Pageable pageable);

    /** 锁定投递事件以完成领取或状态收敛。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from RegistrationOutbox event where event.id = :id and event.deleted = false")
    java.util.Optional<RegistrationOutbox> findLockedById(@Param("id") String id);
}
