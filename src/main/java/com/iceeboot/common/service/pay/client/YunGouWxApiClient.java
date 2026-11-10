package com.iceeboot.common.service.pay.client;

import com.alibaba.fastjson.JSONObject;
import com.iceeboot.common.circuitbreaker.SimpleCircuitBreaker;
import com.iceeboot.common.exception.IceeBootException;
import com.yungouos.pay.entity.WxOauthInfo;
import com.yungouos.pay.entity.WxWebLoginBiz;
import com.yungouos.pay.wxapi.WxApi;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * YunGouOS 微信 API 访问入口。
 */
@Slf4j
@Component
public class YunGouWxApiClient {

    @Value("${iceeboot.pay.yungou.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    @Value("${iceeboot.pay.yungou.circuit-breaker.cooldown-millis:30000}")
    private long cooldownMillis;

    private SimpleCircuitBreaker wxOauthUrlBreaker;
    private SimpleCircuitBreaker webLoginBreaker;
    private SimpleCircuitBreaker wxOauthInfoBreaker;

    @PostConstruct
    public void init() {
        wxOauthUrlBreaker = new SimpleCircuitBreaker(
                "YunGouOS",
                "getWxOauthUrl",
                failureThreshold,
                cooldownMillis
        );
        webLoginBreaker = new SimpleCircuitBreaker(
                "YunGouOS",
                "getWebLogin",
                failureThreshold,
                cooldownMillis
        );
        wxOauthInfoBreaker = new SimpleCircuitBreaker(
                "YunGouOS",
                "getWxOauthInfo",
                failureThreshold,
                cooldownMillis
        );
    }

    public String getWxOauthUrl(String mchId,
                                String callbackUrl,
                                String type,
                                JSONObject params,
                                String key) {
        if (!wxOauthUrlBreaker.allowRequest()) {
            throw new IceeBootException("微信授权服务暂时不可用，请稍后重试");
        }

        try {
            String result = WxApi.getWxOauthUrl(mchId, callbackUrl, type, params, key);
            wxOauthUrlBreaker.onSuccess();
            return result;
        } catch (Exception e) {
            wxOauthUrlBreaker.onFailure();
            log.error("YunGouOS getWxOauthUrl 调用失败", e);
            throw new IceeBootException("微信授权服务调用失败：" + e.getMessage(), e);
        }
    }

    public WxWebLoginBiz getWebLogin(String mchId,
                                     String callbackUrl,
                                     JSONObject params,
                                     String key) {
        if (!webLoginBreaker.allowRequest()) {
            throw new IceeBootException("微信扫码登录服务暂时不可用，请稍后重试");
        }

        try {
            WxWebLoginBiz result = WxApi.getWebLogin(mchId, callbackUrl, params, key);
            webLoginBreaker.onSuccess();
            return result;
        } catch (Exception e) {
            webLoginBreaker.onFailure();
            log.error("YunGouOS getWebLogin 调用失败", e);
            throw new IceeBootException("微信扫码登录服务调用失败：" + e.getMessage(), e);
        }
    }

    public WxOauthInfo getWxOauthInfo(String mchId, String code, String key) {
        if (!wxOauthInfoBreaker.allowRequest()) {
            throw new IceeBootException("微信用户信息服务暂时不可用，请稍后重试");
        }

        try {
            WxOauthInfo result = WxApi.getWxOauthInfo(mchId, code, key);
            wxOauthInfoBreaker.onSuccess();
            return result;
        } catch (Exception e) {
            wxOauthInfoBreaker.onFailure();
            log.error("YunGouOS getWxOauthInfo 调用失败", e);
            throw new IceeBootException("微信用户信息服务调用失败：" + e.getMessage(), e);
        }
    }
}
