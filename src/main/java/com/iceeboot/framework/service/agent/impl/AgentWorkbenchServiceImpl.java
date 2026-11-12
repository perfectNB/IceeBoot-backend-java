package com.iceeboot.framework.service.agent.impl;

import com.fasterxml.jackson.databind.*;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.runtime.*;
import com.iceeboot.framework.vo.agent.*;
import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.sandbox.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** 预检只解析资源清单、查询运行环境，不执行导入的技能脚本或安装依赖。 */
@Service
@RequiredArgsConstructor
public class AgentWorkbenchServiceImpl implements IAgentWorkbenchService {
    private final IAgentConfigurationService configs;
    private final IAgentPersistenceService store;
    private final AgentProperties properties;
    private final ObjectMapper json;
    private final AgentRunControl control;
    private static final Set<String> SENSITIVE = Set.of("apiKey","headers","environment");
    private static final Pattern REQUIREMENT = Pattern.compile("^([A-Za-z0-9][A-Za-z0-9_.-]*)(?:\\[.*?])?([<>=!~].*)?$");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*(?:from\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+import|import\\s+([A-Za-z_][A-Za-z0-9_]*))");
    public record Dependencies(List<String> distributions,List<String> modules,boolean hasPython,boolean hasNode,List<String> warnings) {}
    public record PythonCheck(String kind,String name,boolean present,String version) {}

    public static Dependencies dependencies(PublishedAgent p) {
        var distributions = new TreeSet<String>();
        var modules = new TreeSet<String>();
        var local = new HashSet<String>();
        var warnings = new ArrayList<String>();
        boolean python = false,node = false;
        for (var c : p.capabilities()) {
            if (c.kind()!=CapabilityKind.SKILL) continue;
            var resources = c.skill().resources() == null ? Map.<String,String>of() : c.skill().resources();
            for (var entry : resources.entrySet()) {
                String path = entry.getKey(),text=entry.getValue();
                if (path.endsWith(".py")) {
                    python=true;
                    local.add(Path.of(path).getFileName().toString().replaceFirst("\\.py$",""));
                    var matcher = IMPORT.matcher(text);
                    while(matcher.find()) modules.add(matcher.group(1)==null ? matcher.group(2) : matcher.group(1));
                }
                if (path.endsWith("requirements.txt")) {
                    for (String line : text.lines().toList()) {
                        String value=line.split("#",2)[0].trim();
                        if(value.isEmpty()) continue;
                        var matcher=REQUIREMENT.matcher(value);
                        if(matcher.matches()) distributions.add(matcher.group(1));
                        else warnings.add(c.name()+"：依赖清单包含需人工核对的条目，未自动执行。");
                    }
                }
                if(path.endsWith("package.json") || path.endsWith(".js") || path.endsWith(".ts")) node=true;
                if (path.contains("/")) local.add(path.substring(0,path.indexOf('/')));
            }
        }
        modules.removeAll(local);
        return new Dependencies(List.copyOf(distributions),List.copyOf(modules),python,node,List.copyOf(warnings));
    }

