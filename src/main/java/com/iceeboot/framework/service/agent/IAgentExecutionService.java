package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.model.agent.*;

/** 智能体执行服务，调用方身份必须来自可信登录上下文。 */
public interface IAgentExecutionService {
    InvokeResult invoke(String code, InvokeRequest request, Caller caller);

    reactor.core.publisher.Flux<AgentProgress> stream(String code, InvokeRequest request, Caller caller);

    String invokeModel(ModelInvokeRequest request, Caller caller);

    void cancel(String id);
}
