package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.vo.agent.AgentQuotaVO;

/** 所有根调用、子调用和直接模型调用共享同一用户的数据库原子额度。 */
public interface IAgentQuotaService {
    void acquire(String userId);

    AgentQuotaVO getQuota(String userId);
}
