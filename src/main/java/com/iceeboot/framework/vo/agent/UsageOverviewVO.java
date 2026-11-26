package com.iceeboot.framework.vo.agent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public record UsageOverviewVO(LocalDateTime updatedAt, List<Row> models, List<Row> days) {
    @Data
    public static class Row {
        private String label;
        private String provider;
        private long calls;
        private long knownCalls;
        private long pricedCalls;
        private long inputTokens;
        private long outputTokens;
        private Long cachedTokens;
        private Long cacheWriteTokens;
        private BigDecimal costCny;
    }
}
