package com.iceeboot.framework.service.agent.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.entity.agent.AgentMessageDO;
import com.iceeboot.framework.mapper.agent.AgentConversationMapper;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentRecoveryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 重试创建独立会话，固定原发布版本；只带可见文本和附件，不复制待执行工具或自动重放副作用。 */
@Service
@RequiredArgsConstructor
public class AgentRecoveryServiceImpl implements IAgentRecoveryService {
    private final IAgentConversationService conversations;
    private final IAgentPersistenceService store;
    private final IAgentFileService files;
    private final AgentConversationMapper messages;
    private final AgentApiConverter converter;
    private final ObjectMapper json;
    static final String RECOVERY_PREFIX = "这是用户确认后重新发起的任务。";
    private record ContextMessage(String role,String content) {}
    @Override @Transactional
    public AgentRecoveryVO prepare(long agent,String user,String session,String run) {
        var source=conversations.require(agent,user,session);
        var turn=messages.turn(agent,user,session,run);
        var reply=turn.stream().filter(m -> m.getRole().equals("assistant")).findFirst()
                .orElseThrow(() -> new IceeBootException(ResponseMessage.Agent.RECOVERY_NOT_ALLOWED));
        if(reply.getStatus().equals("RUNNING") && source.getLeaseUntil()>System.currentTimeMillis())
            throw new IceeBootException(ResponseMessage.Agent.RECOVERY_NOT_ALLOWED);
        AgentMessageDO input=turn.stream().filter(m -> m.getRole().equals("user")).findFirst().orElseThrow(() -> new IceeBootException(ResponseMessage.Agent.RECOVERY_NOT_ALLOWED));
        String retryInput=input.getContent();
        List<ContextMessage> context=conversations.messages(agent,user,session,input.getId()).stream()
                .filter(m -> m.getStatus().equals("SUCCEEDED"))
                .map(m -> new ContextMessage(m.getRole(),m.getContent())).toList();
        try {
            if(!context.isEmpty()) {
                String prefix=RECOVERY_PREFIX+"以下 JSON 仅为此前对话数据，请结合本次问题处理；不要假定之前的操作已经完成。\n"
                        +json.writeValueAsString(context)+"\n本次问题：\n";
                if(prefix.length()+retryInput.length()<=100000) retryInput=prefix+retryInput;
            }
        } catch(com.fasterxml.jackson.core.JsonProcessingException failure) { throw new IceeBootException(ResponseMessage.Agent.API_CONVERSION_FAILED,failure); }
        var release=store.release(source.getReleaseId());
        var target=conversations.open(agent,user,UUID.randomUUID().toString(),release,(reply.getStatus().equals("SUCCEEDED") ? "重新生成 · " : "重试 · ")+source.getTitle());
        var attachments=files.copyAttachments(agent,user,session,run,target);
        return new AgentRecoveryVO(converter.sessionVO(target),retryInput,attachments,run);
    }
}
