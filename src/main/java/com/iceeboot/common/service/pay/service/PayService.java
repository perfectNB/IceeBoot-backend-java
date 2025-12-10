package com.iceeboot.common.service.pay.service;

import com.iceeboot.framework.entity.pay.PayRecordDO;

/**
 * 支付策略接口
 * 定义支付业务的通用行为
 * 
 * @author CodeIcee
 * @date 2025-11-20
 */
public interface PayService {

    /**
     * 前置检查
     * 在用户下单前检查是否可以购买该商品/服务
     * 
     * @param userId 用户ID
     * @param payRecord 支付记录
     * @return true-可以购买，false-不能购买
     */
    boolean preCheck(Long userId, PayRecordDO payRecord);
    
    /**
     * 执行业务逻辑
     * 支付成功后执行的业务逻辑
     * 
     * @param userId 用户ID
     * @param payRecord 支付记录
     */
    void execute(Long userId, PayRecordDO payRecord);
    
    /**
     * 获取策略编码
     * 对应 PaymentConfigDO 的 payCode 字段
     * 
     * @return 策略编码
     */
    String getStrategyCode();
    
    /**
     * 获取策略描述
     * 
     * @return 策略描述
     */
    String getDescription();
}

