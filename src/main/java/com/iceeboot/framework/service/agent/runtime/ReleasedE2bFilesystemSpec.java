package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.service.agent.IAgentFileService;
import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.sandbox.*;

/** 兼容 E2B 工作区首次初始化，并避免持久化每轮释放后已失效的连接凭据。 */
public final class ReleasedE2bFilesystemSpec extends E2bFilesystemSpec {
    private RunRecorder recorder;
    private java.util.function.Consumer<Sandbox> ready = sandbox -> {};
    public ReleasedE2bFilesystemSpec observe(RunRecorder value, java.util.function.Consumer<Sandbox> callback) {
        recorder = value;
        ready = callback;
        return this;
    }
    private void begin(String key,String title) { if (recorder != null) recorder.beginStage(key,title); }
    private void end(String key,String status) { if (recorder != null) recorder.endStage(key,status); }
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

    private Sandbox bootstrapSandbox(E2bSandboxState state,E2bSandboxClientOptions options) {
        return new RuntimeE2bSandbox(state,options);
    }
    private final class RuntimeE2bSandbox extends E2bSandbox implements SandboxFileTransfer {
        private final E2bSandboxState state;
        private final E2bSandboxClientOptions options;
        RuntimeE2bSandbox(E2bSandboxState state,E2bSandboxClientOptions options) {
            super(state,options);this.state=state;this.options=options;
        }
        @Override public boolean supportsFileTransfer(String path) { return true; }
        private String filePath(String path) {
            if(path==null || path.isBlank() || path.indexOf('\0')>=0) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
            java.nio.file.Path root=java.nio.file.Path.of(state.getWorkspaceRoot()).normalize();
            java.nio.file.Path file=root.resolve(path).normalize();
            if(!file.startsWith(root) || file.equals(root)) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
            return file.toString();
        }
        @Override public void uploadFile(String path,byte[] content) throws Exception {
            if(content==null || content.length>IAgentFileService.MAX_BYTES)
                throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
            String target=filePath(path);
            var result=doExec(null,"mkdir -p -- "+ConfiguredTool.shellQuote(java.nio.file.Path.of(target).getParent().toString()),15);
            if(!result.ok()) throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED);
            E2bWorkspaceTransfer.uploadFile(target,content,state,options);
        }
        @Override public byte[] downloadFile(String path) {
            return E2bWorkspaceTransfer.downloadFile(filePath(path),state,options);
        }
        private boolean creatingWorkspace;
        @Override
        public void start() throws Exception {
            begin("SANDBOX_START", "启动沙箱与准备工作区");
            try { super.start(); ready.accept(this); end("SANDBOX_START","SUCCEEDED"); }
            catch (Exception failure) {
                end("SANDBOX_START","FAILED");
                try { shutdown(); } catch(Exception cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        }

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
            begin("WORKSPACE_SYNC", "批量同步工作区文件");
            try {
            String remote = E2bWorkspaceTransfer.upload(bytes, state, options);
            String command = "archive=" + ConfiguredTool.shellQuote(remote)
                    + "; trap 'rm -f -- \"$archive\"' EXIT; tar -xzf \"$archive\" -C "
                    + ConfiguredTool.shellQuote(state.getWorkspaceRoot());
            doExec(null, command, 300);
            end("WORKSPACE_SYNC","SUCCEEDED");
            } catch (Exception failure) { end("WORKSPACE_SYNC","FAILED"); throw failure; }
        }

        @Override
        protected String getWorkspaceRoot() {
            // BETA1 在 mkdir 前就使用目标目录作为 cwd；仅初始化命令从已存在的根目录启动。
            return creatingWorkspace ? "/" : super.getWorkspaceRoot();
        }
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
