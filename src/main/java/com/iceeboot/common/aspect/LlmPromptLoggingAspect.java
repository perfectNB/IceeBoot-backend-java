package com.iceeboot.common.aspect;

import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import com.iceeboot.common.service.ai.llm.utils.LlmPromptLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 大模型提示词日志切面。
 *
 * <p>统一打印 Spring AI 和自定义 LLMService 两套调用链发给模型的提示词，
 * 避免在每个业务调用点重复埋点。</p>
 */
@Slf4j
@Aspect
@Component
public class LlmPromptLoggingAspect {

    @Value("${iceeboot.common.ai.log-prompts:false}")
    private boolean logPromptsEnabled;

    @Before("execution(* org.springframework.ai.chat.model.ChatModel.call(org.springframework.ai.chat.prompt.Prompt)) && args(prompt)")
    public void logSpringAiCall(JoinPoint joinPoint, Prompt prompt) {
        if (!logPromptsEnabled || !log.isInfoEnabled()) {
            return;
        }
        String executor = joinPoint.getTarget() == null ? "unknown" : joinPoint.getTarget().getClass().getSimpleName();
        log.info("\n{}", LlmPromptLogUtils.buildSpringAiPromptLog("call", executor, prompt));
    }

    @Before("execution(* org.springframework.ai.chat.model.StreamingChatModel.stream(org.springframework.ai.chat.prompt.Prompt)) && args(prompt)")
    public void logSpringAiStream(JoinPoint joinPoint, Prompt prompt) {
        if (!logPromptsEnabled || !log.isInfoEnabled()) {
            return;
        }
        String executor = joinPoint.getTarget() == null ? "unknown" : joinPoint.getTarget().getClass().getSimpleName();
        log.info("\n{}", LlmPromptLogUtils.buildSpringAiPromptLog("stream", executor, prompt));
    }

    @Before("execution(* com.iceeboot.common.service.ai.llm.service.LLMService.chat(com.iceeboot.common.service.ai.llm.dto.LLMRequest)) && args(request)")
    public void logCustomLlmChat(JoinPoint joinPoint, LLMRequest request) {
        if (!logPromptsEnabled || !log.isInfoEnabled()) {
            return;
        }
        log.info("\n{}", LlmPromptLogUtils.buildLlmRequestLog("chat", resolveProvider(joinPoint), request));
    }

    @Before("execution(* com.iceeboot.common.service.ai.llm.service.LLMService.chatStream(com.iceeboot.common.service.ai.llm.dto.LLMRequest)) && args(request)")
    public void logCustomLlmStream(JoinPoint joinPoint, LLMRequest request) {
        if (!logPromptsEnabled || !log.isInfoEnabled()) {
            return;
        }
        log.info("\n{}", LlmPromptLogUtils.buildLlmRequestLog("chatStream", resolveProvider(joinPoint), request));
    }

    private String resolveProvider(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        if (target instanceof LLMService llmService) {
            try {
                return llmService.getProvider();
            } catch (Exception ignored) {
                return target.getClass().getSimpleName();
            }
        }
        return target == null ? "unknown" : target.getClass().getSimpleName();
    }
}
