package com.iceeboot.framework.agent;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.impl.AgentWorkbenchServiceImpl;
import com.iceeboot.framework.service.agent.runtime.*;
import org.junit.jupiter.api.*;
import java.util.*;

class AgentWorkbenchTest {
    AgentFixture f;
    @BeforeEach void setup() throws Exception { f=new AgentFixture(); }
    PublishedAgent publish(String code,boolean sandbox,List<Long> skills) {
        Long template=null;
        if(sandbox) template=f.configs.saveCapability(new Capability(null,CapabilityKind.SANDBOX,"sandbox_"+code,"sandbox","",true,null,null,null,new SandboxConfig("test-template",120))).id();
        return f.publish(f.save(code,f.model().id(),skills,List.of(),List.of(),List.of(),sandbox,template));
    }
    AgentWorkbenchServiceImpl workbench() {
        return new AgentWorkbenchServiceImpl(f.configs,f.store,f.properties,f.json,mock(AgentRunControl.class));
    }
    @Test void rollbackCreatesNewImmutableReleaseAndLeavesDraftAndExistingSessionsUntouched() {
        var first=publish("rollback",false,List.of());
        var session=f.conversations().open(first.agent().id(),"1","old_session",first,"old");
        var second=f.configs.publish(first.agent().id(),"2");
        f.jdbc.update("UPDATE t_agent_definition SET prompt='new draft' WHERE id=?",first.agent().id());
        var restored=f.configs.rollback(first.agent().id(),first.releaseId(),"3");
        assertThat(restored.version()).isEqualTo(3);
        var current=f.store.agent(first.agent().id());
        assertThat(current.activeReleaseId()).isEqualTo(restored.id());
        assertThat(current.draft().prompt()).isEqualTo("new draft");
        assertThat(f.store.release(restored.id()).agent()).isEqualTo(first.agent());
        assertThat(f.conversations().require(first.agent().id(),"1","old_session").getReleaseId()).isEqualTo(session.getReleaseId());
        assertThat(f.store.releases(first.agent().id())).hasSize(3);
        assertThat(f.store.release(second.id()).version()).isEqualTo(2);
    }
    @Test void foreignReleaseCannotBeComparedOrRolledBack() {
        var a=publish("a",false,List.of());var b=publish("b",false,List.of());
        assertThatThrownBy(() -> f.configs.rollback(a.agent().id(),b.releaseId(),"1")).hasMessage(ResponseMessage.Agent.RELEASE_AGENT_MISMATCH);
        assertThatThrownBy(() -> workbench().diff(a.agent().id(),a.releaseId(),b.releaseId())).hasMessage(ResponseMessage.Agent.RELEASE_AGENT_MISMATCH);
        assertThat(f.store.releases(a.agent().id())).hasSize(1);
    }
    @Test void diffMasksChangedAndRemovedSecretsAtEveryDepth() {
        var tool=f.configs.saveCapability(new Capability(null,CapabilityKind.TOOL,"secret_tool","secret","",true,null,
                new ToolConfig(ToolKind.HTTP,"https://example.invalid","GET",Map.of("Authorization","private-header"),Map.of("type","object"),null,false,null),null,null));
        var a=f.publish(f.save("secrets",f.model().id(),List.of(),List.of(tool.id()),List.of(),List.of(),false,null));
        f.jdbc.update("UPDATE t_agent_definition SET tool_ids_json='[]',environment_json=? WHERE id=?",f.json.valueToTree(Map.of("TOKEN",f.secrets.encrypt("private-env"))).toString(),a.agent().id());
        var b=f.configs.publish(a.agent().id(),"1");
        var diff=workbench().diff(a.agent().id(),a.releaseId(),b.id());
        assertThat(diff.changes()).anyMatch(c -> c.sensitive() && c.path().endsWith("headers"));
        assertThat(diff.changes()).anyMatch(c -> c.sensitive() && c.path().endsWith("environment"));
        assertThat(f.store.json(diff)).doesNotContain("private-header","private-env","enc:v");
        assertThat(workbench().diff(a.agent().id(),a.releaseId(),a.releaseId()).changes()).isEmpty();
    }
    @Test void preflightValidatesBindingsAndReportsMissingConnectionWithoutCallingModelOrCloud() {
        var a=publish("preflight",true,List.of());
        var result=workbench().preflight(a.agent().id());
        assertThat(result.passed()).isFalse();
        assertThat(result.checks()).anyMatch(c -> c.name().equals("沙箱连接") && c.status().equals("ERROR"));
        f.jdbc.update("UPDATE t_agent_model SET enabled=FALSE WHERE id=?",a.model().id());
        assertThat(workbench().preflight(a.agent().id()).checks()).anyMatch(c -> c.name().equals("配置与能力绑定") && c.status().equals("ERROR"));
    }
    @Test void optionalBlankSkillDescriptionUsesDisplayNameForSdkAssembly() {
        var skill=new Capability(1L,CapabilityKind.SKILL,"optional_skill","显示名称","",true,
                new SkillConfig("instructions",Map.of(),List.of()),null,null,null);
        var repository=new SnapshotSkillRepository(List.of(skill));
        assertThat(repository.getSkill("optional_skill").getDescription()).isEqualTo("显示名称");
    }
    @Test void dependencyScanReadsManifestsAndImportsWithoutExecutingSkillCode() {
        var skill=f.configs.saveCapability(new Capability(null,CapabilityKind.SKILL,"check_skill","skill","",true,
                new SkillConfig("instructions",Map.of("requirements.txt","lunar-python==1.4.9\n# comment\n-e git+https://example.invalid/pkg",
                    "scripts/run.py","from lunar_python import Solar\nimport json\nfrom helper import foo\nraise Exception('MUST NOT RUN')",
                    "scripts/helper.py","print('MUST NOT RUN')"),List.of()),null,null,null));
        var a=publish("dependencies",true,List.of(skill.id()));
        var d=AgentWorkbenchServiceImpl.dependencies(a);
        assertThat(d.distributions()).containsExactly("lunar-python");
        assertThat(d.modules()).contains("lunar_python","json").doesNotContain("helper");
        assertThat(d.warnings()).hasSize(1);
        assertThat(d.hasPython()).isTrue();
    }
}
