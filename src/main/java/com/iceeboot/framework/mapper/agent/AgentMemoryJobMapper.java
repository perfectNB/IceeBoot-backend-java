package com.iceeboot.framework.mapper.agent;

import com.iceeboot.framework.entity.agent.MemoryJobDO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AgentMemoryJobMapper {
    @Insert("""
            INSERT INTO t_agent_memory_update_job(source_run_id,trigger_type,source_agent_id,source_user_id,
            source_session_id,source_release_id,policy_snapshot_ciphertext,request_ciphertext,status)
            VALUES(#{row.sourceRunId},#{trigger},#{row.sourceAgentId},#{row.sourceUserId},#{row.sourceSessionId},
            #{row.sourceReleaseId},#{row.policySnapshotCiphertext},#{row.requestCiphertext},#{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    void insert(MemoryJobDO row, String trigger, String status);

    String READY = "((status IN ('PENDING','RETRY') AND next_retry_at<=#{now}) OR (status='RUNNING' AND lease_until<=#{now}))";

    @Select("SELECT id FROM t_agent_memory_update_job WHERE " + READY + " ORDER BY id LIMIT 1")
    Long next(long now);

    @Update("UPDATE t_agent_memory_update_job SET status='RUNNING',lease_epoch=lease_epoch+1,attempts=attempts+1,"
            + "lease_owner=#{worker},execution_run_id=#{run},lease_until=#{until} WHERE id=#{id} AND " + READY)
    int claim(long id, long now, long until, String worker, String run);

    @Select("SELECT * FROM t_agent_memory_update_job WHERE id=#{id}")
    MemoryJobDO get(long id);

    @Select("SELECT COUNT(*) FROM t_agent_memory_update_job WHERE source_run_id=#{run}")
    int exists(String run);

    @Select("SELECT * FROM t_agent_memory_update_job WHERE id=#{id} AND status='RUNNING' AND lease_epoch=#{epoch} FOR UPDATE")
    MemoryJobDO locked(long id, long epoch);

    @Update("""
            UPDATE t_agent_memory_update_job SET status=#{status},next_retry_at=#{retry},lease_until=0,error_code=#{error}
            WHERE id=#{id} AND status='RUNNING' AND lease_epoch=#{epoch}
            """)
    int finish(long id, long epoch, String status, long retry, String error);
}
