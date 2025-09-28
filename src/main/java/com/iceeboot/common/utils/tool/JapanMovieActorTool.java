package com.iceeboot.common.utils.tool;

import org.springframework.ai.tool.annotation.Tool;

import java.util.Map;

/**
 * @author CodeIcee
 * @date 2025/9/26
 */
public class JapanMovieActorTool {


    /**
     * true 返回结果不走大模型
     * false 走大模型
     */
    @Tool(description = "获取人物的介绍", returnDirect = false)
    public String moveActor(String name) {
        Map<String, String> map = Map.of(
                "河北彩花", "河北人，喜欢采花",
                "木下日葵", "身材高挑，技术好",
                "日向森子", "还可以，偶尔看",
                "涉谷华", "黑皮辣妹",
                "佐藤爱琉", "一米八几，高美",
                "清水健", "非常强大的男人",
                "加藤鹰", "神之手"
        );
        String introduce = map.getOrDefault(name,"没有查到此人，他也去干这个了？？？");
        return introduce;

    }
}
