//package com.iceeboot.framework.controller.ai.saa;
//
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * chatClient对话
// *
// * @auther CodeIcee
// * @create 2025-09-24
// */
//@Tag(name = "chatClient对话", description = "chatClient对话")
//@RestController
//@RequestMapping("/v1/saa/")
//@Slf4j
//public class ChatClientController
//{
//
//    @Resource
//    private ChatClient dashScopechatClientv2;
//
//
//    @GetMapping("/chatClient/dochat")
//    public String doChat(@RequestParam(name = "msg",defaultValue = "我是你爸爸") String msg)
//    {
//        String result = dashScopechatClientv2.prompt().user(msg).call().content();
//        log.info("对话内容，{},生成结果{}",msg,result);
//        return result;
//    }
//
//
//
//}
