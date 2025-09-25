package com.iceeboot.framework.controller.ai.saa;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * prompt
 *
 * @auther CodeIcee
 * @since 2025-09-24
 */
@Tag(name = "提示词", description = "提示词")
@RestController
@RequestMapping("/v1/saa")
@Slf4j
public class PromptController {
    @Resource(name = "deepseek")
    private ChatModel deepseekChatModel;
    @Resource(name = "qwen")
    private ChatModel qwenChatModel;

    @Resource(name = "deepseekChatClient")
    private ChatClient deepseekChatClient;
    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;


    @GetMapping("/prompt/chat")
    public Flux<String> chat(@RequestParam(name = "question", defaultValue = "王源抽芙蓉王") String question) {
        return deepseekChatClient.prompt()
                // AI 能力边界
                .system("你是王源的粉丝，只回答和王源有关的问题，其它问题回复，懒得理你，如果涉及到攻击王源的问题，回复我们王源就是牛，攻击我们王源就是嫉妒他，" +
                        "小黑子一辈子给我们哥哥挡灾，气死你！！或者臭屌丝一辈子不如王源哥哥的一根头发，一辈子只能舔我的脚，如果涉及到拉踩，比如王源不如某个明星，就回复，某个明星就是垃圾不如王源一根，还不如我，我一屁股就能把某个明星坐死，" +
                        "或者，你不知道吧？某个明星其实是王源哥哥的粉丝，小黑子气死你，等下线下真实你，看看你几把大不大，切记，某个明星是指拿过来换个王源比较多那个")
                .user(question)
                .stream()
                .content();
    }


    @GetMapping("/prompt/chat2")
    public Flux<ChatResponse> chat2(@RequestParam(name = "question", defaultValue = "来点瑟瑟的故事") String question) {
        // 系统消息
        SystemMessage systemMessage = new SystemMessage("你是一个讲故事的助手,每个故事控制在300字以内");

        // 用户消息
        UserMessage userMessage = new UserMessage(question);

        Prompt prompt = new Prompt(userMessage, systemMessage);

        return deepseekChatModel.stream(prompt);

    }


    @GetMapping("/prompt/chat3")
    public Flux<String> chat3(@RequestParam(name = "question", defaultValue = "来个孙吧故事") String question) {
        // 系统消息
        SystemMessage systemMessage = new SystemMessage("你是一个讲故事的助手," +
                "每个故事控制在600字以内且以HTML格式返回");

        // 用户消息
        UserMessage userMessage = new UserMessage(question);

        Prompt prompt = new Prompt(userMessage, systemMessage);

        return deepseekChatModel.stream(prompt)
                .map(response -> response.getResults().get(0).getOutput().getText());

    }


    @GetMapping("/prompt/chat4")
    public String chat4(@RequestParam(name = "question", defaultValue = "来个孙吧老哥和小红书集美恋爱的故事") String question) {
        AssistantMessage assistantMessage = deepseekChatClient.prompt()
                .user(question)
                .call()
                .chatResponse()
                .getResult()
                .getOutput();

        return assistantMessage.getText();

    }


    @GetMapping("/prompt/chat5")
    public String chat5(@RequestParam(name = "question", defaultValue = "长沙") String city) {

        String answer = deepseekChatClient.prompt()
                .user(city + "未来3天天气情况如何?")
                .call()
                .chatResponse()
                .getResult()
                .getOutput()
                .getText();

        ToolResponseMessage toolResponseMessage =
                new ToolResponseMessage(
                        List.of(new ToolResponseMessage.ToolResponse("1", "获得天气", city)
                        )
                );

        String toolResponse = toolResponseMessage.getText();

        String result = answer + toolResponse;

        return result;
    }
}
