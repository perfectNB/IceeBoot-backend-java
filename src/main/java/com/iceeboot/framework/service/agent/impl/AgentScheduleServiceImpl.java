package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.agent.AgentScheduleDTO;
import com.iceeboot.framework.dto.update.agent.AgentScheduleUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentScheduleDO;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.service.agent.IAgentScheduleService;
import com.iceeboot.framework.service.agent.schedule.*;
import com.iceeboot.framework.vo.agent.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentScheduleServiceImpl implements IAgentScheduleService {
    private final AgentScheduleMapper schedules;
    private final AgentScheduleExecutionMapper executions;
    private final AgentSchedulePolicy policy;
    private final AgentScheduleAccess access;

    private AgentScheduleDO owned(long owner, long id, boolean lock) {
        AgentScheduleDO row = lock ? schedules.lock(id) : schedules.owned(id, owner);
        if (row == null || row.isDeleted() || row.getOwnerId() != owner)
            throw new IceeBootException(404, ResponseMessage.Agent.SCHEDULE_NOT_FOUND);
        return row;
    }

    private AgentScheduleVO view(AgentScheduleDO row) {
        return new AgentScheduleVO(row.getId(), row.getAgentCode(), row.getName(), row.getInput(),
                row.getScheduleType(), row.getCronExpression(), row.getZoneId(), row.getExecuteAt(),
                row.getNextFireAt(), row.isEnabled(), row.getRunningId() != null);
    }

    private void configure(AgentScheduleDO row, AgentScheduleDTO request) {
        access.requireExecution(row.getOwnerId());
        access.requireAgent(request.getAgentCode());
        BeanUtils.copyProperties(request, row, "id", "ownerId");
        row.setName(request.getName().trim());
        row.setNextFireAt(policy.next(row, System.currentTimeMillis(), true));
        if (!row.isEnabled()) row.setNextFireAt(null);
    }

    @Override
    public List<AgentScheduleVO> list(long owner) {
        return schedules.list(owner).stream().map(this::view).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentScheduleVO create(long owner, AgentScheduleDTO request) {
        AgentScheduleDO row = new AgentScheduleDO();
        row.setOwnerId(owner);
        configure(row, request);
        schedules.insert(row);
        return view(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentScheduleVO update(long owner, AgentScheduleUpdateDTO request) {
        AgentScheduleDO row = owned(owner, request.getId(), true);
        if (row.getRunningId() != null)
            throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_BUSY);
        configure(row, request);
        schedules.update(row);
        return view(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentScheduleVO enabled(long owner, long id, boolean enabled) {
        AgentScheduleDO row = owned(owner, id, true);
        if (enabled == row.isEnabled()) return view(row);
        if (enabled) {
            access.requireExecution(owner);
            access.requireAgent(row.getAgentCode());
            row.setNextFireAt(policy.next(row, System.currentTimeMillis(), true));
        } else row.setNextFireAt(null);
        row.setEnabled(enabled);
        schedules.update(row);
        return view(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long owner, long id) {
        AgentScheduleDO row = owned(owner, id, true);
        if (row.getRunningId() != null)
            throw new IceeBootException(ResponseMessage.Agent.SCHEDULE_BUSY);
        row.setDeleted(true);
        row.setEnabled(false);
        row.setNextFireAt(null);
        schedules.update(row);
    }

    @Override
    public List<AgentScheduleExecutionVO> executions(long owner, long id, int offset) {
        owned(owner, id, false);
        if (offset < 0 || offset > 100000)
            throw new IceeBootException(ResponseMessage.Agent.REQUEST_INVALID);
        return executions.list(id, owner, offset).stream().map(row -> new AgentScheduleExecutionVO(
                row.getId(), row.getScheduledAt(), row.getStartedAt(), row.getFinishedAt(),
                row.getStatus(), row.getMessage(), row.getRunId(), row.getSessionId())).toList();
    }
}
