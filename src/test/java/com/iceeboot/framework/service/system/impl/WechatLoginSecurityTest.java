package com.iceeboot.framework.service.system.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.service.pay.client.YunGouWxApiClient;
import com.iceeboot.framework.controller.system.AuthController;
import com.iceeboot.framework.entity.system.SocialDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.service.system.*;
import com.iceeboot.framework.vo.system.LoginVO;
import com.yungouos.pay.entity.WxOauthInfo;
import com.yungouos.pay.entity.WxWebLoginBiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WechatLoginSecurityTest {
    static final String FRONTEND = "https://console.example/app/";
    RedisTemplate<String, Object> redis;
    ValueOperations<String, Object> values;
    YunGouWxApiClient wx;
    ISysUserService users;
    ISocialService socials;
    AuthServiceImpl service;

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        redis = mock(RedisTemplate.class); values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        wx = mock(YunGouWxApiClient.class); users = mock(ISysUserService.class); socials = mock(ISocialService.class);
        service = new AuthServiceImpl(users, mock(ISysUserRoleService.class), socials, redis,
                mock(TransactionTemplate.class), wx);
        ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND);
        ReflectionTestUtils.setField(service, "wechatCallbackUrl", "https://api.example/callback?channel=web");
        ReflectionTestUtils.setField(service, "mchId", "merchant-fixture");
        ReflectionTestUtils.setField(service, "key", "key-fixture");
        ReflectionTestUtils.setField(service, "stateExpireSeconds", 300);
        ReflectionTestUtils.setField(service, "ticketExpireSeconds", 120);
        ReflectionTestUtils.setField(service, "refreshTokenExpireSeconds", 604800L);
    }

    @ParameterizedTest @ValueSource(strings={"dev", "test", "prod", "prod,staging"})
    void everyProfileRejectsUnverifiedStateBeforeCallingWechat(String profile) {
        new ApplicationContextRunner().withUserConfiguration(AuthServiceImpl.class)
                .withBean(ISysUserService.class, () -> users)
                .withBean(ISysUserRoleService.class, () -> mock(ISysUserRoleService.class))
                .withBean(ISocialService.class, () -> socials)
                .withBean(RedisTemplate.class, () -> redis)
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .withBean(YunGouWxApiClient.class, () -> wx)
                .withPropertyValues("spring.profiles.active="+profile,"YunGouOS.key=fixture",
                        "YunGouOS.mchId=fixture", "YunGouOS.notifyUrl=https://api.example/notify",
                        "YunGouOS.frontendUrl="+FRONTEND, "YunGouOS.wechatCallbackUrl=https://api.example/callback",
                        "YunGouOS.stateSecret=fixture", "YunGouOS.ticketExpireSeconds=120")
                .run(context -> {
                    String result = context.getBean(AuthServiceImpl.class).handleWechatCallback("code-fixture", "unverified");
                    assertThat(URLDecoder.decode(result, StandardCharsets.UTF_8)).contains("安全验证失败");
                    verify(wx, never()).getWxOauthInfo(anyString(), anyString(), anyString());
                });
    }

    @Test void oauthUrlCarriesExactlyTheStateStoredInRedis() {
        when(wx.getWxOauthUrl(anyString(), anyString(), anyString(), any(), anyString()))
                .thenAnswer(call -> {
                    JSONObject params = call.getArgument(3);
                    assertThat(call.<String>getArgument(1)).contains("channel=web", "state="+params.getString("state"));
                    verify(values).set(eq(CacheConstants.Auth.WECHAT_STATE+params.getString("state")),
                            eq(params.getString("state")), eq(300L), any());
                    return "https://wechat.example/auth";
                });
        assertThat(service.getWechatAuthUrl()).isEqualTo("https://wechat.example/auth");
    }

    @Test void qrCallbackAlsoCarriesLocallyGeneratedState() {
        when(wx.getWebLogin(anyString(), anyString(), any(), anyString())).thenAnswer(call -> {
            JSONObject params = call.getArgument(2);
            assertThat(params.getString("state")).isNotBlank();
            assertThat(call.<String>getArgument(1)).contains("state="+params.getString("state"));
            return new WxWebLoginBiz();
        });
        service.getWechatQr();
    }

    @Test void stateIsConsumedEvenWhenTheUserDeclinesAndCannotBeReplayed() throws Exception {
        when(values.getAndDelete(CacheConstants.Auth.WECHAT_STATE+"once")).thenReturn("once", null);
        String declined = service.handleWechatCallback(null, "once");
        String replay = service.handleWechatCallback(null, "once");
        assertThat(URLDecoder.decode(declined, StandardCharsets.UTF_8)).contains("未同意授权");
        assertThat(URLDecoder.decode(replay, StandardCharsets.UTF_8)).contains("安全验证失败");
        verifyNoInteractions(wx);
    }

    @Test void ticketIsInFragmentAndNeverWrittenToAuthLogs() throws Exception {
        when(values.getAndDelete(CacheConstants.Auth.WECHAT_STATE+"once")).thenReturn("once");
        var info = mock(WxOauthInfo.class, RETURNS_DEEP_STUBS);
        when(info.getOpenId()).thenReturn("openid-fixture");
        when(wx.getWxOauthInfo(anyString(), anyString(), anyString())).thenReturn(info);
        var social = new SocialDO(); social.setUsername("fixture-user");
        when(socials.findByOpenIdAndPlatform(anyString(), any())).thenReturn(social);
        var user = new UserDO(); user.setId(7L); user.setStatus("active");
        when(users.getUserByUsername("fixture-user")).thenReturn(user);
        Logger logger = (Logger) LoggerFactory.getLogger(AuthServiceImpl.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try (var tokens = mockStatic(StpUtil.class)) {
            tokens.when(StpUtil::getTokenValue).thenReturn("access-fixture");
            tokens.when(StpUtil::getTokenTimeout).thenReturn(7200L);
            URI result = URI.create(service.handleWechatCallback("code-fixture", "once"));
            assertThat(result.getRawQuery()).isNull();
            assertThat(result.getRawFragment()).startsWith("ticket=");
            String ticket = result.getRawFragment().substring("ticket=".length());
            assertThat(appender.list).noneMatch(event -> event.getFormattedMessage().contains(ticket));
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @Test void controllerFailureUsesConfiguredFrontend() {
        var auth = mock(IAuthService.class);
        new ApplicationContextRunner().withUserConfiguration(AuthController.class)
                .withBean(IAuthService.class, () -> auth).withPropertyValues("YunGouOS.frontendUrl="+FRONTEND)
                .run(context -> {
                    when(auth.handleWechatCallback(any(), any())).thenThrow(new IllegalStateException("fixture"));
                    var response = new MockHttpServletResponse();
                    context.getBean(AuthController.class).handleWechatCallback("code", "state", response);
                    assertThat(response.getRedirectedUrl()).startsWith(FRONTEND+"?error=");
                });
    }

    @Test void ticketExchangeAcceptsJsonBodyAndLegacyQuery() throws Exception {
        var auth = mock(IAuthService.class);
        when(auth.exchangeTicket(anyString())).thenReturn(new LoginVO());
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth)).build();
        mvc.perform(post("/v1/auth/wechat/exchange-ticket").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticket\":\"body-fixture\"}")).andExpect(status().isOk());
        mvc.perform(post("/v1/auth/wechat/exchange-ticket").param("ticket","query-fixture"))
                .andExpect(status().isOk());
        verify(auth).exchangeTicket("body-fixture"); verify(auth).exchangeTicket("query-fixture");
    }
}
