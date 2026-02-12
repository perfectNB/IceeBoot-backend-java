package com.iceeboot.framework.controller.pay;

import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.core.domain.R;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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

    @Operation(summary = "支付回调接口", description = "由支付平台调用，处理支付结果")
    @PostMapping("/callback")
    public String handlePaymentCallback(HttpServletRequest request) {
        return payRecordService.handlePaymentCallback(request);
    }

    @Operation(summary = "分页查询支付配置", description = "分页查询支付配置列表")
    @GetMapping("/configs")
    public R<PageResult<PayConfigVO>> getPaymentConfigPage(PayConfigQuery query) {
        PageResult<PayConfigVO> pageResult = payConfigService.getPayConfigPage(query);
        return R.ok(pageResult);
    }

    @Operation(summary = "新增支付配置", description = "新增支付配置")
    @PostMapping("/configs")
    public R<Boolean> addPaymentConfig(@Validated @RequestBody PayConfigDTO configDTO) {
        boolean result = payConfigService.addPayConfig(configDTO);
        return R.ok(result);
    }

    @Operation(summary = "修改支付配置", description = "修改支付配置")
    @PutMapping("/configs")
    public R<Boolean> updatePaymentConfig(@Validated @RequestBody PayConfigUpdateDTO configDTO) {
        boolean result = payConfigService.updatePayConfig(configDTO);
        return R.ok(result);
    }

    @Operation(summary = "删除支付配置", description = "批量删除支付配置")
    @DeleteMapping("/configs")
    public R<Boolean> deletePaymentConfigs(@RequestBody List<Long> ids) {
        boolean result = payConfigService.deletePayConfigs(ids);
        return R.ok(result);
    }


    @Operation(summary = "查询支付记录", description = "分页查询用户的支付记录")
    @GetMapping("/records")
    public R<PageResult<PayRecordVO>> getPayRecords(PayRecordQuery query) {
        PageResult<PayRecordVO> pageResult = payRecordService.getPayRecordPage(query);
        return R.ok(pageResult);
    }

}

