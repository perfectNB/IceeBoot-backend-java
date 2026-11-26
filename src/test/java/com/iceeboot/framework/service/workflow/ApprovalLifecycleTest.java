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
        var ds = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ClassPathResource("db/agent/V025__approval_workflow.sql")).execute(ds);
        transactions = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
        var jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE t_role(id INT, code VARCHAR(20), name VARCHAR(20),status VARCHAR(10),sort INT,del_flag INT)");
        jdbc.update("INSERT INTO t_role VALUES(1,'finance','财务','active',0,0),(2,'manager','经理','active',0,0)");
        var json = new ObjectMapper();
        designs = new ApprovalDesignService(jdbc,json);
        cases = new ApprovalCaseService(jdbc,designs,users,json);
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
