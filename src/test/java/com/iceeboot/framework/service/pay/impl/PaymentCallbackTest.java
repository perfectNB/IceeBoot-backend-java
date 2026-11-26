package com.iceeboot.framework.service.pay.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.common.constant.PaymentBusinessStatuses;
import com.iceeboot.common.service.pay.client.YunGouPayClient;
import com.iceeboot.common.service.pay.factory.PayFactory;
import com.iceeboot.common.service.pay.service.PayService;
import com.iceeboot.framework.entity.pay.PayConfigDO;
import com.iceeboot.framework.entity.pay.PayRecordDO;
import com.iceeboot.framework.mapper.pay.PayRecordMapper;
import com.iceeboot.framework.service.database.DatabaseInitService;
import com.iceeboot.framework.service.pay.IPayConfigService;
import com.iceeboot.framework.service.system.ISysUserService;
import com.yungouos.pay.util.PaySignUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用真实 MyBatis SQL、事务和内存数据库，验证回调校验及重复处理。 */
class PaymentCallbackTest {
    static final String KEY = "fixture-signing-key";
    PayRecordServiceImpl service;
    PayRecordMapper mapper;
    com.iceeboot.framework.mapper.pay.PayTermsMapper terms;
    JdbcTemplate jdbc;
    PayService strategy;
    TransactionTemplate tx;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:pay_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                "generateCreateTableSql", PayRecordDO.class, "t_pay_record");
        new JdbcTemplate(dataSource).execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        jdbc = new JdbcTemplate(dataSource);
        try (var connection = dataSource.getConnection()) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/agent/V023__payment_entitlements.sql"));
        }
        var configuration = new MybatisConfiguration();
        configuration.addMapper(com.iceeboot.framework.mapper.pay.PayTermsMapper.class);
        configuration.addMapper(PayRecordMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/pay/PayRecordMapper.xml"));
        mapper = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(PayRecordMapper.class);
        terms = new SqlSessionTemplate(factory.getObject()).getMapper(com.iceeboot.framework.mapper.pay.PayTermsMapper.class);
        var configs = mock(IPayConfigService.class);
        var payFactory = mock(PayFactory.class);
        strategy = mock(PayService.class);
        var config = new PayConfigDO();
        config.setPayCode("TEST_HOOK");
        when(configs.getById(5L)).thenReturn(config);
        when(payFactory.hasStrategy("TEST_HOOK")).thenReturn(true);
        when(payFactory.getStrategy("TEST_HOOK")).thenReturn(strategy);
        service = new PayRecordServiceImpl(configs, mock(RedisTemplate.class), payFactory,
                mock(ISysUserService.class), mock(YunGouPayClient.class), terms, java.util.List.of());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "key", KEY);
        ReflectionTestUtils.setField(service, "mchId", "merchant-fixture");
        tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        var order = new PayRecordDO().setOutTradeNo("order-fixture").setConfigId(5L)
                .setPayAmount(1250).setPayStatus(1).setUserId(7L)
                .setBusinessStatus(PaymentBusinessStatuses.PENDING);
        order.setDelFlag(0);
        mapper.insert(order);
    }

    MockHttpServletRequest callback(String code, String money, String merchant) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("code", code);
        fields.put("orderNo", "platform-fixture");
        fields.put("outTradeNo", "order-fixture");
        fields.put("payNo", "transaction-fixture");
        fields.put("money", money);
        fields.put("mchId", merchant);
        var request = new MockHttpServletRequest();
        fields.forEach((name, value) -> request.setParameter(name, value.toString()));
        request.setParameter("sign", PaySignUtil.createSign(fields, KEY));
        return request;
    }

    String call(MockHttpServletRequest request) {
        return tx.execute(status -> service.handlePaymentCallback(request));
    }

    PayRecordDO order() { return service.getByOutTradeNo("order-fixture"); }

    @ParameterizedTest
    @CsvSource({"0,12.50,merchant-fixture", "1,12.51,merchant-fixture",
            "1,12.501,merchant-fixture", "1,12.50,other-merchant", "1,invalid,merchant-fixture"})
    void signedButInvalidCallbacksCannotMarkPaid(String code, String money, String merchant) {
        assertThat(call(callback(code, money, merchant))).isEqualTo("fail");
        assertThat(order().getPayStatus()).isEqualTo(1);
        verifyNoInteractions(strategy);
    }

    @Test
    void invalidSignatureCannotMarkPaid() {
        var request = callback("1", "12.50", "merchant-fixture");
        request.setParameter("sign", "incorrect");
        assertThat(call(request)).isEqualTo("fail");
        assertThat(order().getPayStatus()).isEqualTo(1);
        verifyNoInteractions(strategy);
    }

    @Test
    void duplicateValidCallbackSucceedsAndExecutesStrategyOnceWithoutRedisOrderFlag() {
        assertThat(call(callback("1", "12.50", "merchant-fixture"))).isEqualTo("SUCCESS");
        assertThat(call(callback("1", "12.50", "merchant-fixture"))).isEqualTo("SUCCESS");
        assertThat(order().getPayStatus()).isEqualTo(2);
        assertThat(order().getBusinessStatus()).isEqualTo(PaymentBusinessStatuses.SUCCESS);
        verify(strategy, times(1)).execute(eq(7L), any());
    }

    @Test
    void failedStrategyCanBeRetriedWithoutLosingPayment() {
        doThrow(new IllegalStateException("temporary fixture failure")).doNothing().when(strategy).execute(eq(7L), any());
        assertThat(call(callback("1", "12.50", "merchant-fixture"))).isEqualTo("SUCCESS");
        assertThat(order().getPayStatus()).isEqualTo(2);
        assertThat(order().getBusinessStatus()).isEqualTo(PaymentBusinessStatuses.FAILED);
        Boolean retried = tx.execute(status -> service.retryPaymentBusiness(order().getId()));
        assertThat(retried).isTrue();
        assertThat(order().getBusinessStatus()).isEqualTo(PaymentBusinessStatuses.SUCCESS);
        verify(strategy, times(2)).execute(eq(7L), any());
    }

    @Test
    void officialNotificationMatchesChannelAmountAndTransactionBeforeGrant() {
        var channel = com.iceeboot.framework.enums.pay.PaymentChannel.ALIPAY;
        var notice = new com.iceeboot.common.service.pay.client.VerifiedPayment("order-fixture", "txn", 1250);
        terms.save(order().getId(), channel, "TEST_HOOK");
        assertThat(call(callback("1", "12.50", "merchant-fixture"))).isEqualTo("fail");
        assertThatThrownBy(() -> service.confirmPayment(com.iceeboot.framework.enums.pay.PaymentChannel.WECHAT_NATIVE, notice))
                .hasMessageContaining("订单不符");
        var wrong = new com.iceeboot.common.service.pay.client.VerifiedPayment("order-fixture", "txn", 1249);
        assertThatThrownBy(() -> service.confirmPayment(channel, wrong)).hasMessageContaining("订单不符");
        tx.executeWithoutResult(s -> service.confirmPayment(channel, notice));
        tx.executeWithoutResult(s -> service.confirmPayment(channel, notice));
        verify(strategy, times(1)).execute(eq(7L), any());
        assertThat(order().getTradeNo()).isEqualTo("txn");
    }

    @Test
    void permanentMembershipPersistsOnlyOnceForThePaidOrder() {
        PayService member = new PayFactory(terms).getStrategy("SPA_MEMBER");
        member.execute(7L, order());
        member.execute(7L, order());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_pay_entitlement WHERE user_id=7 AND pay_code='SPA_MEMBER'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentCallbacksExecuteStrategyOnce() throws Exception {
        var start = new CyclicBarrier(2);
        Callable<String> callback = () -> {
            start.await();
            return call(callback("1", "12.50", "merchant-fixture"));
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(callback);
            var second = executor.submit(callback);
            assertThat(first.get()).isEqualTo("SUCCESS");
            assertThat(second.get()).isEqualTo("SUCCESS");
        }
        verify(strategy, times(1)).execute(eq(7L), any());
    }
}
