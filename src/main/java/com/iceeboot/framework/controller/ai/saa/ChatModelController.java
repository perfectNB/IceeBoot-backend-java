package com.iceeboot.framework.controller.ai.saa;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * chatmodel对话
 *
 * @auther CodeIcee
 * @since 2025-09-24
 */
@Tag(name = "chatModel对话", description = "chatmodel对话")
@RestController
@RequestMapping("/v1/saa/")
public class ChatModelController {
    @Resource
    @Qualifier("dashscopeChatModel")
    private ChatModel chatModel;

    /**
     * 普通对话
     *
     * @param msg
     * @return
     */
    @GetMapping(value = "/chatModel/dochat")
    public String doChat(@RequestParam(name = "msg", defaultValue = "我是你爹") String msg) {
        String result = chatModel.call(msg);
        return result;
    }

    /**
     * 流式对话
     *
     * @param msg
     * @return
     */
    @GetMapping(value = "/chatModel/streamchat")
    public Flux<String> stream(@RequestParam(name = "msg", defaultValue = "你是我儿") String msg) {
        return chatModel.stream(msg);
    }
}
