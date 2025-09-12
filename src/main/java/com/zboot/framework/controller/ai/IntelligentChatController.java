/**
 * 智能对话控制器
 * 基于 Spring AI Alibaba 实现 Function Calling 功能
 * 支持数据库查询和普通对话的智能切换
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.zboot.framework.controller.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zboot.common.core.domain.R;
import com.zboot.common.utils.TokenUtils;
import com.zboot.framework.llm.service.DatabaseQueryFunction;
import com.zboot.framework.dto.create.ai.StreamChatRequest;
import com.zboot.framework.entity.ai.AiChatMessageDO;
import com.zboot.framework.mapper.ai.AiChatMessageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能对话控制器
 */
@Slf4j
@Tag(name = "智能对话", description = "支持数据库查询和普通对话的智能助手")
@RestController
@RequestMapping("/v1/chat/intelligent")
@RequiredArgsConstructor
public class IntelligentChatController {

    private final ChatModel chatModel;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final DatabaseQueryFunction databaseQueryFunction;
    
    // 数据库查询相关关键词
    private static final Set<String> DB_QUERY_KEYWORDS = Set.of(
        "查询", "查找", "搜索", "统计", "多少", "几个", "数量",
        "用户", "注册", "活动", "任务", "文件", "角色", "权限",
        "最近", "今天", "昨天", "本周", "本月", "最新",
        "状态", "激活", "禁用", "正常", "异常"
    );

    /**
     * 智能对话（非流式）
     * 自动判断是否需要查询数据库
     */
    @Operation(summary = "智能对话", description = "支持数据库查询和普通对话")
    @PostMapping("/chat")
    public R<String> chat(@Validated @RequestBody StreamChatRequest request) {
        String userInput = request.getContent();
        if (userInput == null || userInput.isBlank()) {
            return R.fail("对话内容不能为空");
        }
        
        try {
            Long userId = TokenUtils.getCurrentUserId();
            
            // 1. 保存用户消息
            saveUserMessage(request.getSessionId(), userInput, userId);
            
            // 2. 获取对话历史
            List<Message> messages = getConversationHistory(request.getSessionId(), 10);
            messages.add(new UserMessage(userInput));
            
            // 3. 判断是否需要数据库查询
            String response;
            if (isDataQueryIntent(userInput)) {
                // 使用 Function Calling 进行数据库查询
                response = chatWithFunctionCalling(messages);
            } else {
                // 普通对话
                response = chatModel
                    .call(new Prompt(messages))
                    .getResult()
                    .getOutput()
                    .getContent();
            }
            
            // 4. 保存助手回复
            saveAssistantMessage(request.getSessionId(), response, userId);
            
            return R.ok(response);
            
        } catch (Exception e) {
            log.error("智能对话失败: {}", e.getMessage(), e);
            return R.fail("对话失败: " + e.getMessage());
        }
    }

