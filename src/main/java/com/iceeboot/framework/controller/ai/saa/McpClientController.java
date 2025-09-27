package com.iceeboot.framework.controller.ai.saa;

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
 * @auther CodeIcee
 * @since 2025-09-26
 */

@RestController
@RequestMapping("/v1/saa")
@Slf4j
public class McpClientController {
    @Resource(name = "qwen")
    private ChatModel qwenChatModel;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;


    @GetMapping("/mcpclient/chat")
    public Flux<String> chat(@RequestParam(name = "msg", defaultValue = "北京") String msg) {
        log.info("使用了mcp");
        return qwenChatClient.prompt(msg).stream().content();
    }

    @RequestMapping("/mcpclient/chat2")
    public Flux<String> chat2(@RequestParam(name = "msg", defaultValue = "北京") String msg) {
        log.info("未使用mcp");
        return qwenChatModel.stream(msg);
    }
}
