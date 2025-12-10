package com.iceeboot.common.service.pay.service;

import com.iceeboot.framework.entity.pay.PayRecordDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付策略抽象基类
 * 子类可以继承此类，复用通用逻辑
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
@Slf4j
public abstract class AbstractPayService implements PayService {

    @Override
    public boolean preCheck(Long userId, PayRecordDO payRecord) {
        log.info("策略[{}] - 前置检查开始，用户ID: {}, 支付记录ID: {}", getStrategyCode(), userId, payRecord.getId());
        
        // 子类可以重写此方法实现具体的检查逻辑
        // 默认返回true
        return true;
    }
    
    @Override
    public void execute(Long userId, PayRecordDO payRecord) {
        log.info("策略[{}] - 执行业务逻辑，用户ID: {}, 支付记录ID: {}, 金额: {}", 
                getStrategyCode(), userId, payRecord.getId(), payRecord.getPayAmount());
        
        try {
            // 调用子类的具体业务逻辑
            doExecute(userId, payRecord);
            
            log.info("策略[{}] - 业务逻辑执行成功", getStrategyCode());
            
        } catch (Exception e) {
            log.error("策略[{}] - 业务逻辑执行失败: {}", getStrategyCode(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 执行具体的业务逻辑（由子类实现）
     * 
     * @param userId 用户ID
     * @param payRecord 支付记录
     */
    protected abstract void doExecute(Long userId, PayRecordDO payRecord);
}