    /**
     * 智能对话（流式）
     * 支持 SSE 流式输出
     */
    @Operation(summary = "智能流式对话", description = "支持数据库查询和普通对话的流式输出")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Validated @RequestBody StreamChatRequest request) {
        String userInput = request.getContent();
        if (userInput == null || userInput.isBlank()) {
            return Flux.just("data: {\"error\":\"对话内容不能为空\",\"finished\":true}\n\n");
        }

        try {
            Long userId = TokenUtils.getCurrentUserId();
            
            // 1. 保存用户消息
            saveUserMessage(request.getSessionId(), userInput, userId);
            
            // 2. 获取对话历史
            List<Message> messages = getConversationHistory(request.getSessionId(), 10);
            messages.add(new UserMessage(userInput));
            
            // 3. 判断是否需要数据库查询
            if (isDataQueryIntent(userInput)) {
                // 数据库查询不支持流式，直接返回结果
                String response = chatWithFunctionCalling(messages);
                saveAssistantMessage(request.getSessionId(), response, userId);
                
                String escaped = response.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                return Flux.just(
                    "data: {\"content\":\"" + escaped + "\",\"finished\":false}\n\n",
                    "data: {\"finished\":true}\n\n"
                );
            } else {
                // 普通对话流式输出
                return streamNormalChat(messages, request.getSessionId(), userId);
            }
            
        } catch (Exception e) {
            log.error("智能流式对话失败: {}", e.getMessage(), e);
            return Flux.just("data: {\"error\":\"对话失败\",\"finished\":true}\n\n");
        }
    }

    /**
     * 使用 Function Calling 进行对话
     */
    private String chatWithFunctionCalling(List<Message> messages) {
        try {
            // 创建带有 Function 的 Prompt
            // 使用 Bean 名称 "databaseQuery" 来引用注册的函数
            Prompt prompt = new Prompt(messages, 
                DashScopeChatOptions.builder()
                    .withFunction("databaseQuery") // 使用 Bean 名称
                    .build());
            
            // 调用模型
            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getContent();
            
        } catch (Exception e) {
            log.error("Function Calling 执行失败", e);
            return "抱歉，数据查询功能暂时不可用，请稍后再试。";
        }
    }

    /**
     * 普通对话流式输出
     */
    private Flux<String> streamNormalChat(List<Message> messages, Long sessionId, Long userId) {
        AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());
        
        Flux<ChatResponse> stream = chatModel.stream(new Prompt(messages));
        return stream
            .map(resp -> {
                String piece = resp.getResult().getOutput().getContent();
                if (piece == null) piece = "";
                
                // 收集完整内容
                fullContent.get().append(piece);
                
                String escaped = piece.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                return "data: {\"content\":\"" + escaped + "\",\"finished\":false}\n\n";
            })
            .doOnComplete(() -> {
                // 流式响应完成，保存完整的助手回复
                String content = fullContent.get().toString();
                if (!content.isEmpty()) {
                    saveAssistantMessage(sessionId, content, userId);
                }
            })
            .onErrorResume(e -> {
                log.error("流式对话失败: {}", e.getMessage(), e);
                return Mono.just("data: {\"error\":\"" + e.getMessage().replace("\"", "'") + "\",\"finished\":true}\n\n");
            })
            .concatWith(Mono.just("data: {\"finished\":true}\n\n"));
    }

    /**
     * 判断是否为数据查询意图
     */
    private boolean isDataQueryIntent(String userInput) {
        String lowerInput = userInput.toLowerCase();
        
        // 检查是否包含数据库查询关键词
        for (String keyword : DB_QUERY_KEYWORDS) {
            if (lowerInput.contains(keyword)) {
                log.debug("检测到数据查询意图，关键词: {}", keyword);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取对话历史记忆
     */
    private List<Message> getConversationHistory(Long sessionId, int limit) {
        List<Message> messages = new ArrayList<>();
        
        try {
            // 从数据库获取历史消息，按时间正序排列
            LambdaQueryWrapper<AiChatMessageDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiChatMessageDO::getSessionId, sessionId)
                   .orderByAsc(AiChatMessageDO::getCreateTime)
                   .last("LIMIT " + limit);
            
            List<AiChatMessageDO> historyMessages = aiChatMessageMapper.selectList(wrapper);
            
            // 转换为Spring AI Message格式
            for (AiChatMessageDO message : historyMessages) {
                if ("user".equals(message.getRole())) {
                    messages.add(new UserMessage(message.getContent()));
                } else if ("assistant".equals(message.getRole())) {
                    messages.add(new AssistantMessage(message.getContent()));
                }
            }
            
            log.debug("获取智能对话历史消息，sessionId: {}, 消息数量: {}", sessionId, messages.size());
            
        } catch (Exception e) {
            log.error("获取智能对话历史失败: {}", e.getMessage(), e);
        }
        
        return messages;
    }
    
    /**
     * 保存用户消息
     */
    private void saveUserMessage(Long sessionId, String content, Long userId) {
        try {
            AiChatMessageDO userMessage = new AiChatMessageDO();
            userMessage.setSessionId(sessionId);
            userMessage.setRole("user");
            userMessage.setContent(content);
            aiChatMessageMapper.insert(userMessage);
            log.debug("保存用户消息成功，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存助手消息
     */
    private void saveAssistantMessage(Long sessionId, String content, Long userId) {
        try {
            AiChatMessageDO assistantMessage = new AiChatMessageDO();
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(content);
            assistantMessage.setCreateBy(String.valueOf(userId));
            assistantMessage.setUpdateBy(String.valueOf(userId));
            aiChatMessageMapper.insert(assistantMessage);
            log.debug("保存助手消息成功，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("保存助手消息失败: {}", e.getMessage(), e);
        }
    }
}