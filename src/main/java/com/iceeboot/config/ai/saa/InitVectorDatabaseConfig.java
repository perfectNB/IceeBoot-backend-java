package com.iceeboot.config.ai.saa;

import cn.hutool.crypto.SecureUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @auther CodeIcee
 * @since 2025-09-25
 */
@Configuration
@Slf4j
public class InitVectorDatabaseConfig {
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("classpath:ops.txt")
    private Resource opsFile;

    @PostConstruct
    public void init() {
        //1 读取文件
        TextReader textReader = new TextReader(opsFile);
        textReader.setCharset(Charset.defaultCharset());

        //2 文件转换为向量(开启分词)
        List<Document> list = new TokenTextSplitter().transform(textReader.read());


        String sourceMetadata = (String) textReader.getCustomMetadata().get("source");

        String textHash = SecureUtil.md5(sourceMetadata);
        String redisKey = "vector-xxx:" + textHash;

        // 判断是否存入过,redisKey如果可以成功插入表示以前没有过，可以假如向量数据
        Boolean retFlag = redisTemplate.opsForValue().setIfAbsent(redisKey, "1");

        log.info("****retFlag : " + retFlag);

        if (Boolean.TRUE.equals(retFlag)) {

            vectorStore.add(list);
        } else {

            log.info("------向量初始化数据已经加载过，请不要重复操作");
        }
    }

}
