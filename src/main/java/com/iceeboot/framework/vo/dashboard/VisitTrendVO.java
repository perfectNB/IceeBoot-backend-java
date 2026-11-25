package com.iceeboot.framework.vo.dashboard;

import lombok.Data;

@Data
public class VisitTrendVO {
    private String date;
    private long logCount;
    private long errorCount;
}
