package com.zboot.framework.vo.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * OSS厂商视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "OSS厂商视图对象")
public class OssProviderVO {

    @Schema(description = "厂商名称")
    private String providerName;

    @Schema(description = "厂商显示名称")
    private String displayName;

    @Schema(description = "密钥列表")
    private List<OssKeyVO> keys;

    @Schema(description = "密钥数量")
    private Integer keyCount;

    @Schema(description = "激活密钥数量")
    private Integer activeKeyCount;

    @Schema(description = "默认端点")
    private String defaultEndpoint;

    @Schema(description = "支持的功能")
    private List<String> supportedFeatures;
}