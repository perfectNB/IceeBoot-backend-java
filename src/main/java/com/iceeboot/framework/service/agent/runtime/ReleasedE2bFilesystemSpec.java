package com.iceeboot.framework.service.agent.runtime;

import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.sandbox.*;

/** SDK 在每轮结束先 stop/shutdown，再持久化状态；不保存已销毁实例的连接凭据。 */
public final class ReleasedE2bFilesystemSpec extends E2bFilesystemSpec {
    @Override
    protected SandboxClient<?> createClient() {
        return new E2bSandboxClient((E2bSandboxClientOptions) clientOptions(), null) {
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
}
