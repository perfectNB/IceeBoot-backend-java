package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.mapper.agent.AgentUsageQuotaMapper;
import com.iceeboot.framework.service.agent.IAgentQuotaService;
import com.iceeboot.framework.vo.agent.AgentQuotaVO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AgentQuotaServiceImpl implements IAgentQuotaService {
    private final AgentUsageQuotaMapper mapper;
    private final AgentProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acquire(String userId) {
        if (userId == null || userId.isBlank())
            throw new IceeBootException(ResponseMessage.Agent.LOGIN_REQUIRED);
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        mapper.ensureDay(userId, day);
        if (mapper.acquire(userId, day, properties.getDailyModelRequestLimit()) != 1)
            throw new IceeBootException(429, ResponseMessage.Agent.QUOTA_EXCEEDED);
    }

    @Override
    public AgentQuotaVO getQuota(String userId) {
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        Long used = mapper.used(userId, day);
        var result = new AgentQuotaVO();
        result.setUsageDay(day);
        result.setUsedRequests(used == null ? 0 : used);
        result.setDailyLimit(properties.getDailyModelRequestLimit());
        result.setRemainingRequests(
                result.getDailyLimit() == 0
                        ? null
                        : Math.max(0, result.getDailyLimit() - result.getUsedRequests()));
        return result;
    }
}
