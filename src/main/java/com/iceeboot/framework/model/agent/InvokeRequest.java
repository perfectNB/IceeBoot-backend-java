package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

/** 智能体运行时InvokeRequest，仅供服务内部与发布快照使用。 */
public record InvokeRequest(
        @NotBlank @Size(max = 100000) String input, @Size(max = 100) String sessionId,
        @Size(max=5) java.util.List<String> attachmentIds) {
    public InvokeRequest(String input,String sessionId) { this(input,sessionId,java.util.List.of()); }
}
