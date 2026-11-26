package com.iceeboot.framework.controller.pay;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.annotation.RateLimit;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.RateLimitType;
import com.iceeboot.framework.dto.create.pay.PayConfigDTO;
import com.iceeboot.framework.dto.update.pay.PayConfigUpdateDTO;
import com.iceeboot.framework.query.pay.PayConfigQuery;
import com.iceeboot.framework.query.pay.PayRecordQuery;
import com.iceeboot.framework.service.pay.IPayConfigService;
import com.iceeboot.framework.service.pay.IPayRecordService;
import com.iceeboot.framework.vo.pay.PayConfigVO;
import com.iceeboot.framework.vo.pay.PayRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.iceeboot.framework.enums.pay.PaymentChannel;
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
@RequiredArgsConstructor
public class PayController {

    private final IPayRecordService payRecordService;

    private final IPayConfigService payConfigService;


    @Operation(summary = "获取支付二维码", description = "根据支付配置ID创建支付订单并返回二维码")
    @GetMapping("/create-qr/{configId}")
    public R<String> createPaymentQR(@PathVariable("configId") Long configId) {
        String qrCodeUrl = payRecordService.createPaymentQR(configId);
        return R.ok(qrCodeUrl);
    }

    @GetMapping("/channels")
    public R<List<PaymentChannel>> channels() {
        return R.ok(payRecordService.enabledChannels());
    }

    @PostMapping("/orders/{configId}")
    public R<String> createOrder(@PathVariable Long configId,
            @RequestParam PaymentChannel channel) {
        return R.ok(payRecordService.createPaymentQR(configId, channel));
    }

    @GetMapping("/records/{id}")
    @RequirePermission(dynamic = true)
    public R<PayRecordVO> detail(@PathVariable Long id) {
        return R.ok(payRecordService.getPayRecordById(id));
    }

    @Operation(summary = "支付回调接口", description = "由支付平台调用，处理支付结果")
    @PostMapping("/callback")
    @RateLimit(count = 120, time = 60, limitType = RateLimitType.IP, message = "支付回调请求过于频繁")
    public String handlePaymentCallback(HttpServletRequest request) {
        return payRecordService.handlePaymentCallback(request);
    }

    @Operation(summary = "分页查询支付配置", description = "分页查询支付配置列表")
    @GetMapping("/configs")
    @RequirePermission(dynamic = true)
    public R<PageResult<PayConfigVO>> getPaymentConfigPage(PayConfigQuery query) {
        PageResult<PayConfigVO> pageResult = payConfigService.getPayConfigPage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "新增支付配置", description = "新增支付配置")
    @PostMapping("/configs")
    @RequirePermission(dynamic = true)
    public R<Boolean> addPaymentConfig(@Validated @RequestBody PayConfigDTO configDTO) {
        boolean result = payConfigService.addPayConfig(configDTO);
        return R.ok(result);
    }

    @Operation(summary = "修改支付配置", description = "修改支付配置")
    @PutMapping("/configs")
    @RequirePermission(dynamic = true)
    public R<Boolean> updatePaymentConfig(@Validated @RequestBody PayConfigUpdateDTO configDTO) {
        boolean result = payConfigService.updatePayConfig(configDTO);
        return R.ok(result);
    }

    @PutMapping("/configs/{id}/status")
    @RequirePermission(dynamic = true)
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return R.ok(payConfigService.updateStatus(id, status));
    }

    @Operation(summary = "删除支付配置", description = "批量删除支付配置")
    @DeleteMapping("/configs")
    @RequirePermission(dynamic = true)
    public R<Boolean> deletePaymentConfigs(@RequestBody List<Long> ids) {
        boolean result = payConfigService.deletePayConfigs(ids);
        return R.ok(result);
    }


    @Operation(summary = "查询支付记录", description = "分页查询用户的支付记录")
    @GetMapping("/records")
    @RequirePermission(dynamic = true)
    public R<PageResult<PayRecordVO>> getPayRecords(PayRecordQuery query) {
        PageResult<PayRecordVO> pageResult = payRecordService.getPayRecordPage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "重试支付业务", description = "重新执行支付成功但业务到账失败的订单")
    @PutMapping("/records/{id}/retry-business")
    @RequirePermission(dynamic = true)
    public R<Boolean> retryPaymentBusiness(@PathVariable Long id) {
        payRecordService.getPayRecordById(id);
        return R.ok(payRecordService.retryPaymentBusiness(id));
    }

}
