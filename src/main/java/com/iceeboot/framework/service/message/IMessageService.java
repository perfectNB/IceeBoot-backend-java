/**
 * 消息服务接口
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.iceeboot.framework.service.message;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.message.MessageDTO;
import com.iceeboot.framework.entity.message.MessageDO;
import com.iceeboot.framework.query.message.MessageQuery;
import com.iceeboot.framework.vo.message.MessageVO;

import java.util.List;
import java.util.Map;

/**
 * 消息服务接口
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
public interface IMessageService extends IService<MessageDO> {

    /**
     * 分页查询当前用户消息列表
     * 
     * @param query 查询条件
     * @return 消息分页列表
     */
    PageResult<MessageVO> getMessagePage(MessageQuery query);


    /**
     * 新增消息
     * 
     * @param messageDTO 消息信息
     * @return 是否成功
     */
    boolean addMessage(MessageDTO messageDTO);

    /**
     * 全部已读
     *
     * @return 是否成功
     */
    boolean allRead();


    /**
     * 更新消息
     *
     * @param ids 消息信息
     * @return 是否成功
     */
    boolean updateMessage(List<Long> ids);


    /**
     * 删除消息
     * 
     * @param ids 消息ID列表
     * @return 是否成功
     */
    boolean deleteMessages(List<Long> ids);

    /**
     * 获取消息统计信息
     * 
     * @return 统计信息（包含未读、已读、总数）
     */
    Map<String, Long> getMessageStats();
}