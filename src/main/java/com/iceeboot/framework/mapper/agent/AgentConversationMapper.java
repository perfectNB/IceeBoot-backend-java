package com.iceeboot.framework.mapper.agent;

import com.iceeboot.framework.entity.agent.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 按会话归属访问消息与 SDK 原子状态。 */
@Mapper
public interface AgentConversationMapper {
    @Select("""
            SELECT * FROM t_agent_session WHERE agent_id=#{agent} AND user_id=#{user} AND session_id=#{session}
            """)
    AgentSessionDO session(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session);

    @Insert("""
            INSERT INTO t_agent_session(agent_id,user_id,session_id,release_id,version,title)
            VALUES(#{agentId},#{userId},#{sessionId},#{releaseId},#{version},#{title})
            """)
    int insertSession(AgentSessionDO row);

    @Select("""
            SELECT * FROM t_agent_session WHERE agent_id=#{agent} AND user_id=#{user} ORDER BY update_time
            DESC,session_id LIMIT 50 OFFSET #{offset}
            """)
    List<AgentSessionDO> sessions(@Param("agent") long agent,
            @Param("user") String user,
            @Param("offset") int offset);

    @Update("""
            UPDATE t_agent_session SET owner=#{owner},lease_until=#{until},title=CASE WHEN title='新对话' THEN #{title}
            ELSE title END,update_time=CURRENT_TIMESTAMP WHERE agent_id=#{agent} AND user_id=#{user} AND
            session_id=#{session} AND (owner='' OR lease_until<#{now})
            """)
    int acquire(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("owner") String owner,
            @Param("now") long now,
            @Param("until") long until,
            @Param("title") String title);

    @Update("""
            UPDATE t_agent_session SET owner='',lease_until=0,update_time=CURRENT_TIMESTAMP WHERE agent_id=#{agent}
            AND user_id=#{user} AND session_id=#{session} AND owner=#{owner}
            """)
    int release(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("owner") String owner);

    @Insert("""
            INSERT INTO t_agent_message(agent_id,user_id,session_id,run_id,role,content,status)
            VALUES(#{agentId},#{userId},#{sessionId},#{runId},#{role},#{content},#{status})
            """)
    int insertMessage(AgentMessageDO row);

    @Update("""
            UPDATE t_agent_message SET content=#{content},status=#{status} WHERE agent_id=#{agent} AND
            user_id=#{user} AND session_id=#{session} AND run_id=#{run} AND role='assistant'
            """)
    int finish(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("run") String run,
            @Param("content") String content,
            @Param("status") String status);

    @Select("""
            SELECT m.id,m.agent_id,m.user_id,m.session_id,m.run_id,m.role,m.content,m.create_time,CASE WHEN
            m.status='RUNNING' AND s.lease_until<#{now} THEN 'INTERRUPTED' ELSE m.status END AS status FROM
            t_agent_message m JOIN t_agent_session s ON s.agent_id=m.agent_id AND s.user_id=m.user_id AND
            s.session_id=m.session_id WHERE m.agent_id=#{agent} AND m.user_id=#{user} AND m.session_id=#{session} AND
            m.id<#{before} ORDER BY m.id DESC LIMIT 100
            """)
    List<AgentMessageDO> messages(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("before") long before,
            @Param("now") long now);

    @Select("""
            SELECT revision,payload FROM t_agent_session_state WHERE agent_id=#{agent} AND user_id=#{user} AND
            session_id=#{session} AND storage_path=#{path}
            """)
    AgentSessionStateDO state(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("path") String path);

    @Insert("""
            INSERT INTO t_agent_session_state(agent_id,user_id,session_id,storage_path,revision,payload)
            VALUES(#{agent},#{user},#{session},#{path},1,#{payload})
            """)
    int insertState(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("path") String path,
            @Param("payload") byte[] payload);

    @Update("""
            UPDATE t_agent_session_state SET revision=revision+1,payload=#{payload} WHERE agent_id=#{agent} AND
            user_id=#{user} AND session_id=#{session} AND storage_path=#{path} AND revision=#{revision}
            """)
    int updateState(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session,
            @Param("path") String path,
            @Param("revision") long revision,
            @Param("payload") byte[] payload);

    @Select("""
            SELECT storage_path FROM t_agent_session_state WHERE agent_id=#{agent} AND user_id=#{user} AND
            session_id=#{session}
            """)
    List<String> paths(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session);

    @Update("""
            UPDATE t_agent_message SET status='INTERRUPTED' WHERE agent_id=#{agent} AND user_id=#{user} AND
            session_id=#{session} AND role='assistant' AND status='RUNNING'
            """)
    int interruptPending(@Param("agent") long agent,
            @Param("user") String user,
            @Param("session") String session);
}
