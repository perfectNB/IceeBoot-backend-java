package com.iceeboot.common.service.pay.service.impl;

import com.iceeboot.common.service.pay.service.AbstractPayService;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import lombok.extern.slf4j.Slf4j;

/**
 * SPA会员支付策略
 * 策略编码：SPA_MEMBER
 *
 * @author CodeIcee
 * @date 2025-11-20
 */
@Slf4j
public class SpaMemberService extends AbstractPayService {

    @Override
    public String getStrategyCode() {
        return "SPA_MEMBER";
    }

    @Override
    public String getDescription() {
        return "SPA会员";
    }

    @Override
    public boolean preCheck(Long userId, PayRecordDO payRecord) {
        log.info("SPA会员 - 前置检查，用户ID: {}", userId);

        // TODO: 伪代码 - 实际业务逻辑

        log.info("SPA会员 - 前置检查通过");
        return true;
    }

    @Override
    protected void doExecute(Long userId, PayRecordDO payRecord) {
        log.info("SPA会员 - 开始执行业务逻辑，用户ID: {}, 金额: {}", userId, payRecord.getPayAmount());

        // TODO: 伪代码 - 实际业务逻辑

        log.info("SPA会员 - 业务逻辑执行成功，用户ID: {}", userId);
    }
}

