package com.iceeboot.common.utils.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author CodeIcee
 * @date 2025-09-26
 */
@Component
public class JapanMoviePathTool {

    /**
     * true 返回结果不走大模型
     * false 走大模型
     */
    @Tool(description = "获取不同类型电影的地址", returnDirect = false)
    public String movePath(String name) {

        Map<String, String> map = Map.of(
                "搜查官", "地址为：https://baike.baidu.com/item/%E6%B2%B3%E5%8C%97%E5%BD%A9%E4%BC%BD/64480389",
                "逃犯", "地址为：https://www.bilibili.com/opus/933433845263368438",
                "动漫", "地址为：https://baike.baidu.com/item/%E6%A3%AE%E6%97%A5%E5%90%91%E5%AD%90/66751386",
                "欧美", "地址为：https://www.bilibili.com/opus/868672318345314339",
                "日韩", "地址为：https://www.bilibili.com/opus/1052492370265767941",
                "国产", "地址为：https://baike.baidu.com/link?url=iaVmu3ICZ53I0U1G_RfFvCRzZl1d6s50N9So0Cgj6_snDcIUg3PjjMn7JvTfkjaPzDiNWoViumzRpODA_pr6b4stfkmlA-FBV-yJ3gfjN_a",
                "四爱", "地址为：https://baike.baidu.com/link?url=Rpmx4t16NoDTiAPj_s1O8UdhB_Kgm8FNmP3idaEoDe8y_MeUcL-_VDP2-YQppuYAf76w2uW8U1fh2lfN44NaPWWCsvDNOANEkQ_fP_-YiZO"
        );
        String introduce = map.getOrDefault(name, "没有查到这个类型，您可以找人拍一下");
        return introduce;

    }
}
