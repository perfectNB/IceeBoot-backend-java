package com.iceeboot.framework.controller.ai.saa;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @auther CodeIcee
 * @since 2025-10-11
 */
@Tag(name = "调用百炼知识库", description = "调用百炼知识库")
@RestController
@RequestMapping("/v1/saa/bailian")
@Slf4j
public class BailianRagController {
    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;
    @Resource
    private DashScopeApi dashScopeApi;


    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam(name = "msg", defaultValue = "有哪些剧本杀") String msg) {
        // 百炼 RAG 构建器
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName("剧本杀天眼") // 知识库名称
                        .build()
        );

        return qwenChatClient.prompt()
                .user(msg)
                .advisors(new DocumentRetrievalAdvisor(retriever))
                .stream()
                .content();
    }

}
