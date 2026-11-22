package com.iceeboot.framework.mapper.agent;

import com.iceeboot.framework.entity.agent.AgentScheduleDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentScheduleMapper {
    @Select("SELECT * FROM t_agent_schedule WHERE owner_id=#{owner} AND deleted=FALSE ORDER BY id DESC LIMIT 500")
    List<AgentScheduleDO> list(long owner);

    @Select("SELECT * FROM t_agent_schedule WHERE id=#{id} FOR UPDATE")
    AgentScheduleDO lock(long id);

    @Select("SELECT * FROM t_agent_schedule WHERE id=#{id} AND owner_id=#{owner} AND deleted=FALSE")
    AgentScheduleDO owned(@Param("id") long id, @Param("owner") long owner);

    @Insert("""
            INSERT INTO t_agent_schedule(owner_id,agent_code,name,input,schedule_type,cron_expression,
                zone_id,execute_at,next_fire_at,enabled)
            VALUES(#{ownerId},#{agentCode},#{name},#{input},#{scheduleType},#{cronExpression},
                #{zoneId},#{executeAt},#{nextFireAt},#{enabled})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentScheduleDO row);

    @Update("""
            UPDATE t_agent_schedule SET name=#{name},agent_code=#{agentCode},input=#{input},
                schedule_type=#{scheduleType},cron_expression=#{cronExpression},zone_id=#{zoneId},
                execute_at=#{executeAt},next_fire_at=#{nextFireAt},enabled=#{enabled},deleted=#{deleted},
                running_id=#{runningId},lease_until=#{leaseUntil},update_time=CURRENT_TIMESTAMP WHERE id=#{id}
            """)
    int update(AgentScheduleDO row);

    @Select("SELECT id FROM t_agent_schedule WHERE enabled=TRUE AND deleted=FALSE AND next_fire_at<=#{now} ORDER BY next_fire_at,id LIMIT 100")
    List<Long> due(long now);

    @Select("SELECT id FROM t_agent_schedule WHERE running_id IS NOT NULL AND lease_until<=#{now} ORDER BY lease_until,id LIMIT 100")
    List<Long> expired(long now);
}
