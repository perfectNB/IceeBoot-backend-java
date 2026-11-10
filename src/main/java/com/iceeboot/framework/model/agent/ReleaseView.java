package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** 智能体运行时ReleaseView，仅供服务内部与发布快照使用。 */
public record ReleaseView(String id, int version, LocalDateTime createdAt, String createdBy) {}
