package com.iceeboot.agent.web;

import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.persistence.AgentStore;
import com.iceeboot.agent.service.*;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.system.ISysUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 管理入口默认只授权超级管理员，使用固定权限码，未配置权限时也不能匿名放行。 */
@RestController
@RequestMapping("/v1/agent-console")
public class AgentConsoleController {
  private final AgentConfigurationService configs;
  private final AgentExecutionService execution;
  private final AgentStore store;
  private final ISysUserService users;

  public AgentConsoleController(
      AgentConfigurationService configs,
      AgentExecutionService execution,
      AgentStore store,
      ISysUserService users) {
    this.configs = configs;
    this.execution = execution;
    this.store = store;
    this.users = users;
  }

  private Caller admin() {
    Long id = TokenUtils.getCurrentUserId();
    if (id == null || !users.getUserRoles(id).contains("superAdmin"))
      throw new SecurityException("仅超级管理员可以使用智能体控制台");
    return new Caller(id.toString(), TokenUtils.getToken());
  }

  @GetMapping("/models")
  @RequirePermission(code = "ai:model:read")
  public R<List<ModelConfig>> models() {
    admin();
    return R.ok(configs.models());
  }

  @PostMapping("/models")
  @RequirePermission(code = "ai:model:write")
  public R<ModelConfig> saveModel(@Valid @RequestBody ModelConfig request) {
    admin();
    return R.ok(configs.saveModel(request));
  }

  @DeleteMapping("/models/{id}")
  @RequirePermission(code = "ai:model:write")
  public R<Void> deleteModel(@PathVariable long id) {
    admin();
    store.deleteModel(id);
    return R.ok();
  }

  @PostMapping("/models/invoke")
  @RequirePermission(code = "ai:model:invoke")
  public R<String> invokeModel(@Valid @RequestBody ModelInvokeRequest request) {
    admin();
    return R.ok("调用成功", execution.invokeModel(request));
  }

  @GetMapping("/capabilities")
  @RequirePermission(code = "ai:capability:read")
  public R<List<Capability>> capabilities() {
    admin();
    return R.ok(configs.capabilities());
  }

  @PostMapping("/capabilities")
  @RequirePermission(code = "ai:capability:write")
  public R<Capability> saveCapability(@Valid @RequestBody Capability request) {
    admin();
    return R.ok(configs.saveCapability(request));
  }

  @DeleteMapping("/capabilities/{id}")
  @RequirePermission(code = "ai:capability:write")
  public R<Void> deleteCapability(@PathVariable long id) {
    admin();
    store.deleteCapability(id);
    return R.ok();
  }

  @GetMapping("/agents")
  @RequirePermission(code = "ai:agent:read")
  public R<List<AgentView>> agents() {
    admin();
    return R.ok(configs.agents());
  }

  @GetMapping("/agents/{id}")
  @RequirePermission(code = "ai:agent:read")
  public R<AgentView> agent(@PathVariable long id) {
    admin();
    return R.ok(configs.agent(id));
  }

  @PostMapping("/agents")
  @RequirePermission(code = "ai:agent:write")
  public R<AgentView> saveAgent(@Valid @RequestBody AgentSpec request) {
    admin();
    return R.ok(configs.saveAgent(request));
  }

  @DeleteMapping("/agents/{id}")
  @RequirePermission(code = "ai:agent:write")
  public R<Void> deleteAgent(@PathVariable long id) {
    admin();
    store.deleteAgent(id);
    return R.ok();
  }

  @GetMapping("/agents/{id}/releases")
  @RequirePermission(code = "ai:agent:read")
  public R<List<ReleaseView>> releases(@PathVariable long id) {
    admin();
    return R.ok(store.releases(id));
  }

  @PostMapping("/agents/{id}/publish")
  @RequirePermission(code = "ai:agent:publish")
  public R<ReleaseView> publish(@PathVariable long id) {
    Caller caller = admin();
    return R.ok(configs.publish(id, caller.userId()));
  }

  @PostMapping("/agents/by-code/{code}/invoke")
  @RequirePermission(code = "ai:agent:invoke")
  public R<InvokeResult> invoke(
      @PathVariable String code, @Valid @RequestBody InvokeRequest request) {
    Caller caller = admin();
    return R.ok(execution.invoke(code, request, caller));
  }

  @GetMapping("/runs")
  @RequirePermission(code = "ai:run:read")
  public R<List<RunView>> runs() {
    admin();
    return R.ok(store.runs());
  }

  @GetMapping("/runs/{id}")
  @RequirePermission(code = "ai:run:read")
  public R<RunDetail> run(@PathVariable String id) {
    admin();
    return R.ok(store.runDetail(id));
  }
}
