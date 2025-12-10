/**
 * 支付策略工厂类
 * 根据策略编码获取对应的支付策略实例
 *
 * @author CodeIcee
 * @date 2025-11-20
 */
package com.iceeboot.common.service.pay.factory;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.pay.service.PayService;
import com.iceeboot.common.service.pay.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 支付策略工厂类
 * @author CodeIcee
 * @date 2025-11-20
 */
@Slf4j
@Component
public class PayFactory {

    /**
     * 策略实例缓存
     * key: 策略编码（payCode）
     * value: 策略实例
     */
    private final Map<String, PayService> serviceMap = new HashMap<>();

    /**
     * 构造函数，初始化所有策略实例
     */
    public PayFactory() {
        initStrategies();
    }

    /**
     * 初始化所有策略实例
     */
    private void initStrategies() {
        // SPA会员
        serviceMap.put("SPA_MEMBER", new SpaMemberService());

        // MMK会员
        serviceMap.put("MMK_MEMBER", new MmkMemberService());

        // 98会员
        serviceMap.put("NINETY_EIGHT_MEMBER", new NinetyEightMemberService());

        // 搓背会员
        serviceMap.put("SCRUB_MEMBER", new ScrubMemberService());

        // 精油会员
        serviceMap.put("ESSENTIAL_OIL_MEMBER", new EssentialOilMemberService());

        log.info("支付策略工厂初始化完成，共注册 {} 个策略: {}", serviceMap.size(), serviceMap.keySet());
    }

    /**
     * 根据策略编码获取策略实例
     * @param payCode 策略编码
     * @return 策略实例
     * @throws IceeBootException 如果找不到对应的策略
     */
    public PayService getStrategy(String payCode) {
        if (payCode == null || payCode.trim().isEmpty()) {
            throw new IceeBootException("策略编码不能为空");
        }

        PayService strategy = serviceMap.get(payCode);
        if (strategy == null) {
            throw new IceeBootException("不支持的支付策略: " + payCode);
        }

        return strategy;
    }

    /**
     * 获取所有支持的策略编码
     * @return 策略编码集合
     */
    public Set<String> getAllStrategyCodes() {
        return serviceMap.keySet();
    }

    /**
     * 检查策略是否存在
     * @param payCode 策略编码
     * @return true-存在，false-不存在
     */
    public boolean hasStrategy(String payCode) {
        return payCode != null && serviceMap.containsKey(payCode);
    }

    /**
     * 获取默认策略（SPA会员）
     * @return 默认策略实例
     */
    public PayService getDefaultStrategy() {
        return getStrategy("SPA_MEMBER");
    }
}


