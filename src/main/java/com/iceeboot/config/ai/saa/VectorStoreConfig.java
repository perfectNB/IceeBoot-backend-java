package com.iceeboot.config.ai.saa;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * 向量数据库配置类
 * 配置Redis作为向量存储
 *
 * @author CodeIcee
 * @date 2025-01-25
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 配置JedisPooled
     *
     * @return JedisPooled实例
     */
    @Bean
    public JedisPooled jedisPooled() {
        if (redisPassword != null && !redisPassword.isEmpty()) {
            return new JedisPooled(redisHost, redisPort, null, redisPassword);
        } else {
            return new JedisPooled(redisHost, redisPort);
        }
    }

    /**
     * 配置Redis向量存储
     *
     * @param embeddingModel 嵌入模型
     * @param jedisPooled Jedis连接池
     * @return VectorStore实例
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   JedisPooled jedisPooled) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("custom-index")
                .prefix("custom-prefix")
                .initializeSchema(true)
                .build();
    }
}
