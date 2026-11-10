package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.security.AgentSecrets;
import com.iceeboot.framework.service.system.ISysUserService;

import io.agentscope.core.message.*;
import io.agentscope.core.tool.*;

import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** 后台 HTTP/脚本配置的实际执行边界，调用者身份不作为模型参数暴露。 */
public final class ConfiguredTool extends ToolBase {
    private final Capability definition;
    private final Caller caller;
    private final AgentProperties properties;
    private final AgentSecrets secrets;
    private final IAgentPersistenceService store;
    private final ISysUserService users;
    private final boolean sandboxEnabled;

    public ConfiguredTool(
            Capability c,
            Caller caller,
            AgentProperties properties,
            AgentSecrets secrets,
            IAgentPersistenceService store,
            ISysUserService users,
            boolean sandboxEnabled) {
        super(
                ToolBase.builder()
                        .name(c.code())
                        .description(c.description() == null ? c.name() : c.description())
                        .inputSchema(c.tool().inputSchema())
                        .readOnly("GET".equals(c.tool().method()))
                        .concurrencySafe(false));
        this.definition = c;
        this.caller = caller;
        this.properties = properties;
        this.secrets = secrets;
        this.store = store;
        this.users = users;
        this.sandboxEnabled = sandboxEnabled;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.defer(
                        () -> {
                            ToolConfig tool = definition.tool();
                            SchemaSupport.validate(tool.inputSchema(), param.getInput());
                            if (tool.kind() == ToolKind.SCRIPT) {
                                AgentTool execute =
                                        sandboxEnabled && param.getAgent() != null
                                                ? param.getAgent().getToolkit().getTool("execute")
                                                : null;
                                if (execute == null)
                                    return Mono.error(
                                            new IceeBootException(
                                                    ResultCodeEnum.ERROR.getCode(),
                                                    ResponseMessage.Agent
                                                            .TOOL_SANDBOX_UNAVAILABLE));
                                String command =
                                        "printf '%s' "
                                                + shellQuote(store.json(param.getInput()))
                                                + " | "
                                                + tool.script();
                                return execute.callAsync(
                                        ToolCallParam.builder(param)
                                                .input(Map.of("command", command))
                                                .build());
                            }
                            String url = tool.url();
                            if (tool.projectApi()) {
                                if (caller == null
                                        || caller.accessToken() == null
                                        || !users.getUserPermissions(Long.valueOf(caller.userId()))
                                                .contains(tool.permissionCode()))
                                    return Mono.error(
                                            new IceeBootException(
                                                    ResultCodeEnum.FORBIDDEN.getCode(),
                                                    ResponseMessage.Agent
                                                            .TOOL_PROJECT_PERMISSION_DENIED));
                                url =
                                        properties.getProjectApiBaseUrl().replaceAll("/$", "")
                                                + tool.url();
                            }
                            if ("GET".equals(tool.method()) && !param.getInput().isEmpty()) {
                                StringBuilder query = new StringBuilder(url);
                                query.append(url.contains("?") ? '&' : '?');
                                param.getInput()
                                        .forEach(
                                                (k, v) ->
                                                        query.append(encode(k))
                                                                .append('=')
                                                                .append(
                                                                        encode(
                                                                                v instanceof String
                                                                                        ? (String) v
                                                                                        : store
                                                                                                .json(
                                                                                                        v)))
                                                                .append('&'));
                                url = query.toString();
                            }
                            var client =
                                    WebClient.builder()
                                            .codecs(
                                                    c ->
                                                            c.defaultCodecs()
                                                                    .maxInMemorySize(1024 * 1024))
                                            .build();
                            var request =
                                    client.method(
                                                    org.springframework.http.HttpMethod.valueOf(
                                                            tool.method()))
                                            .uri(URI.create(url));
                            if (tool.headers() != null)
                                tool.headers()
                                        .forEach((k, v) -> request.header(k, secrets.decrypt(v)));
                            if (tool.projectApi())
                                request.headers(h -> h.setBearerAuth(caller.accessToken()));
                            if (!"GET".equals(tool.method()))
                                request.contentType(
                                                org.springframework.http.MediaType.APPLICATION_JSON)
                                        .bodyValue(param.getInput());
                            return request.retrieve()
                                    .bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .timeout(Duration.ofSeconds(30))
                                    .map(
                                            text ->
                                                    ToolResultBlock.text(text)
                                                            .withState(ToolResultState.SUCCESS)
                                                            .withIdAndName(
                                                                    param.getToolUseBlock().getId(),
                                                                    getName()));
                        })
                .onErrorResume(
                        e ->
                                Mono.just(
                                        ToolResultBlock.error(
                                                        ResponseMessage.Agent.TOOL_EXECUTION_FAILED)
                                                .withIdAndName(
                                                        param.getToolUseBlock().getId(),
                                                        getName())));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
