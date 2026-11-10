package com.iceeboot.framework.controller.agent;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.agent.*;
import com.iceeboot.framework.dto.update.agent.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理入口默认只授权超级管理员，按请求方法与路径动态校验权限，缺少配置时禁止放行。 */
@Tag(name = "智能体控制台")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/agent-console")
public class AgentConsoleController {
    private final IAgentConfigurationService configs;
    private final IAgentExecutionService execution;
    private final IAgentPersistenceService store;
    private final ISysUserService users;

    private final AgentApiConverter converter;

    private Caller admin() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null || !users.getUserRoles(id).contains("superAdmin"))
            throw new IceeBootException(
                    ResultCodeEnum.FORBIDDEN.getCode(), ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
        return new Caller(id.toString(), TokenUtils.getToken());
    }

    @Operation(summary = "Get /models")
    @GetMapping("/models")
    @RequirePermission(dynamic = true)
    public R<List<AgentModelVO>> models() {
        admin();
        return R.ok(
                configs.models().stream()
                        .map(value -> converter.convert(value, AgentModelVO.class))
                        .toList());
    }

    @Operation(summary = "Post /models")
    @PostMapping("/models")
    @RequirePermission(dynamic = true)
    public R<AgentModelVO> saveModel(@Valid @RequestBody AgentModelDTO request) {
        admin();
        return R.ok(
                converter.convert(
                        configs.saveModel(converter.convert(request, ModelConfig.class)),
                        AgentModelVO.class));
    }

    @Operation(summary = "Delete /models/{id}")
    @DeleteMapping("/models/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteModel(@PathVariable long id) {
        admin();
        store.deleteModel(id);
        return R.ok();
    }

    @Operation(summary = "Post /models/invoke")
    @PostMapping("/models/invoke")
    @RequirePermission(dynamic = true)
    public R<String> invokeModel(@Valid @RequestBody AgentModelInvokeDTO request) {
        Caller caller = admin();
        return R.ok(
                ResponseMessage.Agent.INVOKE_SUCCESS,
                execution.invokeModel(
                        converter.convert(request, ModelInvokeRequest.class), caller));
    }

    @Operation(summary = "Get /capabilities")
    @GetMapping("/capabilities")
    @RequirePermission(dynamic = true)
    public R<List<AgentCapabilityVO>> capabilities() {
        admin();
        return R.ok(
                configs.capabilities().stream()
                        .map(value -> converter.convert(value, AgentCapabilityVO.class))
                        .toList());
    }

    @Operation(summary = "Post /capabilities")
    @PostMapping("/capabilities")
    @RequirePermission(dynamic = true)
    public R<AgentCapabilityVO> saveCapability(@Valid @RequestBody AgentCapabilityDTO request) {
        admin();
        return R.ok(
                converter.convert(
                        configs.saveCapability(converter.convert(request, Capability.class)),
                        AgentCapabilityVO.class));
    }

    @Operation(summary = "Delete /capabilities/{id}")
    @DeleteMapping("/capabilities/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteCapability(@PathVariable long id) {
        admin();
        store.deleteCapability(id);
        return R.ok();
    }

    @Operation(summary = "Get /agents")
    @GetMapping("/agents")
    @RequirePermission(dynamic = true)
    public R<List<AgentVO>> agents() {
        admin();
        return R.ok(
                configs.agents().stream()
                        .map(value -> converter.convert(value, AgentVO.class))
                        .toList());
    }

    @Operation(summary = "Get /agents/{id}")
    @GetMapping("/agents/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentVO> agent(@PathVariable long id) {
        admin();
        return R.ok(converter.convert(configs.agent(id), AgentVO.class));
    }

    @Operation(summary = "Post /agents")
    @PostMapping("/agents")
    @RequirePermission(dynamic = true)
    public R<AgentVO> saveAgent(@Valid @RequestBody AgentDefinitionDTO request) {
        admin();
        return R.ok(
                converter.convert(
                        configs.saveAgent(converter.convert(request, AgentSpec.class)),
                        AgentVO.class));
    }

    @Operation(summary = "Delete /agents/{id}")
    @DeleteMapping("/agents/{id}")
    @RequirePermission(dynamic = true)
    public R<Void> deleteAgent(@PathVariable long id) {
        admin();
        store.deleteAgent(id);
        return R.ok();
    }

    @Operation(summary = "Get /agents/{id}/releases")
    @GetMapping("/agents/{id}/releases")
    @RequirePermission(dynamic = true)
    public R<List<AgentReleaseVO>> releases(@PathVariable long id) {
        admin();
        return R.ok(
                store.releases(id).stream()
                        .map(value -> converter.convert(value, AgentReleaseVO.class))
                        .toList());
    }

    @Operation(summary = "Post /agents/{id}/publish")
    @PostMapping("/agents/{id}/publish")
    @RequirePermission(dynamic = true)
    public R<AgentReleaseVO> publish(@PathVariable long id) {
        Caller caller = admin();
        return R.ok(converter.convert(configs.publish(id, caller.userId()), AgentReleaseVO.class));
    }

    @Operation(summary = "Post /agents/by-code/{code}/invoke")
    @PostMapping("/agents/by-code/{code}/invoke")
    @RequirePermission(dynamic = true)
    public R<AgentInvokeVO> invoke(
            @PathVariable String code, @Valid @RequestBody AgentInvokeDTO request) {
        Caller caller = admin();
        return R.ok(
                converter.convert(
                        execution.invoke(
                                code, converter.convert(request, InvokeRequest.class), caller),
                        AgentInvokeVO.class));
    }

    @Operation(summary = "Get /runs")
    @GetMapping("/runs")
    @RequirePermission(dynamic = true)
    public R<List<AgentRunVO>> runs() {
        admin();
        return R.ok(
                store.runs().stream()
                        .map(value -> converter.convert(value, AgentRunVO.class))
                        .toList());
    }

    @Operation(summary = "Get /runs/{id}")
    @GetMapping("/runs/{id}")
    @RequirePermission(dynamic = true)
    public R<AgentRunDetailVO> run(@PathVariable String id) {
        admin();
        return R.ok(converter.convert(store.runDetail(id), AgentRunDetailVO.class));
    }

    @Operation(summary = "更新models")
    @PutMapping("/models")
    @RequirePermission(dynamic = true)
    public R<AgentModelVO> updateModelConfig(@Valid @RequestBody AgentModelUpdateDTO request) {
        admin();
        return R.ok(
                converter.convert(
                        configs.saveModel(converter.convert(request, ModelConfig.class)),
                        AgentModelVO.class));
    }

    @Operation(summary = "更新agents")
    @PutMapping("/agents")
    @RequirePermission(dynamic = true)
    public R<AgentVO> updateAgentSpec(@Valid @RequestBody AgentDefinitionUpdateDTO request) {
        admin();
        return R.ok(
                converter.convert(
                        configs.saveAgent(converter.convert(request, AgentSpec.class)),
                        AgentVO.class));
    }

    @Operation(summary = "更新capabilities")
    @PutMapping("/capabilities")
    @RequirePermission(dynamic = true)
    public R<AgentCapabilityVO> updateCapability(
            @Valid @RequestBody AgentCapabilityUpdateDTO request) {
        admin();
        return R.ok(
                converter.convert(
                        configs.saveCapability(converter.convert(request, Capability.class)),
                        AgentCapabilityVO.class));
    }
}
