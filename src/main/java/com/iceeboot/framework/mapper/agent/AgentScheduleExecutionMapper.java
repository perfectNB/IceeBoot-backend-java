package com.iceeboot.framework.mapper.agent;

import com.iceeboot.framework.entity.agent.AgentScheduleExecutionDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentScheduleExecutionMapper {
    @Insert("""
            INSERT INTO t_agent_schedule_execution(id,schedule_id,owner_id,agent_code,input,
                scheduled_at,started_at,finished_at,status,message,session_id)
            VALUES(#{id},#{scheduleId},#{ownerId},#{agentCode},#{input},#{scheduledAt},#{startedAt},
                #{finishedAt},#{status},#{message},#{sessionId})
            """)
    int insert(AgentScheduleExecutionDO row);

    @Select("SELECT * FROM t_agent_schedule_execution WHERE schedule_id=#{schedule} AND owner_id=#{owner} ORDER BY scheduled_at DESC,id DESC LIMIT 50 OFFSET #{offset}")
    List<AgentScheduleExecutionDO> list(@Param("schedule") long schedule, @Param("owner") long owner, @Param("offset") int offset);

    @Update("UPDATE t_agent_schedule_execution SET run_id=#{run} WHERE id=#{id} AND status='RUNNING' AND run_id IS NULL")
    int bindRun(@Param("id") String id, @Param("run") String run);

    @Update("UPDATE t_agent_schedule_execution SET status=#{status},message=#{message},finished_at=#{finishedAt} WHERE id=#{id} AND status='RUNNING'")
    int finish(AgentScheduleExecutionDO row);
}
