package com.iceeboot.framework.job;

import com.iceeboot.framework.service.agent.memory.MemoryExtractionWorker;
import lombok.RequiredArgsConstructor;
import org.quartz.*;

@DisallowConcurrentExecution
@RequiredArgsConstructor
public class MemoryExtractionJob implements Job {
    private final MemoryExtractionWorker worker;

    @Override
    public void execute(JobExecutionContext context) {
        worker.poll();
    }
}
