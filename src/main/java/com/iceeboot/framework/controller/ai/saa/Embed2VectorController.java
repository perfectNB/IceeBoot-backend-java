package com.iceeboot.framework.controller.ai.saa;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 向量数据库
 *
 * @auther CodeIcee
 * @since 2025-09-24
 */
@Tag(name = "向量数据库", description = "向量数据库")
@RestController
@RequestMapping("/v1/saa")
@Slf4j
public class Embed2VectorController {
    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    @Qualifier("vectorStore")
    private VectorStore vectorStore;

    /**
     * 文本向量化
     *
     * @param msg
     * @return
     */
    @GetMapping("/text2embed")
    public EmbeddingResponse text2Embed(@RequestParam(name = "msg", defaultValue = "斗破苍穹") String msg) {

        EmbeddingResponse embeddingResponse = embeddingModel.call(new EmbeddingRequest(List.of(msg),
                DashScopeEmbeddingOptions.builder().withModel("text-embedding-v3").build()));

        log.info(Arrays.toString(embeddingResponse.getResult().getOutput()));

        return embeddingResponse;
    }


    /**
     * 文本向量化 后存入向量数据库RedisStack
     */

    @GetMapping("/embed2vector/add")
    public void add() {
        List<Document> documents = List.of(
                new Document("我喜欢唱跳rap篮球"),
                new Document("我喜欢打游戏")
        );

        vectorStore.add(documents);
    }


    /**
     * 从向量数据库RedisStack查找，进行相似度查找
     *
     * @param msg
     * @return
     */
    @GetMapping("/embed2vector/get")
    public List getAll(@RequestParam(name = "msg") String msg) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(msg)
                .topK(2)
                .build();

        List<Document> list = vectorStore.similaritySearch(searchRequest);

        log.info(list.toString());

        return list;
    }
}
