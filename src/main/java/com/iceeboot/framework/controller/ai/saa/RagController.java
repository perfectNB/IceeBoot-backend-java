package com.iceeboot.framework.controller.ai.saa;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * rag
 *
 * @auther CodeIcee
 * @create 2025-09-25
 */
@RestController
@RequestMapping("/v1/saa")
@Slf4j
public class RagController {
    @Resource(name = "qwenChatClient")
    private ChatClient chatClient;
    @Resource
    private VectorStore vectorStore;


    @GetMapping("/rag")
    public Flux<String> rag(@RequestParam(name = "msg", defaultValue = "锁舞") String msg) {
        String systemInfo = """
                你是剧本杀dm,按照给出的问题给出对应的解释,如果找不到对应的信息，就回复暂无信息😠
                """;

        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                .build();

        return chatClient
                .prompt()
                .system(systemInfo)
                .user(msg)
                .advisors(advisor)
                .stream()
                .content();
    }
}
