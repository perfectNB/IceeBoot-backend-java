package com.iceeboot.framework.service.agent.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.mapper.agent.AgentSecretMapper;
import com.iceeboot.framework.service.agent.IAgentSecretRotationService;
import com.iceeboot.framework.service.agent.security.AgentSecrets;
import com.iceeboot.framework.vo.agent.AgentSecretRotationVO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentSecretRotationServiceImpl implements IAgentSecretRotationService {
    private final AgentSecretMapper mapper;
    private final AgentSecrets secrets;
    private final AgentProperties properties;
    private final ObjectMapper json;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSecretRotationVO rotate() {
        secrets.validateActiveKey();
        int updated = 0;
        for (var row : mapper.lockModelSecrets()) {
            mapper.updateModelSecret(row.getId(), secrets.reencrypt(row.getApiKey()));
            updated++;
        }
        for (var row : mapper.lockToolSecrets()) {
            mapper.updateToolSecret(row.getId(), values(row.getHeadersJson()));
            updated++;
        }
        for (var row : mapper.lockMcpSecrets()) {
            mapper.updateMcpSecret(row.getId(), values(row.getHeadersJson()));
            updated++;
        }
        for (var row : mapper.lockKnowledgeSecrets()) {
            mapper.updateKnowledgeSecret(row.getId(), values(row.getHeadersJson()));
            updated++;
        }
        for (var row : mapper.lockDefinitionSecrets()) {
            mapper.updateDefinitionSecret(row.getId(), values(row.getEnvironmentJson()));
            updated++;
        }
        for (var row : mapper.lockReleaseSecrets()) {
            mapper.updateReleaseSecret(row.getId(), snapshot(row.getSnapshotJson()));
            updated++;
        }
        var result = new AgentSecretRotationVO();
        result.setActiveKeyId(properties.getEncryptionKeyId());
        result.setUpdatedRows(updated);
        return result;
    }

    private String values(String value) {
        if (value == null || value.equals("null")) return value;
        try {
            JsonNode root = json.readTree(value);
            rotateMap(root);
            return json.writeValueAsString(root);
        } catch (IceeBootException e) {
            throw e;
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.Agent.SECRET_ROTATION_FAILED, e);
        }
    }

    private void rotateMap(JsonNode node) {
        if (node == null || node.isNull()) return;
        if (!(node instanceof ObjectNode object))
            throw new IceeBootException(ResponseMessage.Agent.CONFIG_CORRUPTED);
        object.properties()
                .forEach(
                        entry -> {
                            if (!entry.getValue().isTextual())
                                throw new IceeBootException(ResponseMessage.Agent.CONFIG_CORRUPTED);
                            object.put(
                                    entry.getKey(), secrets.reencrypt(entry.getValue().asText()));
                        });
    }

    private String snapshot(String value) {
        try {
            JsonNode root = json.readTree(value);
            rotateSnapshot(root);
            return json.writeValueAsString(root);
        } catch (IceeBootException e) {
            throw e;
        } catch (Exception e) {
            throw new IceeBootException(ResponseMessage.Agent.SECRET_ROTATION_FAILED, e);
        }
    }

    private void rotateSnapshot(JsonNode root) {
        if (!(root instanceof ObjectNode))
            throw new IceeBootException(ResponseMessage.Agent.CONFIG_CORRUPTED);
        JsonNode model = root.path("model");
        if (model instanceof ObjectNode object && model.hasNonNull("apiKey"))
            object.put("apiKey", secrets.reencrypt(model.path("apiKey").asText()));
        rotateMap(root.path("agent").get("environment"));
        for (JsonNode capability : root.path("capabilities")) {
            rotateMap(capability.path("tool").get("headers"));
            rotateMap(capability.path("mcp").get("headers"));
        }
        for (JsonNode child : root.path("children")) rotateSnapshot(child);
    }
}
