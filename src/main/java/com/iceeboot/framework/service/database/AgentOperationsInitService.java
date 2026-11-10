package com.iceeboot.framework.service.database;

import com.iceeboot.framework.service.agent.runtime.AgentRunControl;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/** RBAC 表初始化完成后补充运维接口，再恢复失去心跳的旧任务。 */
@Service
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class AgentOperationsInitService implements ApplicationRunner {
    private final AgentSchemaMigrationService migrations;
    private final AgentRunControl control;

    @Override
    public void run(ApplicationArguments args) {
        migrations.migratePermissions();
        control.recoverStale();
    }
}
