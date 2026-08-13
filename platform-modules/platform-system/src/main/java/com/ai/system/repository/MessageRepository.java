package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Message;
import com.ai.system.domain.vo.MessagePageVO;
import com.ai.system.domain.vo.MessageVO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface MessageRepository
        extends BaseRepository<MessagePageVO, MessageVO, Message>,
                JpaPlusRepository<Message, String> {

    /**
     * 查询指定收件人的展示消息（过滤逻辑删除），按创建时间降序
     * <p>原 {@code findByDisplayAndReceiverIdAndDeletedFalse(Boolean, String, Sort)} 去掉了 Sort 参数，
     * 排序已内置为 createTime DESC</p>
     */
    default List<Message> findByDisplayAndReceiverIdAndDeletedFalse(Boolean display, String receiverId) {
        return list(QueryWrapper.from(Message.class)
                .eq(Message::getDisplay, display)
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getDeleted, false)
                .orderByDesc(Message::getCreateTime));
    }

    /** 统计指定收件人未读消息数量（过滤逻辑删除） */
    default Long countByReceiverIdAndIsReadAndDeletedFalse(String receiverId, Boolean isRead) {
        return count(QueryWrapper.from(Message.class)
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, isRead)
                .eq(Message::getDeleted, false));
    }
}
