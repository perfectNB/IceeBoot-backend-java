package com.iceeboot.framework.service.agent.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 负责模型连接数据，事务边界保持在公开服务方法。 */
@Service
@RequiredArgsConstructor
public class AgentModelStorageServiceImpl implements IAgentModelStorageService {
    private final AgentModelMapper models;
    private final AgentJsonCodec codec;

    private static <T> T required(T value, String message) {
        if (value == null) throw new IceeBootException(message);
        return value;
    }

    private static void changed(int affected, String message) {
        if (affected != 1) throw new IceeBootException(message);
    }

    @Override
    public List<ModelConfig> models() {
        return models
                .selectList(
                        new LambdaQueryWrapper<AgentModelDO>()
                                .orderByDesc(AgentModelDO::getId)
                                .last("LIMIT 1000"))
                .stream()
                .map(this::model)
                .toList();
    }

    @Override
    public ModelConfig model(long id) {
        return model(required(models.selectById(id), ResponseMessage.Agent.MODEL_NOT_FOUND));
    }

    private ModelConfig model(AgentModelDO m) {
        return new ModelConfig(
                m.getId(),
                m.getName(),
                Provider.valueOf(m.getProvider()),
                Protocol.valueOf(m.getProtocol()),
                m.getBaseUrl(),
                m.getApiKey(),
                m.getModel(),
                m.getContextWindow(),
                Boolean.TRUE.equals(m.getEnabled()),
                codec.read(m.getGenerationJson(), Generation.class));
    }

    @Override
    public long saveModel(ModelConfig m) {
        AgentModelDO row = new AgentModelDO();
        row.setId(m.id());
        row.setName(m.name());
        row.setProvider(m.provider().name());
        row.setProtocol(m.protocol().name());
        row.setBaseUrl(m.baseUrl());
        row.setApiKey(m.apiKey());
        row.setModel(m.model());
        row.setContextWindow(m.contextWindow());
        row.setEnabled(m.enabled());
        row.setGenerationJson(codec.json(m.generation()));
        if (m.id() == null) changed(models.insert(row), ResponseMessage.Agent.MODEL_CREATE_FAILED);
        else changed(models.updateById(row), ResponseMessage.Agent.MODEL_NOT_FOUND);
        return row.getId();
    }

    @Override
    public void deleteModel(long id) {
        models.deleteById(id);
    }
}
