package com.ai.system.service.impl;

import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.mq.producer.MessageProducer;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.dto.MessageCreateDTO;
import com.ai.system.domain.dto.MessageDTO;
import com.ai.system.domain.dto.MessagePageDTO;
import com.ai.system.domain.entity.Message;
import com.ai.system.domain.vo.MessagePageVO;
import com.ai.system.domain.vo.MessageVO;
import com.ai.system.repository.MessageRepository;
import com.ai.system.service.IMessageService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 消息服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends BaseServiceImpl<MessagePageDTO, MessagePageVO, MessageVO, MessageCreateDTO, MessageDTO, Message>
        implements IMessageService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MessageServiceImpl.class);


    private final MessageRepository repository;
    private final MessageProducer messageProducer;
    private final Converter converter;

    @Override
    protected BaseRepository<MessagePageVO, MessageVO, Message> getRepository() {
        return this.repository;
    }

    @Override
    public MessageVO detail(String id) {
        Message message = requireEntity(id);
        verifyCurrentReceiver(message);
        return translate(converter.convert(message, MessageVO.class));
    }

    @Override
    protected void beforeUpdate(Message message, MessageDTO dto) {
        String currentUserId = verifyCurrentReceiver(message);
        if (dto.receiverId() != null && !currentUserId.equals(dto.receiverId())) {
            throw new BizException("不允许修改消息接收人");
        }
    }

    @Override
    protected void beforeDelete(Message message) {
        verifyCurrentReceiver(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(String id, Boolean enabled) {
        verifyCurrentReceiver(requireEntity(id));
        super.updateEnabled(id, enabled);
    }

    /**
     * 构建自定义查询条件
     */
    @Override
    protected Specification<Message> buildQuerySpec(MessagePageDTO pageDTO) {
        String currentUserId = requireCurrentUserId();
        var builder = SpecUtils.<Message>builder()
                .eqIfPresent(Message::getReceiverId, currentUserId);
        if (pageDTO == null) return builder.build();
        return builder
                .likeIfPresent(Message::getMsgTitle, pageDTO.getMsgTitle())
                .eqIfPresent(Message::getMsgType, pageDTO.getMsgType())
                .eqIfPresent(Message::getSenderId, pageDTO.getSenderId())
                .eqIfPresent(Message::getIsRead, pageDTO.getIsRead())
                .eqIfPresent(Message::getPriority, pageDTO.getPriority())
                .geTimeIfPresent(Message::getCreateTime, pageDTO.getStartTime())
                .leTimeIfPresent(Message::getCreateTime, pageDTO.getEndTime())
                .build();
    }

    // ==================== 创建后钩子：SSE 推送 ====================

    @Override
    protected void afterCreate(Message message, MessageCreateDTO dto) {
        // 推送未读消息数量给接收人
        pushUnreadCount(message.getReceiverId());
        // 如果消息设置为公告展示，同时推送公告列表
        if (Boolean.TRUE.equals(message.getDisplay())) {
            pushNotices(message.getReceiverId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean markAsRead(String id) {
        Message message = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("消息不存在"));

        // 检查是否是当前用户的消息
        String currentUserId = verifyCurrentReceiver(message);

        if (Boolean.TRUE.equals(message.getIsRead())) {
            return true;
        }

        message.setIsRead(true);
        message.setReadTime(LocalDateTime.now());
        repository.save(message);

        log.info("消息标记为已读成功，消息ID: {}", id);

        // SSE 推送更新后的未读消息数量
        pushUnreadCount(currentUserId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchMarkAsRead(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        String currentUserId = requireCurrentUserId();
        List<Message> messages = repository.findAllById(ids);

        List<Message> changedMessages = new ArrayList<>();
        for (Message message : messages) {
            // 只处理当前用户的未读消息
            if (Objects.equals(message.getReceiverId(), currentUserId) && Boolean.FALSE.equals(message.getIsRead())) {
                message.setIsRead(true);
                message.setReadTime(LocalDateTime.now());
                changedMessages.add(message);
            }
        }

        repository.saveAll(changedMessages);
        log.info("批量标记消息为已读成功，消息数量: {}", changedMessages.size());

        // SSE 推送更新后的未读消息数量
        if (!changedMessages.isEmpty()) {
            pushUnreadCount(currentUserId);
        }

        return true;
    }

    @Override
    public Long getUnreadCount() {
        String currentUserId = requireCurrentUserId();
        return repository.countByReceiverIdAndIsReadAndDeletedFalse(currentUserId, false);
    }

    @Override
    public List<MessageVO> getNotices() {
        String currentUserId = requireCurrentUserId();
        return converter.convert(getNoticesByUserId(currentUserId), MessageVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDisplay(String id, Boolean isDisplay) {
        Message message = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("消息不存在"));

        // 校验归属：只有消息接收人才能修改展示状态
        verifyCurrentReceiver(message);

        message.setDisplay(isDisplay != null ? isDisplay : false);
        repository.save(message);

        log.info("设置消息展示状态成功，消息ID: {}, 展示状态: {}", id, isDisplay);

        // SSE 推送更新后的公告消息列表
        pushNotices(message.getReceiverId());

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSetDisplay(List<String> ids, Boolean isDisplay) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        List<Message> messages = repository.findAllById(ids);
        List<Message> changedMessages = new ArrayList<>();
        Set<String> affectedReceivers = new HashSet<>();
        String currentUserId = requireCurrentUserId();
        for (Message message : messages) {
            // 只处理当前用户的消息，防止 IDOR
            if (!Objects.equals(message.getReceiverId(), currentUserId)) {
                continue;
            }
            message.setDisplay(isDisplay != null ? isDisplay : false);
            changedMessages.add(message);
            affectedReceivers.add(message.getReceiverId());
        }

        repository.saveAll(changedMessages);
        log.info("批量设置消息展示状态成功，消息数量: {}, 展示状态: {}", changedMessages.size(), isDisplay);

        // SSE 推送更新后的公告消息列表给所有受影响的用户
        affectedReceivers.forEach(this::pushNotices);

        return true;
    }

    // ==================== SSE 推送私有方法 ====================

    /**
     * 推送未读消息数量给指定用户
     *
     * @param userId 用户ID
     */
    private void pushUnreadCount(String userId) {
        try {
            Long count = repository.countByReceiverIdAndIsReadAndDeletedFalse(userId, false);
            Map<String, Object> notification = buildUserNotification(userId, SystemConstants.SseConstants.TYPE_UNREAD_COUNT, count);
            messageProducer.send(SystemConstants.SseConstants.TOPIC_NOTIFICATION, notification);
            log.debug("[SSE] 推送未读消息数量: userId={}, count={}", userId, count);
        } catch (Exception e) {
            log.warn("[SSE] 推送未读消息数量失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 推送公告消息给指定用户
     *
     * @param userId 用户ID
     */
    private void pushNotices(String userId) {
        try {
            List<MessageVO> notices = converter.convert(getNoticesByUserId(userId), MessageVO.class);
            Map<String, Object> notification = buildUserNotification(userId, SystemConstants.SseConstants.TYPE_NOTICES, notices);
            messageProducer.send(SystemConstants.SseConstants.TOPIC_NOTIFICATION, notification);
            log.debug("[SSE] 推送公告消息: userId={}, count={}", userId, notices.size());
        } catch (Exception e) {
            log.warn("[SSE] 推送公告消息失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 根据用户ID获取最新公告消息（不依赖 SecurityContext）
     */
    private List<Message> getNoticesByUserId(String userId) {
        return repository.findByDisplayAndReceiverIdAndDeletedFalse(true, userId).stream()
                .limit(20)
                .toList();
    }

    private String verifyCurrentReceiver(Message message) {
        String currentUserId = requireCurrentUserId();
        if (!Objects.equals(currentUserId, message.getReceiverId())) {
            throw new BizException("无权限操作该消息");
        }
        return currentUserId;
    }

    private String requireCurrentUserId() {
        String currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new BizException("当前登录用户无效");
        }
        return currentUserId;
    }

    /**
     * 构建单用户 SSE 通知 MQ 消息体
     * <p>
     * 结构与 ai-sse 模块 SseNotification 一致，
     * 由 SseConsumerConfig 消费后推送给在线 SSE 客户端。
     * </p>
     *
     * @param userId      目标用户ID
     * @param messageType SSE 消息类型
     * @param content     推送内容
     * @return 通知消息体（Map 形式）
     */
    private Map<String, Object> buildUserNotification(String userId, String messageType, Object content) {
        String tenantId = requireCurrentTenantId();
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("targetType", SystemConstants.SseConstants.TARGET_TYPE_USER);
        notification.put("userId", userId);
        notification.put("userIds", null);
        notification.put("tenantId", tenantId);
        notification.put("messageType", messageType);
        notification.put("content", content);
        return notification;
    }

    private String requireCurrentTenantId() {
        String tenantId = SecurityUtils.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantExecutionScope.currentTenantId();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new BizException("当前租户无效");
        }
        return tenantId;
    }
}
