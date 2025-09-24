package com.iceeboot.framework.controller.ai;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @auther CodeIcee
 * @create 2025-09-24
 */
@RestController
@RequestMapping("/v1/saa/")
public class OllamaController
{


    @Resource
    @Qualifier("ollamaChatModel")
    private ChatModel chatModel;


    @GetMapping("/ollama/chat")
    public String chat(@RequestParam(name = "msg") String msg)
    {
        String result = chatModel.call(msg);
        System.out.println("生成结果：" + result);
        return result;
    }

    @GetMapping("/ollama/streamchat")
    public Flux<String> streamchat(@RequestParam(name = "msg",defaultValue = "操你妈") String msg)
    {
        return chatModel.stream(msg);
    }
}








