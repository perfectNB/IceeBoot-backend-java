package com.iceeboot.common.service.pay.client;

import com.yungouos.pay.entity.BizParams;
import lombok.Builder;
import lombok.Getter;

/**
 * YunGouOS 收银台支付请求。
 */
@Getter
@Builder
public class YunGouCashierPayRequest {

    private final String outTradeNo;
    private final String totalFee;
    private final String mchId;
    private final String body;
    private final String appId;
    private final String attach;
    private final String notifyUrl;
    private final String returnUrl;
    private final String configNo;
    private final String auto;
    private final String autoNode;
    private final BizParams bizParams;
    private final String key;
}
