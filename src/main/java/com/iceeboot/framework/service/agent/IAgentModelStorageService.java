package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.model.agent.*;

import java.util.List;

/** 模型连接持久化边界。 */
public interface IAgentModelStorageService {
    List<ModelConfig> models();

    ModelConfig model(long id);

    long saveModel(ModelConfig m);

    void deleteModel(long id);
}
