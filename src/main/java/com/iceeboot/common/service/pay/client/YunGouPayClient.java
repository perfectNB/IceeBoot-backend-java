package com.iceeboot.common.service.pay.client;

import com.iceeboot.common.circuitbreaker.SimpleCircuitBreaker;
import com.iceeboot.common.exception.IceeBootException;
import com.yungouos.pay.wxpay.WxPay;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * YunGouOS 支付 SDK 访问入口。
 */
@Slf4j
@Component
public class YunGouPayClient {

    @Value("${iceeboot.pay.yungou.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    @Value("${iceeboot.pay.yungou.circuit-breaker.cooldown-millis:30000}")
    private long cooldownMillis;

    private SimpleCircuitBreaker cashierPayBreaker;

    @PostConstruct
    public void init() {
        cashierPayBreaker = new SimpleCircuitBreaker(
                "YunGouOS",
                "cashierPay",
                failureThreshold,
                cooldownMillis
        );
    }

    /**
     * 参数顺序按项目当前依赖的 yungouos-pay-sdk 2.0.30 对齐。
     */
    public String cashierPay(YunGouCashierPayRequest request) throws Exception {
        if (request == null) {
            throw new IceeBootException("支付请求不能为空");
        }
        if (!cashierPayBreaker.allowRequest()) {
            throw new IceeBootException("支付服务暂时不可用，请稍后重试");
        }

        try {
            String result = WxPay.cashierPay(
                    request.getOutTradeNo(),
                    request.getTotalFee(),
                    request.getMchId(),
                    request.getBody(),
                    request.getAppId(),
                    request.getAttach(),
                    request.getNotifyUrl(),
                    request.getReturnUrl(),
                    request.getConfigNo(),
                    request.getAuto(),
                    request.getAutoNode(),
                    request.getBizParams(),
                    request.getKey()
            );
            if (result == null || result.isBlank()) {
                throw new IceeBootException("支付服务返回空结果");
            }
            cashierPayBreaker.onSuccess();
            return result;
        } catch (Exception e) {
            cashierPayBreaker.onFailure();
            log.error("YunGouOS cashierPay 调用失败，outTradeNo: {}", request.getOutTradeNo(), e);
            throw e;
        }
    }
}
