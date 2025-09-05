/**
 * 日志Service实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.log.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.create.log.LogClearDTO;

import com.zboot.framework.entity.log.LogDO;
import com.zboot.framework.mapper.log.LogMapper;
import com.zboot.framework.query.log.LogQuery;
import com.zboot.framework.service.log.LogService;
import com.zboot.framework.vo.log.LogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志Service实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, LogDO> implements LogService {

    @Override
    public PageResult<LogVO> getLogList(LogQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<LogDO> queryWrapper = new LambdaQueryWrapper<>();
        
        // 日志级别筛选
        if (StringUtils.hasText(query.getLevel())) {
            queryWrapper.eq(LogDO::getLevel, query.getLevel());
        }
        
        // 模块筛选
        if (StringUtils.hasText(query.getModule())) {
            queryWrapper.eq(LogDO::getModule, query.getModule());
        }
        
        // 关键词搜索
        if (StringUtils.hasText(query.getKeyword())) {
            queryWrapper.like(LogDO::getMessage, query.getKeyword())
                    .or().like(LogDO::getClassName, query.getKeyword());
        }
        
        // 时间范围筛选
        if (StringUtils.hasText(query.getStartTime())) {
            LocalDateTime startTime = LocalDateTime.parse(query.getStartTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.ge(LogDO::getTimestamp, startTime);
        }
        
        if (StringUtils.hasText(query.getEndTime())) {
            LocalDateTime endTime = LocalDateTime.parse(query.getEndTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.le(LogDO::getTimestamp, endTime);
        }
        
        // 按时间倒序排列
        queryWrapper.orderByDesc(LogDO::getTimestamp);
        
        // 分页查询
        Page<LogDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<LogDO> pageResult = this.page(page, queryWrapper);
        
        // 转换为VO
        List<LogVO> logVOList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return PageResult.of(logVOList, pageResult.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public void deleteLog(String id) {
        this.removeById(Long.valueOf(id));
    }


    @Override
    public boolean deleteLogs(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        try {
            List<Long> longIds = ids.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            return this.removeByIds(longIds);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void clearLogs(LogClearDTO clearDTO) {
        LambdaQueryWrapper<LogDO> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按级别清空
        if (StringUtils.hasText(clearDTO.getLevel())) {
            queryWrapper.eq(LogDO::getLevel, clearDTO.getLevel());
        }
        
        // 按日期清空
        if (StringUtils.hasText(clearDTO.getBeforeDate())) {
            LocalDateTime beforeDate = LocalDateTime.parse(clearDTO.getBeforeDate(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.lt(LogDO::getTimestamp, beforeDate);
        }
        
        this.remove(queryWrapper);
    }

    @Override
    public void saveLog(LogDO logDO) {
        if (logDO.getTimestamp() == null) {
            logDO.setTimestamp(LocalDateTime.now());
        }
        if (logDO.getThread() == null) {
            logDO.setThread(Thread.currentThread().getName());
        }
        this.save(logDO);
    }

    @Override
    @Async("taskExecutor")
    public void saveLogAsync(String level, String module, String message, String className, String summary, String path) {
        LogDO logDO = new LogDO();
        logDO.setLevel(level);
        logDO.setModule(module);
        logDO.setMessage(message);
        logDO.setClassName(className);
        logDO.setSummary(summary);
        logDO.setPath(path);
        logDO.setTimestamp(LocalDateTime.now());
        logDO.setThread(Thread.currentThread().getName());
        
        this.save(logDO);
    }

    /**
     * 转换为VO
     */
    private LogVO convertToVO(LogDO logDO) {
        LogVO logVO = new LogVO();
        BeanUtils.copyProperties(logDO, logVO);
        return logVO;
    }
}