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
public class WeatherService
{
    @Tool(description = "根据城市名称获取天气预报")
    public String getWeatherByCity(String city)
    {
        Map<String, String> map = Map.of(
                "长沙", "下金子，记得出来捡",
                "上海", "下钻石，别出来",
                "深圳", "66666高温末日60度"
        );
        return map.getOrDefault(city, "抱歉：未查询到对应城市！");
    }
}