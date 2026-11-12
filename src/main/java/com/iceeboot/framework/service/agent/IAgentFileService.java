package com.iceeboot.framework.service.agent;
import com.iceeboot.framework.entity.agent.AgentSessionDO;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.vo.agent.AgentFileVO;
import java.util.List;
public interface IAgentFileService {
    long MAX_BYTES=20L*1024*1024;
    AgentFileVO upload(long agent,String user,String session,String name,byte[] content);
    AgentFileContent download(long agent,String user,String session,String id);
    List<AgentFileVO> list(long agent,String user,String session,String run);
    void delete(long agent,String user,String session,String id);
    List<AgentFileContent> bind(long agent,String user,String session,String run,List<String> ids);
    AgentFileVO artifact(long agent,String user,String session,String run,String name,byte[] content);
    List<AgentFileVO> copyAttachments(long agent,String user,String session,String run,AgentSessionDO target);
}
