package com.iceeboot.framework.controller.ai.saa;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @auther CodeIcee
 * @create 2025-09-24
 */
@Tag(name = "chatClient对话", description = "chatClient对话")
@RestController
@RequestMapping("/v1/saa/")
public class ChatClientController
{

    @Resource
    private ChatClient dashScopechatClientv2;


    @GetMapping("/chatClient/dochat")
    public String doChat(@RequestParam(name = "msg",defaultValue = "你是谁") String msg)
    {
        String result = dashScopechatClientv2.prompt().user(msg).call().content();
        System.out.println("ChatClient响应：" + result);
        return result;
    }



}
