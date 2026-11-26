package com.iceeboot.framework.controller.pay;

import com.alipay.api.AlipayApiException;
import com.iceeboot.common.service.pay.client.AlipayQrClient;
import com.iceeboot.common.service.pay.client.WechatNativeClient;
import com.iceeboot.framework.service.pay.IPayRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/pay/notify")
@RequiredArgsConstructor
public class OfficialPaymentNotifyController {
    private final ObjectProvider<WechatNativeClient> wechat;
    private final ObjectProvider<AlipayQrClient> alipay;
    private final IPayRecordService records;

    @PostMapping("/wechat")
    public ResponseEntity<Void> wechat(HttpServletRequest request, @RequestBody String body) {
        var client = wechat.getIfAvailable();
        if (client == null) return ResponseEntity.notFound().build();
        try {
            records.confirmPayment(client.channel(), client.verify(request, body));
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/alipay")
    public String alipay(HttpServletRequest request) {
        var client = alipay.getIfAvailable();
        if (client == null) return "failure";
        try {
            records.confirmPayment(client.channel(), client.verify(request));
            return "success";
        } catch (RuntimeException | AlipayApiException e) {
            return "failure";
        }
    }
}
