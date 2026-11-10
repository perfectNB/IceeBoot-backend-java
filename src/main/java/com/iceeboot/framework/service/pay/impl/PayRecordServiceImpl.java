/**
 * 支付记录服务实现类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay.impl;

import com.iceeboot.common.constant.ApplicationConstants;
import com.iceeboot.common.constant.PaymentBusinessStatuses;
import com.iceeboot.common.constant.PaymentBusinessTypes;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.pay.client.YunGouCashierPayRequest;
import com.iceeboot.common.service.pay.client.YunGouPayClient;
import com.iceeboot.common.service.pay.factory.PayFactory;
import com.iceeboot.common.service.pay.service.PayService;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.entity.pay.PayConfigDO;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import com.iceeboot.framework.mapper.pay.PayRecordMapper;
import com.iceeboot.framework.query.pay.PayRecordQuery;
import com.iceeboot.framework.service.pay.IPayConfigService;
import com.iceeboot.framework.service.pay.IPayRecordService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.pay.PayRecordVO;
import com.iceeboot.framework.vo.system.SysUserVO;
import com.yungouos.pay.util.PaySignUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 支付记录服务实现类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PayRecordServiceImpl extends ServiceImpl<PayRecordMapper, PayRecordDO> implements IPayRecordService {

    @Value("${YunGouOS.key}")
    private String key;

    @Value("${YunGouOS.mchId}")
    private String mchId;

    @Value("${YunGouOS.notifyUrl}")
    private String notifyUrl;

    private final IPayConfigService paymentConfigService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final PayFactory strategyFactory;

    private final ISysUserService sysUserService;

    private final YunGouPayClient yunGouPayClient;

    /** 支付方式：微信 */
    private static final Integer PAY_TYPE_WECHAT = 1;
    
    /** 支付状态：支付中 */
    private static final Integer PAY_STATUS_PAYING = 1;
    
    /** 支付状态：支付成功 */
    private static final Integer PAY_STATUS_SUCCESS = 2;
    
    /** 支付状态：支付失败 */
    private static final Integer PAY_STATUS_FAILED = 3;
    
    /** 支付状态：退款 */
    private static final Integer PAY_STATUS_REFUND = 4;

    /** YunGouOS 回调成功响应体 */
    private static final String CALLBACK_SUCCESS_RESPONSE = "SUCCESS";

    @Override
    public PageResult<PayRecordVO> getPayRecordPage(PayRecordQuery query) {
        log.info("分页查询支付记录，查询条件：{}", query);
        Long userId = TokenUtils.getCurrentUserId();
        if (userId==null){
            throw new IceeBootException("没有userId");
        }
        // 构建查询条件
        LambdaQueryWrapper<PayRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getOutTradeNo()), PayRecordDO::getOutTradeNo, query.getOutTradeNo())
                .eq(PayRecordDO::getCreateBy,userId)
                .eq(query.getPayType() != null, PayRecordDO::getPayType, query.getPayType())
                .eq(query.getPayStatus() != null, PayRecordDO::getPayStatus, query.getPayStatus())
                .eq(query.getConfigId() != null, PayRecordDO::getConfigId, query.getConfigId())
                .eq(StringUtils.hasText(query.getApplicationCode()), PayRecordDO::getApplicationCode, query.getApplicationCode())
                .eq(StringUtils.hasText(query.getBusinessType()), PayRecordDO::getBusinessType, query.getBusinessType())
                .eq(StringUtils.hasText(query.getBusinessStatus()), PayRecordDO::getBusinessStatus, query.getBusinessStatus())
                .ge(query.getStartTime() != null, PayRecordDO::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, PayRecordDO::getCreateTime, query.getEndTime())
                .orderByDesc(PayRecordDO::getCreateTime);

        // 分页查询
        IPage<PayRecordDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PayRecordDO> result = this.page(page, wrapper);

        // 转换为VO
        List<PayRecordVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
