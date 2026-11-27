package com.iceeboot.framework.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.framework.controller.workflow.ApprovalController;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign.*;
import com.iceeboot.framework.dto.update.workflow.ApprovalRequests.*;
import com.iceeboot.framework.service.system.ISysUserService;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalLifecycleTest {
    final ISysUserService users = mock(ISysUserService.class);
    org.springframework.transaction.support.TransactionTemplate transactions;
    ApprovalDesignService designs;
    ApprovalCaseService cases;
    org.activiti.engine.ProcessEngine processEngine;
    JdbcTemplate jdbc;
    @AfterEach void close() { if (processEngine != null) processEngine.close(); }
    final Exit end = new Exit(List.of(), "@end");
    final List<Field> fields = List.of(new Field("state","状态","routing","number",true),
            new Field("sex","类别","routing","number",true),new Field("companyName","公司","business","text",true));
    final List<Node> nodes = List.of(new Node("A","财务审核","finance",new Exit(List.of(),"C")),
            new Node("B","部门审批","manager",end),new Node("C","负责人审批","manager",end));
    final ApprovalDesign design = new ApprovalDesign("purchase","采购审批",fields,
            new Exit(List.of(branch("1","2","A"),branch("1","1","C"),branch("2","1","B")), ""),nodes);
    static Branch branch(String state,String sex,String target) {
        return new Branch(List.of(new Condition("state",state),new Condition("sex",sex)),target);
    }

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ClassPathResource("db/agent/V025__approval_workflow.sql"),new ClassPathResource("db/agent/V027__approval_engine.sql"),
                new ClassPathResource("db/agent/V029__approval_intake.sql"),new ClassPathResource("db/agent/V031__approval_audience.sql"),
                new ClassPathResource("db/agent/V033__approval_launch_roles.sql")).execute(ds);
        // Activiti 7 ships legacy H2 DDL, while application migrations use MySQL syntax.
        ds.setUrl(ds.getUrl().replace("MODE=MySQL","MODE=LEGACY"));
        transactions = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE t_role(id INT, code VARCHAR(20), name VARCHAR(20),status VARCHAR(10),sort INT,del_flag INT)");
        jdbc.update("INSERT INTO t_role VALUES(1,'finance','财务','active',0,0),(2,'manager','经理','active',0,0)");
        jdbc.execute("CREATE TABLE t_user(id BIGINT,username VARCHAR(64),nickname VARCHAR(128),status VARCHAR(10),del_flag INT)");
        jdbc.execute("CREATE TABLE t_user_role(user_id BIGINT,role_id BIGINT,del_flag INT)");
        jdbc.update("INSERT INTO t_user VALUES(2,'finance_user','林宁','active',0)");
        jdbc.update("INSERT INTO t_user_role VALUES(2,1,0)");
        var json = new ObjectMapper();
        var config = new com.iceeboot.config.activity.ActivitiConfig(ds,transactions.getTransactionManager());
        processEngine = config.processEngineConfiguration().buildProcessEngine();
        var engine = new ApprovalEngine(jdbc,processEngine.getRepositoryService(),processEngine.getRuntimeService(),processEngine.getTaskService());
        designs = new ApprovalDesignService(jdbc,json,engine,users);
        cases = new ApprovalCaseService(jdbc,designs,users,json,engine);
        when(users.getUserRoles(2L)).thenReturn(List.of("finance"));
        when(users.getUserRoles(3L)).thenReturn(List.of("manager"));
        designs.save(new Draft(0,design));
    }
    static Map<String,String> form(String state,String sex,String company) {
        return Map.of("state",state,"sex",sex,"companyName",company);
    }

    @Test void publishedOnlyAndUniqueKeysAndConditionalRoutes() {
        assertThat(designs.designs(true)).isEmpty();
        assertThatThrownBy(() -> designs.save(new Draft(0,design))).isInstanceOf(IceeBootException.class);
        var repeated = new ApprovalDesign("other","重复字段",List.of(fields.get(0),fields.get(0)),design.start(),nodes);
        assertThatThrownBy(() -> ApprovalRouting.validate(repeated,Set.of("finance","manager"))).hasMessageContaining("字段");
        designs.publish(design.code(),1);
        assertThatThrownBy(() -> designs.publish(design.code(),1)).hasMessageContaining("更新");
        assertThat(ApprovalRouting.plan(design,form("1.00","2","甲"))).extracting(Node::id).containsExactly("A","C");
        assertThat(ApprovalRouting.plan(design,form("1","1","甲"))).extracting(Node::id).containsExactly("C");
        assertThat(ApprovalRouting.plan(design,form("2","1","甲"))).extracting(Node::id).containsExactly("B");
        assertThatThrownBy(() -> ApprovalRouting.plan(design,form("2","2","甲"))).hasMessageContaining("没有匹配");
        assertThatThrownBy(() -> ApprovalRouting.plan(design,form("1","1",""))).hasMessageContaining("公司");
    }

    @Test void reviewRejectResubmitAndAuditWithinOneCase() {
        designs.publish(design.code(),1);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            var response = new ApprovalController(designs,cases).submit(new Submit("purchase",1,form("1","2","甲")));
            String id = response.getResult();
            assertThat(id).isNotBlank();
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(cases.list("inbox")).isEmpty();
            assertThatThrownBy(() -> cases.detail(id)).hasMessageContaining("无权");
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",null))).hasMessageContaining("角色");
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            assertThat(cases.list("inbox")).hasSize(1);
            cases.review(id,"approve",new Change(1,"预算核对完毕",null));
            assertThat(cases.list("inbox")).isEmpty();
            assertThat(cases.list("handled")).hasSize(1);
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",null))).hasMessageContaining("刷新");
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(cases.detail(id).history().get(1).reason()).isEqualTo("预算核对完毕");
            assertThatThrownBy(() -> cases.review(id,"reject",new Change(2," ",null))).hasMessageContaining("理由");
            cases.review(id,"reject",new Change(2,"请更正公司",null));
            assertThatThrownBy(() -> cases.revise(id,"void",new Change(3,"",null))).hasMessageContaining("发起人");
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            assertThat(cases.list("rejected")).hasSize(1);
            var nextVersion = new ApprovalDesign("purchase","新版本",fields,new Exit(List.of(),"B"),List.of(nodes.get(1)));
            designs.save(new Draft(2,nextVersion));
            designs.publish("purchase",3);
            cases.revise(id,"resubmit",new Change(3,"",form("2","1","乙")));
            assertThat(cases.detail(id).version()).isEqualTo(1);
            assertThat(cases.detail(id).item().round()).isEqualTo(2);
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            cases.review(id,"approve",new Change(4,"同意采购",null));
            var detail = cases.detail(id);
            assertThat(detail.item().status()).isEqualTo("APPROVED");
            assertThat(detail.history()).hasSize(5);
            assertThat(detail.history().get(0).values()).containsEntry("companyName","甲");
            assertThat(detail.history().get(3).values()).containsEntry("companyName","乙");
            assertThat(detail.history().get(4).time()).isNotNull();
            assertThatThrownBy(() -> cases.revise(id,"resubmit",new Change(5,"",form("2","1","丙")))).isInstanceOf(IceeBootException.class);
        }
    }

    @Test void launchAudienceIsIndependentOfTheReviewerRole() {
        designs.save(new Draft(1, new ApprovalDesign(design.code(),design.name(),fields,design.start(),nodes,"manager")));
        designs.publish(design.code(),2);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            assertThat(designs.designs(true)).isEmpty();
            assertThat(designs.page(true,1,10,"采购").getTotal()).isZero();
            assertThatThrownBy(() -> designs.detail(design.code(),true)).isInstanceOf(IceeBootException.class);
            var request = new Submit(design.code(),1,form("1","2","甲"));
            assertThatThrownBy(() -> cases.submit(request)).hasMessageContaining("无权");
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(designs.page(true,1,10,"采购").getTotal()).isOne();
            assertThat(designs.page(true,1,10,"合同").getTotal()).isZero();
            var id = cases.submit(request);
            var detail = cases.detail(id);
            assertThat(detail.candidates().get("finance")).containsExactly("林宁");
            var task = detail.tasks().getFirst().id();
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"确认",null,task))).isInstanceOf(IceeBootException.class);
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            var account = new com.iceeboot.framework.entity.system.UserDO().setNickname("林宁").setUsername("finance_user");
            when(users.getById(2L)).thenReturn(account);
            cases.review(id,"approve",new Change(1,"确认",null,task));
            assertThat(cases.detail(id).history().getLast().actor()).isEqualTo("林宁");
        }
    }

    @Test void researchersCanFindAndStartThePublishedResearchBudget() {
        jdbc.update("INSERT INTO t_role VALUES(4,'ai_researcher','AI研究员','active',0,0)");
        when(users.getUserRoles(4L)).thenReturn(List.of("ai_researcher"));
        var budget = new ApprovalDesign("research_budget","研发经费审批",fields,design.start(),nodes,"ai_researcher");
        designs.save(new Draft(0,budget));
        designs.publish(budget.code(),1);
        var request = new Submit(budget.code(),1,form("1","2","研发部"));
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(4L);
            assertThat(designs.page(true,1,10,"研发经费").getTotal()).isOne();
            assertThat(cases.submit(request)).isNotBlank();
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(designs.page(true,1,10,"研发经费").getTotal()).isZero();
            assertThatThrownBy(() -> cases.submit(request)).hasMessageContaining("无权");
            var shared = new ApprovalDesign(budget.code(),budget.name(),fields,design.start(),nodes,"ai_researcher",List.of("manager"));
            designs.save(new Draft(2,shared));
            assertThat(designs.designs(true)).isEmpty();
            designs.publish(budget.code(),3);
            assertThat(designs.page(true,1,10,"研发经费").getTotal()).isOne();
            assertThat(cases.submit(new Submit(budget.code(),2,request.values()))).isNotBlank();
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            assertThatThrownBy(() -> designs.detail(budget.code(),true)).isInstanceOf(IceeBootException.class);
            assertThatThrownBy(() -> cases.submit(new Submit(budget.code(),2,request.values()))).hasMessageContaining("无权");
            designs.save(new Draft(4,budget));
            designs.publish(budget.code(),5);
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(designs.page(true,1,10,"研发经费").getTotal()).isZero();
        }
    }

    @Test void stoppingIntakeAndPublishingDoNotRerouteOpenCases() {
        designs.publish("purchase", 1);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            var values = form("1", "2", "甲");
            String oldCase = cases.submit(new Submit("purchase", 1, values));
            designs.setStopped("purchase", 2, true);
            assertThat(designs.designs(true)).isEmpty();
            assertThatThrownBy(() -> cases.submit(new Submit("purchase", 1, values))).hasMessageContaining("停用");
            var v2 = new ApprovalDesign("purchase", "新版采购", fields, new Exit(List.of(), "B"), List.of(nodes.get(1)));
            designs.save(new Draft(3, v2));
            designs.publish("purchase", 4);
            assertThat(designs.page(true, 1, 10).getRecords().getFirst().stopped()).isTrue();
            assertThatThrownBy(() -> cases.submit(new Submit("purchase", 2, values))).hasMessageContaining("停用");
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            cases.review(oldCase, "approve", new Change(1, "按旧版办理", null));
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(cases.detail(oldCase).item().node()).isEqualTo("C");
            cases.review(oldCase, "approve", new Change(2, "通过", null));
            assertThat(cases.detail(oldCase).version()).isEqualTo(1);
            assertThat(cases.detail(oldCase).item().status()).isEqualTo("APPROVED");
            assertThatThrownBy(() -> designs.setStopped("purchase", 2, false)).hasMessageContaining("刷新");
            designs.setStopped("purchase", 5, false);
            assertThatThrownBy(() -> cases.submit(new Submit("purchase", 1, values))).hasMessageContaining("版本");
            String newCase = cases.submit(new Submit("purchase", 2, values));
            assertThat(cases.detail(newCase).item().node()).isEqualTo("B");
            assertThat(cases.detail(newCase).version()).isEqualTo(2);
        }
    }

    @Test void templatePagesCountOnlyVisibleVersionsAndKeepDraftChangesPrivate() {
        designs.publish("purchase", 1);
        designs.save(new Draft(2, new ApprovalDesign("purchase", "新版草稿", fields, design.start(), nodes)));
        designs.save(new Draft(0, countersignDesign()));
        var page = designs.page(false, 2, 1);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords().getFirst().changed()).isTrue();
        assertThat(designs.page(true, 2, 1).getCurrent()).isEqualTo(1);
        assertThat(designs.detail("purchase", true).design().name()).isEqualTo("采购审批");
        assertThatThrownBy(() -> designs.detail("budget", true)).isInstanceOf(IceeBootException.class);
        assertThatThrownBy(() -> designs.page(false, 0, 10)).isInstanceOf(IceeBootException.class);
    }

    @Test void simultaneousReviewersCanRecordOnlyOneDecision() throws Exception {
        designs.publish("purchase", 1);
        String id;
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            id = cases.submit(new Submit("purchase", 1, form("1", "2", "甲")));
        }
        when(users.getUserRoles(4L)).thenReturn(List.of("finance"));
        var ready = new java.util.concurrent.CyclicBarrier(2);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (long user : List.of(2L, 4L)) {
                results.add(pool.submit(() -> {
                    try (var identity = mockStatic(TokenUtils.class)) {
                        identity.when(TokenUtils::getCurrentUserId).thenReturn(user);
                        ready.await(5, java.util.concurrent.TimeUnit.SECONDS);
                        transactions.executeWithoutResult(s -> cases.review(id, "approve", new Change(1, "同意", null)));
                        return true;
                    } catch (IceeBootException e) {
                        assertThat(e.getMessage()).contains("刷新");
                        return false;
                    }
                }));
            }
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(true, false);
        }
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            assertThat(cases.detail(id).history()).hasSize(2);
            assertThat(cases.detail(id).item().node()).isEqualTo("C");
        }
    }

    private ApprovalDesign countersignDesign() {
        return new ApprovalDesign("budget","预算审批",List.of(new Field("price","金额","business","number",true)),
                new Exit(List.of(new Branch(List.of(new Condition("price","5000","ge"),
                        new Condition("price","9007199254740993.01","lt")),"joint")),"single"),
                List.of(new Node("single","主管","manager",end),
                        new Node("joint","联合会签","finance",end,List.of("manager"))));
    }

    @Test void decimalBoundariesAndParallelDecisionsAreDrivenByTheEngine() {
        var d = countersignDesign();
        designs.save(new Draft(0,d));
        designs.publish("budget",1);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            for (String amount : List.of("4999.999","9007199254740993.01")) {
                String id = cases.submit(new Submit("budget",1,Map.of("price",amount)));
                assertThat(cases.detail(id).tasks()).hasSize(1);
                assertThat(cases.detail(id).item().node()).isEqualTo("single");
            }
            String id = cases.submit(new Submit("budget",1,Map.of("price","5000.00")));
            var tasks = cases.detail(id).tasks();
            assertThat(tasks).extracting(t -> t.role()).containsExactly("finance","manager");
            String finance = tasks.getFirst().id(), manager = tasks.getLast().id();
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            assertThat(cases.list("inbox")).extracting(i -> i.id()).contains(id);
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",null,manager))).hasMessageContaining("角色");
            cases.review(id,"approve",new Change(1,"财务同意",null,finance));
            assertThat(cases.detail(id).tasks()).extracting(t -> t.role()).containsExactly("manager");
            assertThat(cases.detail(id).item().status()).isEqualTo("PENDING");
            assertThat(cases.list("inbox")).extracting(i -> i.id()).doesNotContain(id);
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",null,finance))).isInstanceOf(IceeBootException.class);
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            cases.review(id,"approve",new Change(1,"主管同意",null,manager));
            assertThat(cases.detail(id).item().status()).isEqualTo("APPROVED");
            assertThat(cases.detail(id).history()).hasSize(3);
        }
    }

    @Test void simultaneousCountersignersAdvanceExactlyOnce() throws Exception {
        var d = countersignDesign();
        designs.save(new Draft(0,d));
        designs.publish("budget",1);
        String id;
        List<com.iceeboot.framework.vo.workflow.ApprovalViews.PendingTask> tasks;
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            id = cases.submit(new Submit("budget",1,Map.of("price","5000")));
            tasks = cases.detail(id).tasks();
        }
        var ready = new java.util.concurrent.CyclicBarrier(2);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = new ArrayList<java.util.concurrent.Future<Void>>();
            for (var task : tasks) {
                results.add(pool.submit(() -> {
                    try (var identity = mockStatic(TokenUtils.class)) {
                        identity.when(TokenUtils::getCurrentUserId).thenReturn(task.role().equals("finance") ? 2L : 3L);
                        ready.await(5,java.util.concurrent.TimeUnit.SECONDS);
                        transactions.executeWithoutResult(tx -> cases.review(id,"approve",new Change(1,"同意",null,task.id())));
                    }
                    return null;
                }));
            }
            for (var result : results) result.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            var detail = cases.detail(id);
            assertThat(detail.item().status()).isEqualTo("APPROVED");
            assertThat(detail.tasks()).isEmpty();
            assertThat(detail.history()).hasSize(3);
        }
    }

    @Test void rejectionCancelsSiblingTasksAndResubmissionPreservesEvidence() {
        var d = countersignDesign();
        designs.save(new Draft(0,d));
        designs.publish("budget",1);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            String id = cases.submit(new Submit("budget",1,Map.of("price","7000")));
            var tasks = cases.detail(id).tasks();
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            cases.review(id,"reject",new Change(1,"缺少凭证",null,tasks.getFirst().id()));
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(cases.list("inbox")).isEmpty();
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",null,tasks.getLast().id()))).isInstanceOf(IceeBootException.class);
            identity.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            cases.revise(id,"resubmit",new Change(2,"",Map.of("price","7000.01")));
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",null,tasks.getLast().id()))).isInstanceOf(IceeBootException.class);
            var detail = cases.detail(id);
            assertThat(detail.tasks()).hasSize(2);
            assertThat(detail.history().get(1).reason()).isEqualTo("缺少凭证");
            assertThat(detail.history().get(1).role()).isEqualTo("finance");
            assertThat(detail.history().get(0).values()).containsEntry("price","7000");
            assertThat(detail.history().get(2).round()).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_workflow_engine_round WHERE application_id=?",Integer.class,id)).isEqualTo(2);
        }
    }

    @Test void oldPublishedVersionsAndInFlightCasesSurviveUpgrade() {
        jdbc.update("UPDATE t_workflow_blueprint SET version=1,published=draft,revision=2 WHERE code='purchase'");
        var values = form("1","2","迁移前");
        jdbc.update("INSERT INTO t_workflow_application(id,blueprint_code,version,blueprint,title,owner_id,status,round_no,revision,node_id,role_code,form_data) VALUES(?,?,?,?,?,1,'PENDING',1,1,'A','finance',?)",
                "old","purchase",1,designs.write(design),"历史采购",designs.write(values));
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            assertThat(cases.list("inbox")).hasSize(1);
            cases.review("old","approve",new Change(1,"同意",null));
            identity.when(TokenUtils::getCurrentUserId).thenReturn(3L);
            assertThat(cases.detail("old").item().node()).isEqualTo("C");
            cases.review("old","approve",new Change(2,"完成",null));
            String fresh = cases.submit(new Submit("purchase",1,values));
            assertThat(cases.detail(fresh).tasks()).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_workflow_engine_round",Integer.class)).isEqualTo(1);
        }
    }

    @Test void engineAndAuditShareTransactionAndAdvancedEndpointsCannotAlterApprovals() {
        designs.publish("purchase",1);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            String id = cases.submit(new Submit("purchase",1,form("1","2","甲")));
            String instance = jdbc.queryForObject("SELECT instance_id FROM t_workflow_engine_round WHERE application_id=?",String.class,id);
            var raw = new com.iceeboot.framework.service.workflow.impl.WorkflowServiceImpl(processEngine.getRuntimeService(),
                    processEngine.getTaskService(),processEngine.getHistoryService(),processEngine.getRepositoryService(),users);
            assertThat(raw.getDefinitions(null)).isEmpty();
            assertThatThrownBy(() -> raw.deleteProcessInstance(instance,"")).isInstanceOf(IceeBootException.class);
            String task = cases.detail(id).tasks().getFirst().id();
            transactions.executeWithoutResult(tx -> {
                cases.review(id,"approve",new Change(1,"回滚",null,task));
                tx.setRollbackOnly();
            });
            assertThat(cases.detail(id).tasks().getFirst().id()).isEqualTo(task);
            assertThat(cases.detail(id).history()).hasSize(1);
        }
    }

    @Test void voidIsTerminalAndActiveFormsCannotBeEdited() {
        designs.publish("purchase",1);
        try (var identity = mockStatic(TokenUtils.class)) {
            identity.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            var values = form("1","2","甲");
            String id = cases.submit(new Submit("purchase",1,values));
            assertThatThrownBy(() -> cases.review(id,"approve",new Change(1,"",values))).hasMessageContaining("不能修改");
            assertThatThrownBy(() -> cases.revise(id,"resubmit",new Change(1,"",values))).isInstanceOf(IceeBootException.class);
            cases.review(id,"reject",new Change(1,"重复提交",null));
            cases.revise(id,"void",new Change(2,"",null));
            assertThat(cases.detail(id).item().status()).isEqualTo("VOID");
            assertThat(cases.detail(id).history()).hasSize(3);
            assertThatThrownBy(() -> cases.revise(id,"resubmit",new Change(3,"",values))).isInstanceOf(IceeBootException.class);
        }
    }
}
