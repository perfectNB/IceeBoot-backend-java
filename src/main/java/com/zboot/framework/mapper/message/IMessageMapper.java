/**
 * 消息Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.zboot.framework.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.message.MessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 消息Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Mapper
public interface IMessageMapper extends BaseMapper<MessageDO> {

    /**
     * 统计用户未读消息数量
     * 
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 统计用户已读消息数量
     * 
     * @param userId 用户ID
     * @return 已读消息数量
     */
    Long countReadByUserId(@Param("userId") Long userId);

    /**
     * 统计用户总消息数量
     * 
     * @param userId 用户ID
     * @return 总消息数量
     */
    Long countTotalByUserId(@Param("userId") Long userId);
}