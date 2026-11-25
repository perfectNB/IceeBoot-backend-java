package com.iceeboot.framework.mapper.agent;

import com.iceeboot.framework.entity.agent.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentMemoryMapper {
    @Select("SELECT * FROM t_agent_memory_policy WHERE scope=#{scope} AND agent_id=#{agent}")
    MemoryPolicyDO policy(String scope, long agent);

    @Select("SELECT revision FROM t_agent_memory_policy WHERE scope=#{scope} AND agent_id=#{agent} FOR UPDATE")
    Long lockPolicy(String scope, long agent);

    @Select("SELECT revision FROM t_agent_memory_grant WHERE user_id=#{user} AND agent_id=#{agent} FOR UPDATE")
    Long lockGrant(String user, long agent);

    @Insert("""
            INSERT INTO t_agent_memory_policy(scope,agent_id,prompt,profile_schema,model_config_id,enabled,review_required)
            VALUES(#{scope},#{agentId},#{prompt},#{profileSchema},#{modelConfigId},#{enabled},#{reviewRequired})
            """)
    void insertPolicy(MemoryPolicyDO policy);

    @Update("""
            UPDATE t_agent_memory_policy SET prompt=#{prompt},profile_schema=#{profileSchema},enabled=#{enabled},
            review_required=#{reviewRequired},model_config_id=#{modelConfigId},revision=revision+1 WHERE scope=#{scope} AND agent_id=#{agentId} AND revision=#{revision}
            """)
    int updatePolicy(MemoryPolicyDO policy);

    @Select("SELECT * FROM t_agent_memory_profile WHERE scope=#{scope} AND agent_id=#{agent} AND user_id=#{user}")
    MemoryProfileDO profile(String scope, long agent, String user);

    @Select("SELECT * FROM t_agent_memory_profile WHERE scope=#{scope} AND agent_id=#{agent} AND user_id=#{user} FOR UPDATE")
    MemoryProfileDO current(String scope, long agent, String user);

    @Select("SELECT * FROM t_agent_memory_profile WHERE id=#{id} FOR UPDATE")
    MemoryProfileDO lockProfile(long id);

    @Insert("""
            INSERT INTO t_agent_memory_profile(scope,agent_id,user_id,profile_ciphertext)
            VALUES(#{scope},#{agent},#{user},#{document}) ON DUPLICATE KEY UPDATE id=id
            """)
    void ensureProfile(String scope, long agent, String user, String document);

    @Update("""
            UPDATE t_agent_memory_profile SET profile_ciphertext=#{document},revision=revision+1,
            forget_epoch=forget_epoch+#{forgot} WHERE id=#{id} AND revision=#{revision}
            """)
    int replace(long id, long revision, String document, int forgot);

    @Select("SELECT * FROM t_agent_memory_grant WHERE user_id=#{user} AND agent_id=#{agent}")
    MemoryGrantDO grant(String user, long agent);

    @Insert("""
            INSERT INTO t_agent_memory_grant(user_id,agent_id,can_read,can_propose_update,read_fields,write_fields)
            VALUES(#{user},#{agent},#{row.canRead},#{row.canProposeUpdate},#{row.readFields},#{row.writeFields})
            """)
    void insertGrant(String user, long agent, MemoryGrantDO row);

    @Update("""
            UPDATE t_agent_memory_grant SET can_read=#{row.canRead},can_propose_update=#{row.canProposeUpdate},
            read_fields=#{row.readFields},write_fields=#{row.writeFields},revision=revision+1
            WHERE user_id=#{user} AND agent_id=#{agent} AND revision=#{row.revision}
            """)
    int updateGrant(String user, long agent, MemoryGrantDO row);

    @Insert("""
            INSERT INTO t_agent_memory_change(profile_id,job_id,base_revision,base_forget_epoch,origin,status,
            patch_ciphertext,evidence_ciphertext,actor_user_id) VALUES(#{row.profileId},#{row.jobId},#{row.baseRevision},
            #{row.baseForgetEpoch},#{origin},#{row.status},#{row.patchCiphertext},#{row.evidenceCiphertext},#{actor})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    void insertChange(MemoryChangeDO row, String origin, String actor);

    @Update("""
            UPDATE t_agent_memory_change SET status=#{status},applied_revision=#{revision},reviewer_user_id=#{actor},
            reviewed_at=#{now} WHERE id=#{id} AND status='PENDING'
            """)
    int decide(long id, String status, long revision, String actor, long now);

    @Update("UPDATE t_agent_memory_change SET status='STALE' WHERE profile_id=#{profile} AND status='PENDING' AND base_revision<=#{revision}")
    void expirePending(long profile, long revision);

    @Select("SELECT * FROM t_agent_memory_change WHERE profile_id=#{profile} AND status='PENDING' ORDER BY id LIMIT 50")
    List<MemoryChangeDO> pending(long profile);

    @Select("SELECT * FROM t_agent_memory_change WHERE id=#{id} AND profile_id=#{profile} FOR UPDATE")
    MemoryChangeDO lockChange(long profile, long id);
}
