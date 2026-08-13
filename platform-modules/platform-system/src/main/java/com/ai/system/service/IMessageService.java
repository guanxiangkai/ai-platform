package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.MessageCreateDTO;
import com.ai.system.domain.dto.MessageDTO;
import com.ai.system.domain.dto.MessagePageDTO;
import com.ai.system.domain.entity.Message;
import com.ai.system.domain.vo.MessagePageVO;
import com.ai.system.domain.vo.MessageVO;

import java.util.List;

/**
 * 消息服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IMessageService extends IBaseService<MessagePageDTO, MessagePageVO, MessageVO, MessageCreateDTO, MessageDTO, Message> {

    /**
     * 标记消息为已读
     *
     * @param id 消息ID
     * @return 是否成功
     */
    Boolean markAsRead(String id);

    /**
     * 批量标记消息为已读
     *
     * @param ids 消息ID列表
     * @return 是否成功
     */
    Boolean batchMarkAsRead(List<String> ids);

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数量
     */
    Long getUnreadCount();

    /**
     * 获取公告消息列表
     * <p>
     * 查询所有展示状态的消息，按创建时间倒序排列
     *
     * @return 公告消息列表
     */
    List<MessageVO> getNotices();

    /**
     * 设置消息展示状态
     *
     * @param id      消息ID
     * @param display 是否展示
     * @return 是否成功
     */
    Boolean setDisplay(String id, Boolean display);

    /**
     * 批量设置消息展示状态
     *
     * @param ids       消息ID列表
     * @param isDisplay 是否展示
     * @return 是否成功
     */
    Boolean batchSetDisplay(List<String> ids, Boolean isDisplay);
}
