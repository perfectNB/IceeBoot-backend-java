package com.iceeboot.common.service.ai.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 *
 * @auther CodeIcee
 * @create 2025-09-26
 */

@Service
public class DanceService
{
    @Tool(description = "根据街舞类型返回街舞解说",returnDirect = false)
    public String get(String dance)
    {
        Map<String, String> map = Map.of(
                "breaking", "转圈圈的",
                "Breaking", "转圈圈的",
                "locking", "甩花手",
                "锁舞", "甩花手",
                "机械舞", "触电了",
                "机器舞", "触电了",
                "震撼舞", "触电了",
                "popping", "触电了",
                "hiphop", "该溜子",
                "krump", "发癫"
        );
        String orDefault = map.getOrDefault(dance, "抱歉：未查询到对应舞种！");
        return orDefault;
    }
}