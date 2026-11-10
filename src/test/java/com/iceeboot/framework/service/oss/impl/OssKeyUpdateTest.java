package com.iceeboot.framework.service.oss.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.update.oss.OssKeyUpdateDTO;
import com.iceeboot.framework.entity.oss.OssKeyDO;
import com.iceeboot.framework.mapper.oss.OssKeyMapper;
import com.iceeboot.common.service.oss.factory.OSSFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Objects;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real MyBatis/H2 persistence proves that omitted credentials remain untouched. */
class OssKeyUpdateTest {
    OssKeyServiceImpl service;OssKeyMapper mapper;
    static final String AK="fixture-access-123456",SK="fixture-secret-654321";
    @BeforeEach void setup() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:oss_update_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var init=new com.iceeboot.framework.service.database.DatabaseInitService(null);
        String ddl=ReflectionTestUtils.invokeMethod(init,"generateCreateTableSql",OssKeyDO.class,"t_oss_key");
        new JdbcTemplate(source).execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$",""));
        var config=new MybatisConfiguration();config.addMapper(OssKeyMapper.class);
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(source);factory.setConfiguration(config);
        mapper=new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(OssKeyMapper.class);
        service=new OssKeyServiceImpl(mock(OSSFactory.class));ReflectionTestUtils.setField(service,"baseMapper",mapper);
        var row=new OssKeyDO();row.setId(42L);row.setOssProvider("qiniu");row.setAccessKeyId(AK);row.setAccessKeySecret(SK);row.setStatus("active");row.setDefaultBucket("");mapper.insert(row);
    }
    OssKeyUpdateDTO edit(){var dto=new OssKeyUpdateDTO();dto.setId(42L);dto.setOssProvider("qiniu");dto.setDefaultBucket(" sample-bucket ");return dto;}
    @Test void settingOnlyBucketDoesNotClearOrRewriteSavedCredentials() {
        var dto=edit();dto.setAccessKeyId("");dto.setAccessKeySecret("");assertThat(service.updateOssKey(dto)).isTrue();
        var row=mapper.selectById(42L);assertThat(row.getDefaultBucket()).isEqualTo("sample-bucket");assertThat(row.getAccessKeyId()).isEqualTo(AK);assertThat(row.getAccessKeySecret()).isEqualTo(SK);assertThat(row.getStatus()).isEqualTo("active");
    }
    @Test void legacyMaskedValuesFromTheActualJacksonSerializerCannotCorruptSavedCredentials() throws Exception {
        var masked=new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(service.getOssKeyById(42L)));
        assertThat(masked.get("accessKeySecret").asText()).contains("*");
        var dto=edit();dto.setAccessKeyId(masked.get("accessKeyId").asText());dto.setAccessKeySecret(masked.get("accessKeySecret").asText());
        service.updateOssKey(dto);assertThat(mapper.selectById(42L).getAccessKeySecret()).isEqualTo(SK);assertThat(mapper.selectById(42L).getAccessKeyId()).isEqualTo(AK);
    }
    @Test void completeNewCredentialsCanBeExplicitlyReplaced() {
        var dto=edit();dto.setAccessKeyId("new-fixture-access");dto.setAccessKeySecret("new-fixture-secret");service.updateOssKey(dto);
        assertThat(mapper.selectById(42L).getAccessKeySecret()).isEqualTo("new-fixture-secret");
    }
    @Test void staleMaskCannotOverwriteARecentlyRotatedCredential() {
        var dto=edit();dto.setAccessKeySecret("old-********0000");assertThatThrownBy(()->service.updateOssKey(dto)).isInstanceOf(IceeBootException.class);
        assertThat(mapper.selectById(42L).getAccessKeySecret()).isEqualTo(SK);assertThat(mapper.selectById(42L).getDefaultBucket()).isEmpty();
    }
    @Test void existingHttpUpdateAcceptsMetadataOnlyAndKeepsCredentials() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.iceeboot.framework.controller.oss.OssKeyController(service))
                .setControllerAdvice(new com.iceeboot.common.exception.GlobalExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/v1/oss-keys")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"id\":42,\"ossProvider\":\"qiniu\",\"defaultBucket\":\"sample-bucket\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200));
        assertThat(mapper.selectById(42L).getAccessKeySecret()).isEqualTo(SK);
        assertThat(mapper.selectById(42L).getDefaultBucket()).isEqualTo("sample-bucket");
    }
    @Test void invalidRecordOrProviderNeverCreatesAnUpsert() {
        var dto=edit();dto.setId(null);assertThatThrownBy(()->service.updateOssKey(dto)).isInstanceOf(IceeBootException.class);
        dto.setId(999L);assertThatThrownBy(()->service.updateOssKey(dto)).isInstanceOf(IceeBootException.class);
        dto.setId(42L);dto.setOssProvider("aliyun");assertThatThrownBy(()->service.updateOssKey(dto)).isInstanceOf(IceeBootException.class);
    }
}
