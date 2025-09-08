package com.zboot.framework.service.ai;

import com.zboot.common.service.llm.dto.LLMResponse;
import com.zboot.framework.dto.create.ai.StreamChatRequest;
import reactor.core.publisher.Flux;

/**
 * 流式对话服务接口
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
public interface IStreamChatService {

    /**
     * 流式对话
     * 
     * @param request 流式对话请求
     * @return 流式响应
     */
    Flux<LLMResponse> streamChat(StreamChatRequest request);
}