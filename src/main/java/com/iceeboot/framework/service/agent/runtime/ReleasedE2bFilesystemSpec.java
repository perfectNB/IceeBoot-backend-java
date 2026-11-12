package com.iceeboot.framework.service.agent.runtime;

import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.sandbox.*;

/** 兼容 E2B 工作区首次初始化，并避免持久化每轮释放后已失效的连接凭据。 */
public final class ReleasedE2bFilesystemSpec extends E2bFilesystemSpec {
    @Override
    protected SandboxClient<?> createClient() {
        var options = (E2bSandboxClientOptions) clientOptions();
        return new E2bSandboxClient(options, null) {
            @Override
            public Sandbox create(WorkspaceSpec workspace,
                    io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec snapshot,
                    E2bSandboxClientOptions requested) {
                var state = (E2bSandboxState) super.create(workspace, snapshot, requested).getState();
                return bootstrapSandbox(state, mergeOptions(options, requested));
            }

            @Override
            public Sandbox resume(SandboxState value) {
                var state = (E2bSandboxState) super.resume(value).getState();
                var effective = mergeOptions(options, null);
                effective.setCodec(state.getCodec());
                return bootstrapSandbox(state, effective);
            }

            @Override
            public String serializeState(SandboxState state) {
                var copy = (E2bSandboxState) super.deserializeState(super.serializeState(state));
                if (copy.isSandboxOwned()) {
                    copy.setSandboxId(null);
                    copy.setEnvdAccessToken(null);
                    copy.setSandboxDomain(null);
                    copy.setWorkspaceRootReady(false);
                    copy.setWorkspaceProjectionHash(null);
                }
                return super.serializeState(copy);
            }
        };
    }

    private static Sandbox bootstrapSandbox(E2bSandboxState state, E2bSandboxClientOptions options) {
        return new E2bSandbox(state, options) {
            private boolean creatingWorkspace;

            @Override
            protected void doSetupWorkspace() throws Exception {
                creatingWorkspace = true;
                try { super.doSetupWorkspace(); }
                finally { creatingWorkspace = false; }
            }

            @Override
            protected void doHydrateWorkspace(java.io.InputStream archive) throws Exception {
                byte[] bytes = archive.readAllBytes();
                // 原生快照引用继续交给 SDK 恢复；只有文件归档走批量上传。
                if (E2bSnapshotRefs.decodeSnapshotIdIfPresent(bytes) != null) {
                    super.doHydrateWorkspace(new java.io.ByteArrayInputStream(bytes));
                    return;
                }
                String remote = E2bWorkspaceTransfer.upload(bytes, state, options);
                String command = "archive=" + ConfiguredTool.shellQuote(remote)
                        + "; trap 'rm -f -- \"$archive\"' EXIT; tar -xzf \"$archive\" -C "
                        + ConfiguredTool.shellQuote(state.getWorkspaceRoot());
                doExec(null, command, 300);
            }

            @Override
            protected String getWorkspaceRoot() {
                // BETA1 在 mkdir 前就使用目标目录作为 cwd；仅初始化命令从已存在的根目录启动。
                return creatingWorkspace ? "/" : super.getWorkspaceRoot();
            }
        };
    }

    /** 沿用 BETA1 的按次配置合并规则，连接超时、重试及快照保留沿用客户端默认值。 */
    private static E2bSandboxClientOptions mergeOptions(E2bSandboxClientOptions defaults, E2bSandboxClientOptions requested) {
        var effective = new E2bSandboxClientOptions();
        org.springframework.beans.BeanUtils.copyProperties(defaults, effective);
        if (requested == null) return effective;
        if (requested.getApiKey() != null) effective.setApiKey(requested.getApiKey());
        if (requested.getApiBaseUrl() != null) effective.setApiBaseUrl(requested.getApiBaseUrl());
        if (requested.getDomain() != null) effective.setDomain(requested.getDomain());
        if (requested.getSandboxTimeoutSeconds() > 0) effective.setSandboxTimeoutSeconds(requested.getSandboxTimeoutSeconds());
        if (requested.getRunUser() != null) effective.setRunUser(requested.getRunUser());
        if (requested.getPersistenceMode() != null) effective.setPersistenceMode(requested.getPersistenceMode());
        if (requested.getCodec() != null) effective.setCodec(requested.getCodec());
        if (requested.getTemplateId() != null) effective.setTemplateId(requested.getTemplateId());
        if (requested.getWorkspaceRoot() != null) effective.setWorkspaceRoot(requested.getWorkspaceRoot());
        if (requested.getHttpClient() != null) effective.setHttpClient(requested.getHttpClient());
        return effective;
    }
}
