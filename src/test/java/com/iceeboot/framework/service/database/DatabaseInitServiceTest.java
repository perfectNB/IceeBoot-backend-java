package com.iceeboot.framework.service.database;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class DatabaseInitServiceTest {
    private final DatabaseInitService service = new DatabaseInitService(null);
    static class Fixture {
        @TableField(numericScale="4") BigDecimal preciseAmount;
        BigDecimal amount;
        @TableField(numericScale="-1") BigDecimal invalid;
        @TableField(numericScale="19") BigDecimal oversized;
        @TableField(numericScale="invalid") BigDecimal malformed;
        String content;
    }
    @Test void explicitDecimalScaleIsPreserved() throws Exception {
        assertThat(service.getColumnType(Fixture.class.getDeclaredField("preciseAmount"))).isEqualTo("decimal(18,4)");
    }
    @Test void ordinaryAmountsKeepTheirExistingPrecision() throws Exception {
        assertThat(service.getColumnType(Fixture.class.getDeclaredField("amount"))).isEqualTo("decimal(10,2)");
    }
    @Test void invalidScaleFailsInsteadOfSilentlyChangingPrecision() {
        for(String field : new String[]{"invalid", "oversized", "malformed"}) {
            assertThatThrownBy(() -> service.getColumnType(Fixture.class.getDeclaredField(field))).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void contentFieldsUseText() throws Exception {
        assertThat(service.getColumnType(Fixture.class.getDeclaredField("content"))).isEqualTo("text");
    }
}
