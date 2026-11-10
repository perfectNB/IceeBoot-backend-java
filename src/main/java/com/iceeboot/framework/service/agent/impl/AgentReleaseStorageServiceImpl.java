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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 负责发布版本数据，事务边界保持在公开服务方法。 */
@Service
@RequiredArgsConstructor
public class AgentReleaseStorageServiceImpl implements IAgentReleaseStorageService {
    private final AgentDefinitionMapper agents;
    private final AgentReleaseMapper releases;
    private final AgentJsonCodec codec;

    private static <T> T required(T value, String message) {
        if (value == null) throw new IceeBootException(message);
        return value;
    }

    private static void changed(int affected, String message) {
        if (affected != 1) throw new IceeBootException(message);
    }

    @Override
    public int nextVersion(long id) {
        return releases.nextVersion(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(PublishedAgent p, String user) {
        AgentReleaseDO row = new AgentReleaseDO();
        row.setId(p.releaseId());
        row.setAgentId(p.agent().id());
        row.setVersion(p.version());
        row.setSnapshotJson(codec.json(p));
        row.setCreateBy(user);
        changed(releases.insert(row), ResponseMessage.Agent.RELEASE_CREATE_FAILED);
        changed(
                agents.update(
                        null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                                        AgentDefinitionDO>()
                                .eq(AgentDefinitionDO::getId, p.agent().id())
                                .set(AgentDefinitionDO::getActiveReleaseId, p.releaseId())
                                .set(AgentDefinitionDO::getUpdateBy, user)
                                .set(AgentDefinitionDO::getUpdateTime, LocalDateTime.now())),
                ResponseMessage.Agent.AGENT_NOT_FOUND);
    }

    @Override
    public PublishedAgent release(String id) {
        return codec.read(
                required(releases.selectById(id), ResponseMessage.Agent.AGENT_NOT_PUBLISHED)
                        .getSnapshotJson(),
                PublishedAgent.class);
    }

    @Override
    public List<ReleaseView> releases(long agent) {
        return releases
                .selectList(
                        new LambdaQueryWrapper<AgentReleaseDO>()
                                .eq(AgentReleaseDO::getAgentId, agent)
                                .orderByDesc(AgentReleaseDO::getVersion)
                                .last("LIMIT 100"))
                .stream()
                .map(
                        r ->
                                new ReleaseView(
                                        r.getId(),
                                        r.getVersion(),
                                        r.getCreateTime(),
                                        r.getCreateBy()))
                .toList();
    }
}
