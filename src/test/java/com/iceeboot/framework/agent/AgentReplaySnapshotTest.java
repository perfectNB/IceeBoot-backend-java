package com.iceeboot.framework.agent;

import com.iceeboot.framework.mapper.agent.AgentConversationMapper;
import com.iceeboot.framework.entity.agent.AgentSessionStateDO;
import com.iceeboot.framework.service.agent.runtime.CachedJournalSessionLog;
import io.agentscope.core.session.*;
import io.agentscope.core.state.AgentState;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** AgentFixture 每次创建随机命名的 H2 内存库；本测试不使用应用配置和外部数据库。 */
class AgentReplaySnapshotTest {
    private SessionEvent checkpoint(long seq, String text) {
        String state = AgentState.builder().userId("u").sessionId("s").summary(text).build().toJson();
        String payload = io.agentscope.core.util.JsonUtils.getJsonCodec().toJson(Map.of("stateJson",state));
        return new SessionEvent(1,UUID.randomUUID().toString(),seq,System.currentTimeMillis(),"state/checkpoint","run","turn",true,payload);
    }
    private CachedJournalSessionLog log(AgentConversationMapper mapper, long agent, String user, String session) {
        return new CachedJournalSessionLog(mapper,agent,user,session,null);
    }
    @Test void freshInstanceRestoresIdenticalProjectionWithFewReadsAndIncrementalCommits() throws Exception {
        var f = new AgentFixture();
        var mapper = mock(AgentConversationMapper.class, org.mockito.AdditionalAnswers.delegatesTo(f.conversationMapper()));
        var first = log(mapper,1,"u","s");
        var writer = first.acquire("first",Duration.ofSeconds(30));
        for (int i=1;i<=20;i++) first.commit(writer,"batch"+i,i-1,List.of(checkpoint(i,"history".repeat(1000)+i)));
        first.release(writer);
        var expected = SessionProjection.read(first);
        clearInvocations(mapper);
        var second = log(mapper,1,"u","s");
        assertThat(SessionProjection.read(second)).isEqualTo(expected);
        verify(mapper,atMost(6)).state(eq(1L),eq("u"),eq("s"),anyString());
        long packed = f.jdbc.queryForObject("SELECT OCTET_LENGTH(payload) FROM t_agent_session_state WHERE agent_id=1 AND user_id='u' AND session_id='s' AND storage_path='iceeboot/replay-cache-v1'",Long.class);
        assertThat(packed).isLessThan(15000);
        var next = second.acquire("second",Duration.ofSeconds(30));
        second.commit(next,"next",20,List.of(checkpoint(21,"latest")));
        second.release(next);
        assertThat(SessionProjection.read(first).restore().getSummary()).isEqualTo("latest");
        assertThat(SessionProjection.read(log(mapper,1,"other","s")).stateJson()).isNull();
        assertThat(SessionProjection.read(log(mapper,2,"u","s")).stateJson()).isNull();
        assertThat(SessionProjection.read(log(mapper,1,"u","other")).stateJson()).isNull();
    }
    @Test void damagedDerivedCacheFallsBackAndNeverBypassesWriterFencing() throws Exception {
        var f = new AgentFixture();
        var mapper = mock(AgentConversationMapper.class, org.mockito.AdditionalAnswers.delegatesTo(f.conversationMapper()));
        var first = log(mapper,1,"u","s");var writer=first.acquire("first",Duration.ofSeconds(30));
        first.commit(writer,"one",0,List.of(checkpoint(1,"intact")));first.release(writer);
        var corrupt = new AgentSessionStateDO(); corrupt.setRevision(1L);corrupt.setPayload(new byte[]{1,2,3});
        doReturn(corrupt).when(mapper).state(1L,"u","s","iceeboot/replay-cache-v1");
        var fresh=log(mapper,1,"u","s");
        assertThat(SessionProjection.read(fresh).restore().getSummary()).isEqualTo("intact");
        var fenced=first.acquire("old",Duration.ofSeconds(30));
        fresh.sealWriter("old");
        assertThatThrownBy(()->first.commit(fenced,"denied",1,List.of(checkpoint(2,"bad")))).isInstanceOf(SessionLogException.class);
        assertThat(SessionProjection.read(fresh).asOfSeq()).isEqualTo(1);
    }
}
