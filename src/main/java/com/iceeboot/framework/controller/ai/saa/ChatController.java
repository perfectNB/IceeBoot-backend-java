package com.iceeboot.framework.controller.ai.saa;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * chatModel和chatClient对话
 *
 * @auther CodeIcee
 * @create 2025-09-24
 */
@Tag(name = "chatModel和chatClient对话", description = "chatModel和chatClient对话")
@RestController
@RequestMapping("/v1/saa/chat")
@Slf4j
public class ChatController {

    @Resource(name = "deepseek")
    private ChatModel deepseekChatModel;

    @Resource(name = "qwen")
    private ChatModel qwenChatModel;

    @Resource(name = "deepseekChatClient")
    private ChatClient deepseekChatClient;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;


    //========================chatModel=============================
    @GetMapping(value = "/deepSeekChatModelStream")
    public Flux<String> deepSeekChatModelStream(@RequestParam(name = "question", defaultValue = "你是？") String question) {
        log.info("对话内容:{}", question);
        return deepseekChatModel.stream(question);
    }

    @GetMapping(value = "/qwenChatModelStream")
    public Flux<String> qwenChatModelStream(@RequestParam(name = "question", defaultValue = "你是？") String question) {
        log.info("对话内容:{}", question);
        return qwenChatModel.stream(question);
    }

    @GetMapping(value = "/deepSeekChatModel")
    public String deepSeekChatModel(@RequestParam(name = "msg", defaultValue = "我是你爹") String msg) {
        String result = deepseekChatModel.call(msg);
        log.info("对话内容，{},生成结果{}",msg,result);
        return result;
    }

    @GetMapping(value = "/qwenChatModel")
    public String qwenChatModel(@RequestParam(name = "msg", defaultValue = "我是你爹") String msg) {
        String result = qwenChatModel.call(msg);
        log.info("对话内容，{},生成结果{}",msg,result);
        return result;
    }


    //========================chatClient=============================

    @GetMapping(value = "/deepSeekChatClientStream")
    public Flux<String> deepSeekChatClientStream(@RequestParam(name = "question", defaultValue = "你是？") String question) {
        log.info("question:{}", question);
        return deepseekChatClient.prompt(question).stream().content();
    }

    @GetMapping(value = "/qwenChatClientStream")
    public Flux<String> qwenChatClientStream(@RequestParam(name = "question", defaultValue = "你是？") String question) {
        log.info("question:{}", question);
        return qwenChatClient.prompt(question).stream().content();
    }

    @GetMapping(value = "/deepSeekChatClient")
    public String deepSeekChatClient(@RequestParam(name = "msg", defaultValue = "我是你爹") String msg) {
        String result = deepseekChatModel.call(msg);
        log.info("对话内容，{},生成结果{}",msg,result);
        return result;
    }

    @GetMapping(value = "/qwenChatChatClient")
    public String qwenChatChatClient(@RequestParam(name = "msg", defaultValue = "我是你爹") String msg) {
        String result = qwenChatModel.call(msg);
        log.info("对话内容，{},生成结果{}",msg,result);
        return result;
    }

}
