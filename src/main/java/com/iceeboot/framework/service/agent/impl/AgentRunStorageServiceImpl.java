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

/** 负责执行账本数据，事务边界保持在公开服务方法。 */
@Service
@RequiredArgsConstructor
public class AgentRunStorageServiceImpl implements IAgentRunStorageService {
    private final AgentRunMapper runs;
    private final AgentModelCallMapper calls;
    private final AgentJsonCodec codec;

    private static <T> T required(T value, String message) {
        if (value == null) throw new IceeBootException(message);
        return value;
    }

    private static void changed(int affected, String message) {
        if (affected != 1) throw new IceeBootException(message);
    }

    @Override
    public void startRun(
            String id, String root, String parent, PublishedAgent p, String user, String input) {
        AgentRunDO row = new AgentRunDO();
        row.setId(id);
        row.setRootRunId(root);
        row.setParentRunId(parent);
        row.setAgentId(p.agent().id());
        row.setAgentCode(p.agent().code());
        row.setVersion(p.version());
        row.setUserId(user);
        row.setStatus("RUNNING");
        row.setInputText(input);
        row.setOutputText("");
        row.setErrorText("");
        row.setInputTokens(0L);
        row.setOutputTokens(0L);
        row.setUsageKnown(true);
        row.setStartedAt(LocalDateTime.now());
        changed(runs.insert(row), ResponseMessage.Agent.RUN_CREATE_FAILED);
    }

    @Override
    public void finishRun(String id, String status, String output, String error) {
        changed(
                runs.update(
                        null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                                        AgentRunDO>()
                                .eq(AgentRunDO::getId, id)
                                .set(AgentRunDO::getStatus, status)
                                .set(AgentRunDO::getOutputText, output)
                                .set(AgentRunDO::getErrorText, error)
                                .set(AgentRunDO::getFinishedAt, LocalDateTime.now())
                                .set(AgentRunDO::getUpdateTime, LocalDateTime.now())),
                ResponseMessage.Agent.RUN_NOT_FOUND);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modelCall(
            String id,
            String run,
            String root,
            String agent,
            String model,
            String input,
            String output,
            long in,
            long out,
            boolean known) {
        AgentModelCallDO row = new AgentModelCallDO();
        row.setId(id);
        row.setRunId(run);
        row.setRootRunId(root);
        row.setAgentCode(agent);
        row.setModel(model);
        row.setInputText(input);
        row.setOutputText(output);
        row.setInputTokens(in);
        row.setOutputTokens(out);
        row.setUsageKnown(known);
        changed(calls.insert(row), ResponseMessage.Agent.MODEL_CALL_RECORD_FAILED);
        changed(runs.addUsage(run, in, out, known), ResponseMessage.Agent.RUN_NOT_FOUND);
    }

    @Override
    public List<RunView> runs() {
        return runs
                .selectList(
                        new LambdaQueryWrapper<AgentRunDO>()
                                .apply("id = root_run_id")
                                .orderByDesc(AgentRunDO::getStartedAt)
                                .last("LIMIT 100"))
                .stream()
                .map(this::run)
                .toList();
    }

    @Override
    public RunDetail runDetail(String root) {
        List<RunView> records =
                runs
                        .selectList(
                                new LambdaQueryWrapper<AgentRunDO>()
                                        .eq(AgentRunDO::getRootRunId, root)
                                        .orderByAsc(AgentRunDO::getStartedAt, AgentRunDO::getId))
                        .stream()
                        .map(this::run)
                        .toList();
        if (records.isEmpty()) throw new IceeBootException(ResponseMessage.Agent.RUN_NOT_FOUND);
        List<ModelCallView> modelCalls =
                calls
                        .selectList(
                                new LambdaQueryWrapper<AgentModelCallDO>()
                                        .eq(AgentModelCallDO::getRootRunId, root)
                                        .orderByAsc(
                                                AgentModelCallDO::getCreateTime,
                                                AgentModelCallDO::getId))
                        .stream()
                        .map(
                                r ->
                                        new ModelCallView(
                                                r.getId(),
                                                r.getRunId(),
                                                r.getRootRunId(),
                                                r.getAgentCode(),
                                                r.getModel(),
                                                r.getInputText(),
                                                r.getOutputText(),
                                                r.getInputTokens(),
                                                r.getOutputTokens(),
                                                Boolean.TRUE.equals(r.getUsageKnown()),
                                                r.getCreateTime()))
                        .toList();
        return new RunDetail(
                records,
                modelCalls,
                modelCalls.stream().mapToLong(ModelCallView::inputTokens).sum(),
                modelCalls.stream().mapToLong(ModelCallView::outputTokens).sum(),
                modelCalls.stream().allMatch(ModelCallView::usageKnown));
    }

    private RunView run(AgentRunDO r) {
        return new RunView(
                r.getId(),
                r.getRootRunId(),
                r.getParentRunId(),
                r.getAgentId(),
                r.getAgentCode(),
                r.getVersion(),
                r.getUserId(),
                r.getStatus(),
                r.getInputText(),
                r.getOutputText(),
                r.getErrorText(),
                r.getInputTokens(),
                r.getOutputTokens(),
                Boolean.TRUE.equals(r.getUsageKnown()),
                r.getStartedAt(),
                r.getFinishedAt(),
                r.getErrorCode(),
                r.getTraceId(),
                Boolean.TRUE.equals(r.getCancelRequested()));
    }

    @Override
    public boolean requestCancellation(String id) {
        return runs.requestCancellation(id) == 1;
    }

    @Override
    public boolean cancellationRequested(String id) {
        return Boolean.TRUE.equals(runs.cancellationRequested(id));
    }

    @Override
    public void heartbeat(String id) {
        runs.heartbeat(id);
    }

    @Override
    public void diagnose(String id, String code, String trace) {
        runs.diagnose(id, code, trace);
    }

    @Override
    public int interruptStale(long seconds) {
        return runs.interruptStale(seconds, ResponseMessage.Agent.RUN_INTERRUPTED);
    }
}
