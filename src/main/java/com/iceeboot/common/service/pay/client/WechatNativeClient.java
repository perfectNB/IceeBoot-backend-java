package com.iceeboot.common.service.pay.client;

import com.iceeboot.framework.enums.pay.PaymentChannel;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "iceeboot.pay.wechat.enabled", havingValue = "true")
public class WechatNativeClient implements OfficialPayClient {
    private final String appId;
    private final String merchantId;
    private final String notifyUrl;
    private final NativePayService service;
    private final NotificationParser parser;

    public WechatNativeClient(@Value("${iceeboot.pay.wechat.app-id}") String appId,
            @Value("${iceeboot.pay.wechat.merchant-id}") String merchantId,
            @Value("${iceeboot.pay.wechat.notify-url}") String notifyUrl,
            @Value("${iceeboot.pay.wechat.private-key-path}") String privateKeyPath,
            @Value("${iceeboot.pay.wechat.merchant-serial}") String merchantSerial,
            @Value("${iceeboot.pay.wechat.public-key-path}") String publicKeyPath,
            @Value("${iceeboot.pay.wechat.public-key-id}") String publicKeyId,
            @Value("${iceeboot.pay.wechat.api-v3-key}") String apiV3Key) {
        this.appId = appId;
        this.merchantId = merchantId;
        this.notifyUrl = notifyUrl;
        var config = new RSAPublicKeyConfig.Builder().merchantId(merchantId)
                .privateKeyFromPath(privateKeyPath).merchantSerialNumber(merchantSerial)
                .publicKeyFromPath(publicKeyPath).publicKeyId(publicKeyId).apiV3Key(apiV3Key).build();
        service = new NativePayService.Builder().config(config).build();
        parser = new NotificationParser(config);
    }

    public PaymentChannel channel() { return PaymentChannel.WECHAT_NATIVE; }

    public String prepay(String orderNo, String subject, int amountFen) {
        var amount = new Amount();
        amount.setTotal(amountFen);
        amount.setCurrency("CNY");
        var request = new PrepayRequest();
        request.setAmount(amount);
        request.setAppid(appId);
        request.setMchid(merchantId);
        request.setDescription(subject);
        request.setNotifyUrl(notifyUrl);
        request.setOutTradeNo(orderNo);
        return service.prepay(request).getCodeUrl();
    }

    public VerifiedPayment verify(HttpServletRequest request, String body) {
        var headers = new RequestParam.Builder().serialNumber(request.getHeader("Wechatpay-Serial"))
                .timestamp(request.getHeader("Wechatpay-Timestamp")).nonce(request.getHeader("Wechatpay-Nonce"))
                .signature(request.getHeader("Wechatpay-Signature")).body(body).build();
        Transaction paid = parser.parse(headers, Transaction.class);
        if (!appId.equals(paid.getAppid()) || !merchantId.equals(paid.getMchid())
                || paid.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || paid.getAmount() == null || !"CNY".equals(paid.getAmount().getCurrency())) {
            throw new IllegalArgumentException("微信支付通知与商户配置不匹配");
        }
        return new VerifiedPayment(paid.getOutTradeNo(), paid.getTransactionId(), paid.getAmount().getTotal());
    }
}