    @Override
    public AgentPreflightVO preflight(long agent) {
        long started=System.nanoTime();
        var checks=new ArrayList<AgentPreflightVO.Check>();
        PublishedAgent p;
        try {
            p=configs.preview(agent);
            new SnapshotSkillRepository(p.capabilities().stream().filter(c -> c.kind()==CapabilityKind.SKILL).toList());
        }
        catch(IceeBootException failure) {
            checks.add(new AgentPreflightVO.Check("配置与能力绑定","ERROR",failure.getMessage(),0));
            return new AgentPreflightVO(false,elapsed(started),checks);
        }
        catch(IllegalArgumentException failure) {
            checks.add(new AgentPreflightVO.Check("技能装配","ERROR","技能正文或元数据无效，请检查技能配置。",elapsed(started)));
            return new AgentPreflightVO(false,elapsed(started),checks);
        }
        checks.add(new AgentPreflightVO.Check("配置与能力绑定","PASS","模型已启用，技能、工具与子智能体绑定有效。",elapsed(started)));
        if(!p.children().isEmpty()) checks.add(new AgentPreflightVO.Check("子智能体环境","WARN","已校验发布绑定；子智能体使用各自的发布环境，需分别预检。",0));
        if(p.capabilities().stream().anyMatch(c -> c.kind()==CapabilityKind.MCP)) checks.add(new AgentPreflightVO.Check("MCP 服务","WARN","已校验工具白名单配置；网络连通性和远端权限需实际调用验证。",0));
        Dependencies dependencies=dependencies(p);
        dependencies.warnings().forEach(w -> checks.add(new AgentPreflightVO.Check("依赖清单","WARN",w,0)));
        if(!p.agent().sandboxEnabled()) {
            checks.add(new AgentPreflightVO.Check("运行环境",dependencies.hasPython() || dependencies.hasNode() ? "WARN" : "PASS",
                    dependencies.hasPython() || dependencies.hasNode() ? "技能包含脚本，当前未开启沙箱，无法检查或执行这些脚本。" : "当前配置无需脚本执行环境。",0));
            return result(started,checks);
        }
        var e2b=properties.getE2b();
        if(e2b.getApiKey().isBlank() || e2b.getApiBaseUrl().isBlank() || e2b.getDomain().isBlank()) {
            checks.add(new AgentPreflightVO.Check("沙箱连接","ERROR",ResponseMessage.Agent.SANDBOX_CONNECTION_NOT_CONFIGURED,0));
            return result(started,checks);
        }
        control.acquire();
        Sandbox sandbox=null;
        long environment=System.nanoTime();
        try {
            Capability template=p.capabilities().stream().filter(c -> c.kind()==CapabilityKind.SANDBOX).findFirst().orElseThrow();
            var workspace=new WorkspaceSpec(); workspace.setRoot("/home/user/workspace");
            var spec=new ReleasedE2bFilesystemSpec().apiKey(e2b.getApiKey()).apiBaseUrl(e2b.getApiBaseUrl())
                    .domain(e2b.getDomain()).templateId(template.sandbox().templateId()).workspaceSpec(workspace)
                    .workspaceRoot(workspace.getRoot()).codec(E2bCodec.JSON).sandboxTimeoutSeconds(120);
            var context=spec.toSandboxContext();
            @SuppressWarnings("unchecked") var client=(SandboxClient<E2bSandboxClientOptions>)context.getClient();
            sandbox=client.create(context.getWorkspaceSpec(),context.getSnapshotSpec(),(E2bSandboxClientOptions)context.getClientOptions());
            sandbox.start();
            checks.add(new AgentPreflightVO.Check("沙箱启动与工作目录","PASS","模板可用，工作目录已就绪。",elapsed(environment)));
            if(dependencies.hasPython() || !dependencies.distributions().isEmpty()) {
                long checkStarted=System.nanoTime();
                String script="import json,sys,importlib.util,importlib.metadata\n"
                        +"d=json.loads(sys.argv[1]); rows=[]\n"
                        +"for name in d['distributions']:\n"
                        +" try: rows.append(dict(kind='distribution',name=name,present=True,version=importlib.metadata.version(name)))\n"
                        +" except importlib.metadata.PackageNotFoundError: rows.append(dict(kind='distribution',name=name,present=False,version=''))\n"
                        +"for name in d['modules']:\n"
                        +" rows.append(dict(kind='module',name=name,present=bool(name in sys.stdlib_module_names or importlib.util.find_spec(name)),version=''))\n"
                        +"print(json.dumps(rows))";
                String payload=json.writeValueAsString(dependencies);
                var response=sandbox.exec(null,"python3 -c "+ConfiguredTool.shellQuote(script)+" "+ConfiguredTool.shellQuote(payload),15);
                if(!response.ok() || response.truncated()) checks.add(new AgentPreflightVO.Check("Python 环境","ERROR","Python 不可用或依赖检查未完成，请检查模板。",elapsed(checkStarted)));
                else {
                    var rows=json.readValue(response.stdout(),PythonCheck[].class);
                    if(rows.length==0) checks.add(new AgentPreflightVO.Check("Python 环境","PASS","Python 可用。",elapsed(checkStarted)));
                    for(var row:rows) checks.add(new AgentPreflightVO.Check(row.kind()+" · "+row.name(),row.present()?"PASS":"ERROR",
                            row.present()? (row.version().isBlank()?"模块可定位。":"已安装 "+row.version()+"；版本约束请对照依赖清单。") : "缺少依赖，建议预装到沙箱模板。",elapsed(checkStarted)));
                }
            }
            if(dependencies.hasNode()) {
                long t=System.nanoTime(); var node=sandbox.exec(null,"node --version",10);
                checks.add(new AgentPreflightVO.Check("Node.js 环境",node.ok()?"WARN":"ERROR",node.ok()?"Node.js 可用；npm 包及版本尚需按项目清单核对。":"Node.js 不可用。",elapsed(t)));
            }
        } catch(Exception failure) {
            checks.add(new AgentPreflightVO.Check("运行环境检查","ERROR","检查未完成，请确认模板、网络及服务端沙箱连接配置。",elapsed(environment)));
        } finally {
            try { if(sandbox!=null) sandbox.shutdown(); }
            catch(Exception cleanup) { checks.add(new AgentPreflightVO.Check("预检环境清理","ERROR","临时环境清理失败，请在云控制台检查。",0)); }
            control.release();
        }
        return result(started,checks);
    }
    private static long elapsed(long started) { return (System.nanoTime()-started)/1_000_000; }
    private static AgentPreflightVO result(long started,List<AgentPreflightVO.Check> checks) {
        return new AgentPreflightVO(checks.stream().noneMatch(c -> c.status().equals("ERROR")),elapsed(started),List.copyOf(checks));
    }
    @Override
    public AgentReleaseDiffVO diff(long agent,String from,String to) {
        PublishedAgent a=store.release(from),b=store.release(to);
        if(!Objects.equals(a.agent().id(),agent) || !Objects.equals(b.agent().id(),agent)) throw new IceeBootException(ResponseMessage.Agent.RELEASE_AGENT_MISMATCH);
        var changes=new ArrayList<AgentReleaseDiffVO.Change>();
        compare("agent",json.valueToTree(a.agent()),json.valueToTree(b.agent()),changes);
        compare("model",json.valueToTree(a.model()),json.valueToTree(b.model()),changes);
        compare("capabilities",json.valueToTree(a.capabilities()),json.valueToTree(b.capabilities()),changes);
        compare("children",json.valueToTree(a.children()),json.valueToTree(b.children()),changes);
        return new AgentReleaseDiffVO(a.version(),b.version(),changes);
    }
    private void compare(String path,JsonNode a,JsonNode b,List<AgentReleaseDiffVO.Change> changes) {
        if(Objects.equals(a,b)) return;
        String name=path.substring(path.lastIndexOf('.')+1);
        if(SENSITIVE.contains(name)) {
            changes.add(new AgentReleaseDiffVO.Change(path,"已脱敏","已变更（已脱敏）",true));return;
        }
        if(a!=null && b!=null && a.isObject() && b.isObject()) {
            Set<String> fields=new TreeSet<>();a.fieldNames().forEachRemaining(fields::add);b.fieldNames().forEachRemaining(fields::add);
            fields.forEach(f -> compare(path+"."+f,a.get(f),b.get(f),changes));
        } else if(a!=null && b!=null && a.isArray() && b.isArray()) {
            for(int i=0;i<Math.max(a.size(),b.size());i++) compare(path+"["+i+"]",a.get(i),b.get(i),changes);
        } else {
            // 新增或删除的对象也逐字段展开，避免整块序列化时泄露嵌套密钥。
            if(a!=null && b!=null && a.getNodeType()!=b.getNodeType()) {
                changes.add(new AgentReleaseDiffVO.Change(path,display(redact(a)),display(redact(b)),false));
            } else if((a!=null && a.isContainerNode()) || (b!=null && b.isContainerNode())) {
                JsonNode present=a==null?b:a;
                if(present.isObject()) {
                    present.fieldNames().forEachRemaining(f -> compare(path+"."+f,a==null?null:a.get(f),b==null?null:b.get(f),changes));
                } else for(int i=0;i<present.size();i++) compare(path+"["+i+"]",a==null?null:a.get(i),b==null?null:b.get(i),changes);
            } else changes.add(new AgentReleaseDiffVO.Change(path,display(a),display(b),false));
        }
    }
    private JsonNode redact(JsonNode value) {
        JsonNode copy=value.deepCopy();
        if(copy.isObject()) {
            var object=(com.fasterxml.jackson.databind.node.ObjectNode)copy;
            var fields=new ArrayList<String>();object.fieldNames().forEachRemaining(fields::add);
            for(String field:fields) {
                if(SENSITIVE.contains(field)) object.put(field,"已脱敏");
                else object.set(field,redact(object.get(field)));
            }
        } else if(copy.isArray()) {
            var array=(com.fasterxml.jackson.databind.node.ArrayNode)copy;
            for(int i=0;i<array.size();i++) array.set(i,redact(array.get(i)));
        }
        return copy;
    }
    private static String display(JsonNode value) {
        String text=value==null ? "（不存在）" : value.isTextual()?value.textValue():value.toString();
        int maxPreviewCharacters=8192;
        return text.length()<=maxPreviewCharacters ? text : text.substring(0,maxPreviewCharacters)+"\n…（内容较长，已截断展示）";
    }
}
