package com.iceeboot.framework.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(description = "凭据轮换结果，不包含任何密钥或密文")
public class AgentSecretRotationVO {
    private String activeKeyId;
    private int updatedRows;
}
