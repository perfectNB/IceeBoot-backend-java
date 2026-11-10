package com.iceeboot.config.message;

import com.iceeboot.common.constant.ApplicationConstants;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** 通知受众的应用分类；应用编码不承担租户或工作空间权限隔离。 */
@Getter
@Component
@ConfigurationProperties(prefix = "iceeboot.notifications")
public class NotificationProperties {

    private List<String> applicationCodes = List.of(ApplicationConstants.PLATFORM);

    public void setApplicationCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("通知应用编码不能为空");
        }
        applicationCodes = codes.stream().map(code -> {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("通知应用编码不能为空");
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            if (ApplicationConstants.ALL.equals(normalized)) {
                throw new IllegalArgumentException("ALL 是通知广播编码，不能作为应用编码");
            }
            return normalized;
        }).distinct().toList();
    }
}
