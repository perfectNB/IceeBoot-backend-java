/**
 * 支付记录服务实现类
 * 
 * @author CodeIcee
 * @date 2025-11-19
 */
package com.iceeboot.framework.service.pay.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.annotation.DataPermission;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import com.iceeboot.framework.entity.pay.PaymentConfigDO;
import com.iceeboot.framework.mapper.pay.PayRecordMapper;
import com.iceeboot.framework.query.pay.PayRecordQuery;
import com.iceeboot.framework.service.pay.IPayRecordService;
import com.iceeboot.framework.service.pay.IPaymentConfigService;
import com.iceeboot.framework.vo.pay.PayRecordVO;
import com.yungouos.pay.util.PaySignUtil;
import com.yungouos.pay.wxpay.WxPay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
public class PayRecordServiceImpl extends ServiceImpl<PayRecordMapper, PayRecordDO> implements IPayRecordService {

    @Value("${YunGouOS.key}")
    private String key;

    @Value("${YunGouOS.mchId}")
    private String mchId;

    @Value("${YunGouOS.notifyUrl}")
    private String notifyUrl;

    @Autowired
    private IPaymentConfigService paymentConfigService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
    @DataPermission  // 自动检查返回值的createBy字段是否等于当前用户ID
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
        
        // 防止频繁请求
        String redisKey = CacheConstants.Pay.QR_LIMIT + userId;
        String limitFlag = (String) redisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isNotBlank(limitFlag)) {
            throw new IceeBootException("您点的实在是太快啦！！！");
        }
        redisTemplate.opsForValue().set(redisKey, "1", 5, TimeUnit.SECONDS);

        // 获取支付配置
        PaymentConfigDO config = paymentConfigService.getById(configId);
        if (config == null) {
            throw new IceeBootException("支付配置不存在");
        }

        // 生成商户订单号
        String outTradeNo = System.currentTimeMillis() + "";

        // 创建支付记录
        PayRecordDO payRecord = new PayRecordDO()
                .setOutTradeNo(outTradeNo)
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
            String qrCodeUrl = WxPay.cashierPay(
                    outTradeNo,           // 商户订单号
                    amount,               // 支付金额（元）
                    mchId,                // 商户号
                    config.getConfigName(), // 订单标题
                    null,                 // 商品描述
                    null,                 // 附加数据
                    notifyUrl,            // 异步回调地址
                    null,                 // 同步回调地址
                    null,                 // auto参数
                    null,                 // autoOpenType参数
                    null,                 // 配置参数
                    null,                 // 开发者参数
                    key                   // 密钥
            );

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
            boolean flag = PaySignUtil.checkNotifySign(request, key);
            if (!flag) {
                log.error("支付回调签名验证失败");
                return "fail";
            }

            log.info("支付回调签名验证成功");

            // 获取回调参数
            String outTradeNo = request.getParameter("outTradeNo");
            String orderNo = request.getParameter("orderNo");

            log.info("支付回调参数 - 商户订单号：{}，平台订单号：{}", outTradeNo, orderNo);

            // 验证订单是否存在于Redis（防止重复回调）
            String tempKey = CacheConstants.Pay.PAY_ORDER_TEMP + outTradeNo;
            String tempFlag = (String) redisTemplate.opsForValue().get(tempKey);
            if (StrUtil.isBlank(tempFlag)) {
                log.warn("支付回调 - 订单已处理或不存在，订单号：{}", outTradeNo);
                return "fail";
            }

            // 查询支付记录
            PayRecordDO payRecord = getByOutTradeNo(outTradeNo);
            if (payRecord == null) {
                log.error("支付回调 - 支付记录不存在，订单号：{}", outTradeNo);
                return "fail";
            }

            // 检查订单状态，避免重复处理
            if (PAY_STATUS_SUCCESS.equals(payRecord.getPayStatus())) {
                log.warn("支付回调 - 订单已支付成功，跳过处理，订单号：{}", outTradeNo);
                // 删除临时标记
                redisTemplate.delete(tempKey);
                return "success";
            }

            // 更新支付记录
            payRecord.setTradeNo(orderNo);
            payRecord.setPayStatus(PAY_STATUS_SUCCESS);
            boolean updated = this.updateById(payRecord);

            if (updated) {
                log.info("支付回调 - 订单支付成功，订单号：{}，平台订单号：{}", outTradeNo, orderNo);
                
                // TODO: 这里可以添加支付成功后的业务逻辑
                // 例如：增加用户余额、开通会员等
                
                // 删除临时标记
                redisTemplate.delete(tempKey);
                
                return "success";
            } else {
                log.error("支付回调 - 更新支付记录失败，订单号：{}", outTradeNo);
                return "fail";
            }

        } catch (Exception e) {
            log.error("支付回调处理异常：{}", e.getMessage(), e);
            return "fail";
        }
    }

    @Override
    public boolean deletePayRecords(List<Long> ids) {
        log.info("删除支付记录，IDs：{}", ids);
        
        return this.removeByIds(ids);
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
            PaymentConfigDO config = paymentConfigService.getById(payRecord.getConfigId());
            if (config != null) {
                vo.setConfigName(config.getConfigName());
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
}

