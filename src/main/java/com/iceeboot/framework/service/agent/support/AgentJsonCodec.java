package com.iceeboot.framework.service.agent.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.*;

/** 持久化 JSON 编解码，业务资源和发布快照使用相同序列化契约。 */
@Component
@RequiredArgsConstructor
public class AgentJsonCodec {
    private final ObjectMapper json;

    public String json(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.CONFIG_SERIALIZATION_FAILED,
                    e);
        }
    }

    public <T> T read(String value, Class<T> type) {
        if (value == null || "null".equals(value)) return null;
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(), ResponseMessage.Agent.CONFIG_CORRUPTED, e);
        }
    }

    public <T> List<T> list(String value, Class<T> type) {
        if (value == null || "null".equals(value)) return List.of();
        try {
            return json.readValue(
                    value, json.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.CONFIG_RESOURCE_LIST_CORRUPTED,
                    e);
        }
    }

    public <T> Map<String, T> map(String value, Class<T> type) {
        if (value == null || "null".equals(value)) return Map.of();
        try {
            return json.readValue(
                    value,
                    json.getTypeFactory()
                            .constructMapType(LinkedHashMap.class, String.class, type));
        } catch (JsonProcessingException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.CONFIG_RESOURCE_PARAMETERS_CORRUPTED,
                    e);
        }
    }
}
