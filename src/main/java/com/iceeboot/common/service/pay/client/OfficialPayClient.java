package com.iceeboot.common.service.pay.client;

import com.iceeboot.framework.enums.pay.PaymentChannel;

/** 官网扫码支付适配器，只有启用的渠道才会注册。金额统一使用分。 */
public interface OfficialPayClient {
    PaymentChannel channel();
    String prepay(String orderNo, String subject, int amountFen);
}
