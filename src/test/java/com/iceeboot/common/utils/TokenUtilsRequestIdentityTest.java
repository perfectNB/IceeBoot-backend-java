package com.iceeboot.common.utils;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.iceeboot.config.mybatisplus.MybatisPlusConfig;
import com.iceeboot.framework.entity.system.RolePermissionDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** 验证批量审计字段填充不逐条访问登录存储，身份也不会跨请求保留。 */
class TokenUtilsRequestIdentityTest {
    private MockHttpServletRequest request;
    private MockedStatic<SaHolder> holder;
    private MockedStatic<StpUtil> auth;

    @BeforeEach
    void setUp() {
        bindRequest("current-token");
        var saRequest = mock(SaRequest.class);
        when(saRequest.getHeader(anyString())).thenAnswer(call -> request.getHeader(call.getArgument(0)));
        holder = mockStatic(SaHolder.class);
        holder.when(SaHolder::getRequest).thenReturn(saRequest);
        auth = mockStatic(StpUtil.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "identity-test"),
                RolePermissionDO.class);
    }

    @AfterEach
    void tearDown() {
        if (auth != null) auth.close();
        if (holder != null) holder.close();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void threeHundredAuditRowsReadIdentityOnlyOnce() {
        auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
        var handler = new MybatisPlusConfig.MyMetaObjectHandler();
        for (int i = 0; i < 300; i++) {
            var row = new RolePermissionDO();
            handler.insertFill(SystemMetaObject.forObject(row));
            assertThat(row.getCreateBy()).isEqualTo("7");
        }
        auth.verify(() -> StpUtil.getLoginIdByToken("current-token"), times(1));
    }

    @Test
    void newRequestOnTheSameThreadRevalidatesARevokedToken() {
        auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L).thenReturn(null);
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        bindRequest("current-token");
        assertThat(TokenUtils.getCurrentUserId()).isNull();
        auth.verify(() -> StpUtil.getLoginIdByToken("current-token"), times(2));
    }

    @Test
    void changedTokenWithinARequestCannotReuseAnotherIdentity() {
        auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L);
        auth.when(() -> StpUtil.getLoginIdByToken("different-token")).thenReturn(8L);
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        request.removeHeader("Authorization");
        request.addHeader("Authorization", "Bearer different-token");
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(8L);
    }

    @Test
    void failuresAndUnknownIdentitiesAreNotCached() {
        auth.when(() -> StpUtil.getLoginIdByToken("current-token"))
                .thenThrow(new RedisConnectionFailureException("fixture"))
                .thenReturn(null).thenReturn(7L);
        assertThat(TokenUtils.getCurrentUserId()).isNull();
        assertThat(TokenUtils.getCurrentUserId()).isNull();
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        auth.verify(() -> StpUtil.getLoginIdByToken("current-token"), times(3));
    }

    @Test
    void logoutInvalidatesTheCurrentRequestSnapshot() {
        auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L).thenReturn(null);
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        TokenUtils.logoutByToken("current-token");
        assertThat(TokenUtils.getCurrentUserId()).isNull();
        auth.verify(() -> StpUtil.logoutByTokenValue("current-token"));
    }

    @Test
    void explicitTokenValidationStillReadsTheCurrentLoginState() {
        auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L).thenReturn(null);
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(TokenUtils.isValidToken("current-token")).isFalse();
        assertThat(TokenUtils.getUserIdByToken("current-token")).isNull();
    }

    @Test
    void callsWithoutAnHttpRequestDoNotKeepAnIdentity() {
        RequestContextHolder.resetRequestAttributes();
        auth.when(() -> StpUtil.getLoginIdByToken("current-token")).thenReturn(7L).thenReturn(null);
        assertThat(TokenUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(TokenUtils.getCurrentUserId()).isNull();
    }

    private void bindRequest(String token) {
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
