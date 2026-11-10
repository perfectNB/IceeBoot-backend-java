package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentUsageQuotaDO;

import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface AgentUsageQuotaMapper extends BaseMapper<AgentUsageQuotaDO> {
    @Insert(
            "INSERT INTO t_agent_usage_quota(user_id,usage_day,used_requests)"
                + " VALUES(#{user},#{day},0) ON DUPLICATE KEY UPDATE used_requests=used_requests")
    int ensureDay(@Param("user") String user, @Param("day") LocalDate day);

    @Update(
            "UPDATE t_agent_usage_quota SET"
                    + " used_requests=used_requests+1,update_time=CURRENT_TIMESTAMP WHERE"
                    + " user_id=#{user} AND usage_day=#{day} AND (#{limit}=0 OR"
                    + " used_requests<#{limit})")
    int acquire(@Param("user") String user, @Param("day") LocalDate day, @Param("limit") int limit);

    @Select(
            "SELECT used_requests FROM t_agent_usage_quota WHERE user_id=#{user} AND"
                    + " usage_day=#{day}")
    Long used(@Param("user") String user, @Param("day") LocalDate day);
}
