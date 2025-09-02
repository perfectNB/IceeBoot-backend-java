/**
 * 日志Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.log;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.log.LogClearDTO;
import com.zboot.framework.dto.log.LogQueryDTO;
import com.zboot.framework.entity.log.LogDO;
import com.zboot.framework.query.log.LogQuery;
import com.zboot.framework.vo.log.LogVO;

import java.util.List;

/**
 * 日志Service接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface LogService extends IService<LogDO> {

    /**
     * 获取日志列表
     * 
     * @param query 查询条件
     * @return 日志列表
     */
    PageResult<LogVO> getLogList(LogQuery query);

    /**
     * 删除日志
     * 
     * @param id 日志ID
     */
    void deleteLog(String id);



    /**
     * 删除日志（单个或批量）
     * 
     * @param ids 日志ID列表
     * @return 删除是否成功
     */
    boolean deleteLogs(List<String> ids);

    /**
     * 清空日志
     * 
     * @param clearDTO 清空参数
     */
    void clearLogs(LogClearDTO clearDTO);

    /**
     * 保存日志
     * 
     * @param logDO 日志实体
     */
    void saveLog(LogDO logDO);

    /**
     * 异步保存日志
     * 
     * @param level 日志级别
     * @param module 模块
     * @param message 消息
     * @param className 类名
     * @param summary 接口描述
     * @param path 请求路径
     */
    void saveLogAsync(String level, String module, String message, String className, String summary, String path);
}