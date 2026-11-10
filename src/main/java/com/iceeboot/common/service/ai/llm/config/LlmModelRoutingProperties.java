package com.iceeboot.common.service.ai.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 模型名称兼容配置，集中承接历史别名，不向业务流程泄漏。
 */
@Data
@Component
@ConfigurationProperties(prefix = "iceeboot.framework.llm")
public class LlmModelRoutingProperties {

    private Map<String, Map<String, String>> modelAliases = new LinkedHashMap<>();

    public String normalize(String provider, String model) {
        if (!StringUtils.hasText(model)) {
            return model;
        }
        String normalizedModel = model.trim();
        if (!StringUtils.hasText(provider)) {
            return normalizedModel;
        }
        Map<String, String> aliases = modelAliases.get(provider.trim().toLowerCase(Locale.ROOT));
        if (aliases == null || aliases.isEmpty()) {
            return normalizedModel;
        }
        return aliases.getOrDefault(normalizedModel.toLowerCase(Locale.ROOT), normalizedModel);
    }
}
