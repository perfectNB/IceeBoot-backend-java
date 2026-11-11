package com.iceeboot.framework.service.agent;
import com.iceeboot.framework.entity.agent.*;
import com.iceeboot.framework.model.agent.*;
import io.agentscope.core.session.SessionLogStore;
import java.util.List;
public interface IAgentConversationService {
    AgentSessionDO open(long agent, String user, String session, PublishedAgent release, String title);
    AgentSessionDO require(long agent, String user, String session);
    List<AgentSessionDO> sessions(long agent, String user, int offset);
    List<AgentMessageDO> messages(long agent, String user, String session, long before);
    void begin(AgentSessionDO session, String run, String input);
    void finish(AgentSessionDO session, String run, String output, String status);
    void release(AgentSessionDO session, String run);
    SessionLogStore logStore();
}
