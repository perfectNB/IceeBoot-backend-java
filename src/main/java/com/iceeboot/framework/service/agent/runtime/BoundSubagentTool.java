package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;

import io.agentscope.core.message.*;
import io.agentscope.core.tool.*;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 同步委派给已发布子 Agent，实际推理和工具循环仍由子 Agent 的 Harness 执行。 */
final class BoundSubagentTool extends ToolBase {
    private final Function<String, Mono<Msg>> invoke;

    BoundSubagentTool(String code, String description, Function<String, Mono<Msg>> invoke) {
        super(
                ToolBase.builder()
                        .name("agent_" + code)
                        .description(description)
                        .inputSchema(
                                Map.of(
                                        "type",
                                        "object",
                                        "properties",
                                        Map.of(
                                                "task",
                                                Map.of(
                                                        "type",
                                                        "string",
                                                        "minLength",
                                                        1,
                                                        "maxLength",
                                                        100000)),
                                        "required",
                                        List.of("task"),
                                        "additionalProperties",
                                        false))
                        .concurrencySafe(false));
        this.invoke = invoke;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.defer(
                        () -> {
                            SchemaSupport.validate(getParameters(), param.getInput());
                            return invoke.apply((String) param.getInput().get("task"));
                        })
                .map(
                        result ->
                                ToolResultBlock.text(RunRecorder.text(result))
                                        .withState(ToolResultState.SUCCESS)
                                        .withIdAndName(param.getToolUseBlock().getId(), getName()))
                .onErrorResume(
                        e ->
                                Mono.just(
                                        ToolResultBlock.error(
                                                        ResponseMessage.Agent
                                                                .AGENT_CHILD_EXECUTION_FAILED)
                                                .withIdAndName(
                                                        param.getToolUseBlock().getId(),
                                                        getName())));
    }
}
