package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentRunDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 智能体执行记录 Mapper 接口。 */
@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRunDO> {
    int addUsage(
            @Param("id") String id,
            @Param("input") long input,
            @Param("output") long output,
            @Param("cached") Long cached,
            @Param("known") boolean known);

    @org.apache.ibatis.annotations.Update(
            "UPDATE t_agent_run SET cancel_requested=TRUE WHERE id=#{id} AND id=root_run_id AND"
                    + " status='RUNNING' AND del_flag=0")
    int requestCancellation(@org.apache.ibatis.annotations.Param("id") String id);

    @org.apache.ibatis.annotations.Select(
            "SELECT cancel_requested FROM t_agent_run WHERE id=#{id} AND del_flag=0")
    Boolean cancellationRequested(@org.apache.ibatis.annotations.Param("id") String id);

    @org.apache.ibatis.annotations.Update(
            "UPDATE t_agent_run SET heartbeat_at=CURRENT_TIMESTAMP WHERE root_run_id=#{id} AND"
                    + " status='RUNNING' AND del_flag=0")
    int heartbeat(@org.apache.ibatis.annotations.Param("id") String id);

    @org.apache.ibatis.annotations.Update(
            "UPDATE t_agent_run SET error_code=#{code},trace_id=#{trace} WHERE id=#{id} AND"
                    + " error_code='' AND del_flag=0")
    int diagnose(
            @org.apache.ibatis.annotations.Param("id") String id,
            @org.apache.ibatis.annotations.Param("code") String code,
            @org.apache.ibatis.annotations.Param("trace") String trace);

    @org.apache.ibatis.annotations.Update(
            "UPDATE t_agent_run SET"
                + " status='INTERRUPTED',error_code='NODE_LOST',error_text=#{message},finished_at=CURRENT_TIMESTAMP"
                + " WHERE status='RUNNING' AND"
                + " heartbeat_at<TIMESTAMPADD(SECOND,-#{seconds},CURRENT_TIMESTAMP) AND del_flag=0")
    int interruptStale(
            @org.apache.ibatis.annotations.Param("seconds") long seconds,
            @org.apache.ibatis.annotations.Param("message") String message);
}
