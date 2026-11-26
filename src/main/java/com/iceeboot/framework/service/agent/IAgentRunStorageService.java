package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.enums.agent.UsagePeriod;
import com.iceeboot.framework.vo.agent.UsageOverviewVO;

import java.util.List;

/** 执行账本持久化边界。 */
public interface IAgentRunStorageService {
    void startRun(
            String id, String root, String parent, PublishedAgent p, String user, String input);

    void finishRun(String id, String status, String output, String error);

    void modelCall(
            String id,
            String run,
            String root,
            String agent,
            String model,
            String input,
            String output,
            long in,
            long out,
            Long cached,
            BillingSource billing,
            boolean known);

    UsageOverviewVO usageOverview(UsagePeriod period);

    List<RunView> runs();

    RunDetail runDetail(String root);

    boolean requestCancellation(String id);

    boolean cancellationRequested(String id);

    void heartbeat(String id);

    void diagnose(String id, String code, String trace);

    int interruptStale(long seconds);
}
