package com.iceeboot.framework.model.agent;

/** SDK 状态槽的持久化信封，整体加密后写入当前会话。 */
public record RuntimeStateEnvelope(String user, String session, String key, boolean list, boolean deleted, String payload) {}
