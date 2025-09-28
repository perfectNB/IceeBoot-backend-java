package com.iceeboot.framework.controller.ai.saa;

import com.iceeboot.common.utils.tool.JapanMovieActorTool;
import com.iceeboot.common.utils.tool.JapanMoviePathTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @auther CodeIcee
 * @create 2025-00-26
 */

@RestController
@RequestMapping("/v1/saa")
@Slf4j
public class ToolCallingController {
    @Resource(name = "qwen")
    private ChatModel qwenChatModel;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @GetMapping("/tool/chat")
    public String chat(@RequestParam(name = "msg", defaultValue = "来点欧美电影") String msg) {
        // 1.工具注册到工具集合里
        ToolCallback[] tools = ToolCallbacks.from(new JapanMoviePathTool());

        // 2.将工具集配置进ChatOptions对象
        ChatOptions options = ToolCallingChatOptions.builder().toolCallbacks(tools).build();

        // 3.构建提示词
        Prompt prompt = new Prompt(msg, options);

        // 4.调用大模型
        return qwenChatModel.call(prompt).getResult().getOutput().getText();
    }


    @GetMapping("/tool/streamChat")
    public Flux<String> chat2(@RequestParam(name = "msg", defaultValue = "高市早苗是谁") String msg) {
        return qwenChatClient.prompt()
                .user(msg)
                .tools(new JapanMoviePathTool(),
                        new JapanMovieActorTool()
                )
                .stream()
                .content();
    }
}
