package com.iceeboot.framework.service.message.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.config.message.NotificationProperties;
import com.iceeboot.framework.dto.create.message.NotificationDTO;
import com.iceeboot.framework.entity.message.NotificationDO;
import com.iceeboot.framework.entity.message.NotificationReadDO;
import com.iceeboot.framework.mapper.message.NotificationMapper;
import com.iceeboot.framework.mapper.message.NotificationReadMapper;
import com.iceeboot.framework.query.message.NotificationQuery;
import com.iceeboot.framework.service.database.DatabaseInitService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    NotificationServiceImpl service;
    NotificationMapper mapper;
    NotificationReadMapper readMapper;
    TransactionTemplate tx;
    MockedStatic<TokenUtils> tokens;

    @BeforeEach void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:notice_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        for (var entity : List.of(NotificationDO.class, NotificationReadDO.class)) {
            String table = entity == NotificationDO.class ? "t_notification" : "t_notification_read";
            String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                    "generateCreateTableSql", entity, table);
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        }
        var cfg = new MybatisConfiguration();
        cfg.addMapper(NotificationMapper.class); cfg.addMapper(NotificationReadMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds); factory.setConfiguration(cfg);
        var sql = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        mapper = sql.getMapper(NotificationMapper.class);
        readMapper = sql.getMapper(NotificationReadMapper.class);
        var properties = new NotificationProperties();
        properties.setApplicationCodes(List.of("PLATFORM", "PORTAL"));
        service = new NotificationServiceImpl(properties, mapper, readMapper);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tokens = mockStatic(TokenUtils.class);
        tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
    }
    @AfterEach void close() { if (tokens != null) tokens.close(); }

    NotificationDO published(String code, Long userId) {
        var row = new NotificationDO();
        row.setApplicationCode(code); row.setTargetUserId(userId);
        row.setTitle("fixture"); row.setContent("fixture"); row.setType("system");
        row.setStatus("PUBLISHED"); row.setPublishTime(LocalDateTime.now().minusMinutes(1));
        row.setDelFlag(0); mapper.insert(row); return row;
    }

    @Test void visibleMessagesRespectApplicationAudienceStatusAndExpiration() {
        published("ALL", null); published("PLATFORM", 7L);
        published("PLATFORM", 8L); published("PORTAL", null);
        var expired = published("PLATFORM", null);
        expired.setExpireTime(LocalDateTime.now().minusSeconds(1)); mapper.updateById(expired);
        var withdrawn = published("PLATFORM", null);
        withdrawn.setStatus("WITHDRAWN"); mapper.updateById(withdrawn);
        assertThat(service.getUnreadCount("PLATFORM")).isEqualTo(2);
        assertThat(service.getLatestUnreadImportant("PLATFORM")).isNotNull();
        assertThatThrownBy(() -> tx.execute(s -> service.markRead(expired.getId(), "PLATFORM")))
                .isInstanceOf(IceeBootException.class);
    }
    @Test void repeatedReadCreatesOneRecordAndUnreadCountDecreases() {
        var row = published("PLATFORM", 7L);
        assertThat(service.getUnreadCount("PLATFORM")).isEqualTo(1);
        tx.execute(s -> service.markRead(row.getId(), "PLATFORM"));
        tx.execute(s -> service.markRead(row.getId(), "PLATFORM"));
        assertThat(readMapper.selectCount(null)).isEqualTo(1);
        assertThat(service.getUnreadCount("PLATFORM")).isZero();
    }
    @Test void withdrawnDraftCanClearPreviousTargetAndExpiry() {
        var row = published("PLATFORM", 7L);
        row.setExpireTime(LocalDateTime.now().plusDays(1)); mapper.updateById(row);
        service.withdraw(row.getId());
        var dto = new NotificationDTO();
        dto.setId(row.getId()); dto.setApplicationCode("PLATFORM");
        dto.setTitle("updated"); dto.setContent("updated"); dto.setType("system");
        tx.execute(s -> service.saveDraft(dto));
        var saved = mapper.selectById(row.getId());
        assertThat(saved.getTargetUserId()).isNull(); assertThat(saved.getExpireTime()).isNull();
        assertThat(saved.getPublishTime()).isNull(); assertThat(saved.getStatus()).isEqualTo("DRAFT");
    }
    @Test void unsupportedAndBroadcastApplicationCannotBeUsedAsUserScope() {
        assertThatThrownBy(() -> service.getUnreadCount("ALL")).isInstanceOf(IceeBootException.class);
        assertThatThrownBy(() -> service.getUserPage("UNKNOWN", new NotificationQuery())).isInstanceOf(IceeBootException.class);
        assertThatThrownBy(() -> tx.execute(s -> service.markAllRead("UNKNOWN"))).isInstanceOf(IceeBootException.class);
    }
    @Test void configurationUsesDeveloperSuppliedCodes() {
        var properties = new NotificationProperties();
        properties.setApplicationCodes(List.of(" portal ", "PORTAL", "PLATFORM"));
        assertThat(properties.getApplicationCodes()).containsExactly("PORTAL", "PLATFORM");
        assertThatThrownBy(() -> properties.setApplicationCodes(List.of("ALL"))).isInstanceOf(IllegalArgumentException.class);
    }
}