//    @DataPermission
    public PayRecordVO getPayRecordById(Long id) {
        log.info("根据ID获取支付记录详情，ID：{}", id);
        PayRecordDO payRecord = this.getById(id);
        if (payRecord == null) {
            throw new IceeBootException("支付记录不存在");
        }

        return convertToVO(payRecord);
    }

    @Override
    public PayRecordDO getByOutTradeNo(String outTradeNo) {
        return this.getOne(
                new LambdaQueryWrapper<PayRecordDO>()
                        .eq(PayRecordDO::getOutTradeNo, outTradeNo)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createPaymentQR(Long configId) {
        log.info("创建支付二维码，配置ID：{}", configId);
        
        // 获取当前用户ID
        Long userId = TokenUtils.getCurrentUserId();
        if (userId == null) throw new IceeBootException("请先登录");
        
        // 防止频繁请求
        String redisKey = CacheConstants.Pay.QR_LIMIT + userId;
        String limitFlag = (String) redisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isNotBlank(limitFlag)) {
            throw new IceeBootException("您点的实在是太快啦！！！");
        }
        redisTemplate.opsForValue().set(redisKey, "1", 5, TimeUnit.SECONDS);

        // 获取支付配置
        PayConfigDO config = paymentConfigService.getById(configId);
        if (config == null) {
            throw new IceeBootException("不存在该支付配置");
        }

        // 生成商户订单号
        String outTradeNo = cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr();

        // 创建支付记录
        PayRecordDO payRecord = new PayRecordDO()
                .setOutTradeNo(outTradeNo)
                .setUserId(userId)
                .setApplicationCode(ApplicationConstants.PLATFORM)
                .setBusinessType(PaymentBusinessTypes.PAY_CONFIG)
                .setBusinessStatus(PaymentBusinessStatuses.PENDING)
                .setBusinessRetryCount(0)
                .setOrderName(config.getConfigName())
                .setPayType(PAY_TYPE_WECHAT)
                .setConfigId(configId)
                .setPayAmount(config.getAmount())
                .setPayStatus(PAY_STATUS_PAYING);

        boolean saved = this.save(payRecord);
        if (!saved) {
            throw new IceeBootException("创建支付订单失败");
        }

        log.info("支付订单创建成功，订单号：{}", outTradeNo);

        // 调用YunGouOS生成支付二维码
        try {
            // 金额单位转换：分转元，YunGouOS需要元单位（String类型）
            BigDecimal amountYuan = new BigDecimal(config.getAmount())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            String amount = amountYuan.toString();

            // 使用收银台支付（cashierPay）
            String qrCodeUrl = yunGouPayClient.cashierPay(YunGouCashierPayRequest.builder()
                    .outTradeNo(outTradeNo)
                    .totalFee(amount)
                    .mchId(mchId)
                    .body(config.getConfigName())
                    .attach(ApplicationConstants.PLATFORM)
                    .notifyUrl(notifyUrl)
                    .key(key)
                    .build());

            // 标记订单已创建（用于回调时验证）
            redisTemplate.opsForValue().set(
                    CacheConstants.Pay.PAY_ORDER_TEMP + outTradeNo,
                    "1",
                    30,
                    TimeUnit.MINUTES
            );

            log.info("支付二维码生成成功，订单号：{}，二维码URL：{}", outTradeNo, qrCodeUrl);
            return qrCodeUrl;

        } catch (Exception e) {
            log.error("生成支付二维码失败：{}", e.getMessage(), e);
            throw new IceeBootException("生成支付二维码失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handlePaymentCallback(HttpServletRequest request) {
        log.info("接收支付回调");

        try {
            // 验证签名
            boolean flag = verifyNotifySign(request);
            if (!flag) {
                log.error("支付回调签名验证失败");
                return "fail";
            }

            log.info("支付回调签名验证成功");

            // 获取回调参数
            String code = request.getParameter("code");
            String outTradeNo = request.getParameter("outTradeNo");
            String orderNo = request.getParameter("orderNo");
            String payNo = request.getParameter("payNo");
            String money = request.getParameter("money");
            String callbackMchId = request.getParameter("mchId");
            String tradeNo = StringUtils.hasText(payNo) ? payNo : orderNo;

            log.info("支付回调参数 - code：{}，商户订单号：{}，平台订单号：{}，支付订单号：{}，金额：{}，商户号：{}",
                    code, outTradeNo, orderNo, payNo, money, callbackMchId);
            // YunGouOS 官方回调：code = 1 表示支付成功
            if (!"1".equals(code)) {
                log.warn("支付回调 - 支付未成功，订单号：{}，code：{}", outTradeNo, code);
                return "fail";
            }
            if (!StringUtils.hasText(outTradeNo)) {
                log.error("支付回调 - 缺少商户订单号");
                return "fail";
            }
            if (!StringUtils.hasText(tradeNo)) {
                log.error("支付回调 - 缺少平台订单号，订单号：{}", outTradeNo);
                return "fail";
            }
            if (!StringUtils.hasText(callbackMchId) || !mchId.equals(callbackMchId)) {
                log.error("支付回调 - 商户号不匹配，订单号：{}，回调商户号：{}", outTradeNo, callbackMchId);
                return "fail";
            }
            String tempKey = CacheConstants.Pay.PAY_ORDER_TEMP + outTradeNo;

            // 查询支付记录
            PayRecordDO payRecord = getByOutTradeNo(outTradeNo);
            if (payRecord == null) {
                log.error("支付回调 - 支付记录不存在，订单号：{}", outTradeNo);
                return "fail";
            }

            // 校验回调金额与本地订单金额一致
            if (!isValidCallbackAmount(money, payRecord)) {
                log.error("支付回调 - 金额校验失败，订单号：{}，回调金额：{}，订单金额：{}",
                        outTradeNo, money, resolveExpectedAmountYuan(payRecord));
                return "fail";
            }

            // 检查订单状态，避免重复处理
            if (PAY_STATUS_SUCCESS.equals(payRecord.getPayStatus())) {
                processPaymentBusiness(payRecord);
                redisTemplate.delete(tempKey);
                return CALLBACK_SUCCESS_RESPONSE;
            }

            Integer payAmountFen = resolvePayAmountFen(payRecord);
            if (payAmountFen == null) {
                log.error("支付回调 - 无法计算pay_amount，订单号：{}，payAmount：{}，amount：{}",
                        outTradeNo, payRecord.getPayAmount(), payRecord.getAmount());
                return "fail";
            }

            // 原子更新：仅“支付中”订单可更新成功，避免并发回调重复执行业务逻辑
            int affected = baseMapper.updateToSuccessIfPaying(
                    outTradeNo, tradeNo, payAmountFen, PAY_STATUS_SUCCESS, PAY_STATUS_PAYING);

            if (affected == 1) {
                payRecord.setTradeNo(tradeNo);
                payRecord.setPayAmount(payAmountFen);
                payRecord.setPayStatus(PAY_STATUS_SUCCESS);
                log.info("支付回调 - 订单支付成功，订单号：{}，平台订单号：{}", outTradeNo, tradeNo);

                processPaymentBusiness(payRecord);
                
                redisTemplate.delete(tempKey);
                return CALLBACK_SUCCESS_RESPONSE;
            }

            // 可能是并发回调已由其他线程更新为成功，按幂等返回 success
            PayRecordDO latestRecord = baseMapper.selectByOutTradeNoForUpdate(outTradeNo);
            if (latestRecord != null && PAY_STATUS_SUCCESS.equals(latestRecord.getPayStatus())) {
                log.info("支付回调 - 并发幂等命中，订单已处理成功，订单号：{}", outTradeNo);
                redisTemplate.delete(tempKey);
                return CALLBACK_SUCCESS_RESPONSE;
            }

            log.error("支付回调 - 订单状态异常，订单号：{}，当前状态：{}",
                    outTradeNo, latestRecord == null ? null : latestRecord.getPayStatus());
            return "fail";

        } catch (Exception e) {
            log.error("支付回调处理异常：{}", e.getMessage(), e);
            return "fail";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryPaymentBusiness(Long payRecordId) {
        if (payRecordId == null) {
            return false;
        }
        PayRecordDO payRecord = this.getById(payRecordId);
        if (payRecord == null || !PAY_STATUS_SUCCESS.equals(payRecord.getPayStatus())) {
            return false;
        }
        return processPaymentBusiness(payRecord);
    }

    private boolean processPaymentBusiness(PayRecordDO payRecord) {
        if (PaymentBusinessStatuses.SUCCESS.equals(payRecord.getBusinessStatus())) {
            return true;
        }
        int claimed = baseMapper.claimBusinessProcessing(
                payRecord.getId(),
                PaymentBusinessStatuses.PROCESSING,
                PaymentBusinessStatuses.PENDING,
                PaymentBusinessStatuses.FAILED);
        if (claimed != 1) {
            PayRecordDO latest = this.getById(payRecord.getId());
            return latest != null && PaymentBusinessStatuses.SUCCESS.equals(latest.getBusinessStatus());
        }

        try {
            executePaymentStrategy(payRecord);
            baseMapper.updateBusinessSuccess(payRecord.getId(), PaymentBusinessStatuses.SUCCESS);
            return true;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (!StringUtils.hasText(errorMessage)) {
                errorMessage = e.getClass().getSimpleName();
            }
            if (errorMessage.length() > 240) {
                errorMessage = errorMessage.substring(0, 240);
            }
            baseMapper.updateBusinessFailed(payRecord.getId(), PaymentBusinessStatuses.FAILED, errorMessage);
            log.error("支付业务处理失败，已进入补偿队列，payRecordId={}, outTradeNo={}",
                    payRecord.getId(), payRecord.getOutTradeNo(), e);
            return false;
        }
    }

    @Override
    public boolean deletePayRecords(List<Long> ids) {
        log.info("删除支付记录，IDs：{}", ids);
        
        return this.removeByIds(ids);
    }

    /**
     * 执行支付策略
     * 根据支付配置的payCode找到对应的策略实现类并执行
     * 
     * @param payRecord 支付记录
     */
    private void executePaymentStrategy(PayRecordDO payRecord) {
        log.info("执行支付策略 - 开始，支付记录ID: {}", payRecord.getId());
        
        // 1. 获取支付配置
        Long configId = payRecord.getConfigId();
        if (configId == null) {
            log.warn("执行支付策略 - 支付配置ID为空，跳过策略执行");
            return;
        }
        
        PayConfigDO config = paymentConfigService.getById(configId);
        if (config == null) {
            throw new IceeBootException("支付配置不存在：" + configId);
        }
        
        // 2. 获取策略编码
        String payCode = config.getPayCode();
        if (!StringUtils.hasText(payCode)) {
            log.warn("执行支付策略 - 支付配置未设置payCode，跳过策略执行，configId: {}", configId);
            return;
        }
        
        // 3. 检查策略是否存在
        if (!strategyFactory.hasStrategy(payCode)) {
            throw new IceeBootException("支付策略不存在：" + payCode);
        }
        
        // 4. 获取用户ID
        Long userId = payRecord.getUserId();
        if (userId == null && StrUtil.isNotBlank(payRecord.getCreateBy())) {
            try {
                userId = Long.valueOf(payRecord.getCreateBy());
            } catch (NumberFormatException e) {
                log.error("执行支付策略 - 用户ID格式错误，createBy: {}", payRecord.getCreateBy());
            }
        }
        if (userId == null) {
            throw new IceeBootException("支付订单缺少用户信息");
        }
        
        // 5. 获取策略实例并执行
        try {
            PayService strategy = strategyFactory.getStrategy(payCode);
            log.info("执行支付策略 - 找到策略: {}, 描述: {}", 
                    strategy.getStrategyCode(), strategy.getDescription());
            
            // 执行业务逻辑
            strategy.execute(userId, payRecord);
            
            log.info("执行支付策略 - 完成，策略: {}, 用户ID: {}", payCode, userId);
            
        } catch (Exception e) {
            log.error("执行支付策略 - 执行失败，payCode: {}, 用户ID: {}, 错误: {}", 
                    payCode, userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 转换为VO对象
     */
    private PayRecordVO convertToVO(PayRecordDO payRecord) {
        PayRecordVO vo = new PayRecordVO();
        BeanUtils.copyProperties(payRecord, vo);
        
        // 设置支付方式名称
        if (payRecord.getPayType() != null) {
            vo.setPayTypeName(getPayTypeName(payRecord.getPayType()));
        }
        
        // 设置支付状态名称
        if (payRecord.getPayStatus() != null) {
            vo.setPayStatusName(getPayStatusName(payRecord.getPayStatus()));
        }
        
        // 计算元单位的金额
        if (payRecord.getPayAmount() != null) {
            BigDecimal amountYuan = new BigDecimal(payRecord.getPayAmount())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            vo.setPayAmountYuan(amountYuan.toString());
        }
        
        // 获取支付配置名称
        if (payRecord.getConfigId() != null) {
            PayConfigDO config = paymentConfigService.getById(payRecord.getConfigId());
            if (config != null) {
                vo.setConfigName(config.getConfigName());
            }
        }
        
        // 获取用户名
        if (StrUtil.isNotBlank(payRecord.getCreateBy())) {
            try {
                Long userId = Long.valueOf(payRecord.getCreateBy());
                vo.setUserId(userId);
                SysUserVO user = sysUserService.getUserById(userId);
                if (user != null) {
                    vo.setUsername(user.getUsername());
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", payRecord.getCreateBy());
            }
        }
        
        return vo;
    }

    /**
     * 获取支付方式名称
     */
    private String getPayTypeName(Integer payType) {
        switch (payType) {
            case 1:
                return "微信支付";
            case 2:
                return "支付宝支付";
            case 3:
                return "网银支付";
            default:
                return "未知";
        }
    }

    /**
     * 获取支付状态名称
     */
    private String getPayStatusName(Integer payStatus) {
        switch (payStatus) {
            case 1:
                return "支付中";
            case 2:
                return "支付成功";
            case 3:
                return "支付失败";
            case 4:
                return "退款";
            default:
                return "未知";
        }
    }

    /**
     * 校验回调金额与订单金额是否一致（单位：元）
     */
    private boolean isValidCallbackAmount(String callbackMoney, PayRecordDO payRecord) {
        if (!StringUtils.hasText(callbackMoney)) {
            return false;
        }
        BigDecimal expectedAmountYuan = resolveExpectedAmountYuan(payRecord);
        if (expectedAmountYuan == null) {
            return false;
        }
        try {
            BigDecimal callbackAmountYuan = new BigDecimal(callbackMoney);
            return callbackAmountYuan.compareTo(expectedAmountYuan) == 0;
        } catch (Exception e) {
            log.error("支付回调 - 金额格式错误，callbackMoney: {}", callbackMoney, e);
            return false;
        }
    }

    /**
     * 获取订单应付金额（单位：元）
     */
    private BigDecimal resolveExpectedAmountYuan(PayRecordDO payRecord) {
        if (payRecord.getPayAmount() != null) {
            return new BigDecimal(payRecord.getPayAmount())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        if (payRecord.getAmount() != null) {
            return payRecord.getAmount().setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    /**
     * 解析订单支付金额（单位：分）
     */
    private Integer resolvePayAmountFen(PayRecordDO payRecord) {
        if (payRecord.getPayAmount() != null) {
            return payRecord.getPayAmount();
        }
        BigDecimal expectedAmountYuan = resolveExpectedAmountYuan(payRecord);
        if (expectedAmountYuan == null) {
            return null;
        }
        try {
            return expectedAmountYuan
                    .movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
        } catch (Exception e) {
            log.error("支付回调 - 金额转分失败，amountYuan: {}", expectedAmountYuan, e);
            return null;
        }
    }

    /**
     * 兼容 Jakarta Servlet 环境的验签逻辑
     */
    private boolean verifyNotifySign(HttpServletRequest request) {
        String sign = request.getParameter("sign");
        if (!StringUtils.hasText(sign)) {
            log.error("支付回调 - 缺少签名参数sign");
            return false;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("code", request.getParameter("code"));
        params.put("orderNo", request.getParameter("orderNo"));
        params.put("outTradeNo", request.getParameter("outTradeNo"));
        params.put("payNo", request.getParameter("payNo"));
        params.put("money", request.getParameter("money"));
        params.put("mchId", request.getParameter("mchId"));
        String localSign = PaySignUtil.createSign(params, key);

        return sign.equals(localSign);
    }
}
