package com.iceeboot.framework.enums.agent;

public enum AgentScheduleExecutionStatus {
    RUNNING, SUCCEEDED, FAILED, CANCELLED, TIMED_OUT, INTERRUPTED,
    SKIPPED_OVERLAP, SKIPPED_MISFIRE
}
