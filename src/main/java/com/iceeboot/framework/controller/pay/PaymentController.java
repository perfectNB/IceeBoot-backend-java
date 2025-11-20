/**
 * 支付控制器
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.controller.pay;

import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.framework.query.pay.PayRecordQuery;
import com.iceeboot.framework.service.pay.IPayRecordService;
import com.iceeboot.framework.service.pay.IPaymentConfigService;
import com.iceeboot.framework.vo.pay.PayRecordVO;
import com.iceeboot.framework.vo.pay.PaymentConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 支付控制器
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Slf4j
@RestController
@RequestMapping("/v1/pay")
@Tag(name = "支付管理", description = "支付相关接口")
public class PaymentController {

    @Autowired
    private IPayRecordService payRecordService;

    @Autowired
    private IPaymentConfigService paymentConfigService;

    @Operation(summary = "获取支付配置列表", description = "获取所有启用的支付配置")
    @GetMapping("/configs")
    public R<List<PaymentConfigVO>> getActiveConfigs() {
        List<PaymentConfigVO> configs = paymentConfigService.getAllActiveConfigs();
        return R.ok(configs);
    }

    @Operation(summary = "获取支付二维码", description = "根据支付配置ID创建支付订单并返回二维码")
    @GetMapping("/create-qr/{configId}")
    public R<String> createPaymentQR(@PathVariable("configId") Long configId) {
        String qrCodeUrl = payRecordService.createPaymentQR(configId);
        return R.ok(qrCodeUrl);
    }

    @Operation(summary = "支付回调接口", description = "由支付平台调用，处理支付结果")
    @PostMapping("/callback")
    public String handlePaymentCallback(HttpServletRequest request) {
        return payRecordService.handlePaymentCallback(request);
    }

    @Operation(summary = "查询支付记录", description = "分页查询用户的支付记录")
    @GetMapping("/records")
    public R<PageResult<PayRecordVO>> getPayRecords(PayRecordQuery query) {
        PageResult<PayRecordVO> pageResult = payRecordService.getPayRecordPage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "获取支付记录详情", description = "根据ID获取支付记录详情")
    @GetMapping("/records/{id}")
    public R<PayRecordVO> getPayRecordById(@PathVariable("id") Long id) {
        PayRecordVO payRecord = payRecordService.getPayRecordById(id);
        return R.ok(payRecord);
    }
}

