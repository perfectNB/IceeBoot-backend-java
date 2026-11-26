package com.iceeboot.common.service.pay.client;

import com.alipay.api.*;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.pay.PaymentChannel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "iceeboot.pay.alipay.enabled", havingValue = "true")
public class AlipayQrClient implements OfficialPayClient {
    private final String appId;
    private final String sellerId;
    private final String publicKey;
    private final String notifyUrl;
    private final AlipayClient client;

    public AlipayQrClient(@Value("${iceeboot.pay.alipay.app-id}") String appId,
            @Value("${iceeboot.pay.alipay.seller-id}") String sellerId,
            @Value("${iceeboot.pay.alipay.private-key}") String privateKey,
            @Value("${iceeboot.pay.alipay.public-key}") String publicKey,
            @Value("${iceeboot.pay.alipay.notify-url}") String notifyUrl) {
        this.appId = appId;
        this.sellerId = sellerId;
        this.publicKey = publicKey;
        this.notifyUrl = notifyUrl;
        this.client = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", appId,
                privateKey, "json", "UTF-8", publicKey, "RSA2");
    }

    public PaymentChannel channel() { return PaymentChannel.ALIPAY; }

    public String prepay(String orderNo, String subject, int amountFen) {
        var model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(orderNo);
        model.setSubject(subject);
        model.setTotalAmount(BigDecimal.valueOf(amountFen, 2).toPlainString());
        var request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(notifyUrl);
        request.setBizModel(model);
        try {
            var response = client.execute(request);
            if (response.isSuccess() && orderNo.equals(response.getOutTradeNo())) return response.getQrCode();
        } catch (AlipayApiException e) {
            throw new IceeBootException("支付宝预下单失败");
        }
        throw new IceeBootException("支付宝未接受该订单");
    }

    public VerifiedPayment verify(HttpServletRequest request) throws AlipayApiException {
        Map<String, String> fields = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if (values.length != 1) throw new IllegalArgumentException("支付通知参数重复");
            fields.put(name, values[0]);
        });
        if (!AlipaySignature.rsaCheckV1(fields, publicKey, "UTF-8", "RSA2")
                || !appId.equals(fields.get("app_id")) || !sellerId.equals(fields.get("seller_id"))
                || !("TRADE_SUCCESS".equals(fields.get("trade_status")) || "TRADE_FINISHED".equals(fields.get("trade_status")))) {
            throw new IllegalArgumentException("支付宝通知校验失败");
        }
        int amount = new BigDecimal(fields.get("total_amount")).movePointRight(2).intValueExact();
        return new VerifiedPayment(fields.get("out_trade_no"), fields.get("trade_no"), amount);
    }
}
