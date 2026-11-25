package com.iceeboot.framework.mapper.agent;

import com.iceeboot.framework.entity.agent.*;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** 轮换专用 Mapper：覆盖历史记录，持有行锁，禁止写入无关业务字段。 */
@Mapper
public interface AgentSecretMapper {
    @Select("SELECT id,api_key FROM t_agent_model ORDER BY id FOR UPDATE")
    List<AgentModelDO> lockModelSecrets();

    @Update("UPDATE t_agent_model SET api_key=#{value} WHERE id=#{id}")
    int updateModelSecret(@Param("id") Long id, @Param("value") String value);

    @Select("SELECT id,headers_json FROM t_agent_tool ORDER BY id FOR UPDATE")
    List<AgentToolDO> lockToolSecrets();

    @Update("UPDATE t_agent_tool SET headers_json=#{value} WHERE id=#{id}")
    int updateToolSecret(@Param("id") Long id, @Param("value") String value);

    @Select("SELECT id,headers_json FROM t_agent_mcp ORDER BY id FOR UPDATE")
    List<AgentMcpDO> lockMcpSecrets();

    @Update("UPDATE t_agent_mcp SET headers_json=#{value} WHERE id=#{id}")
    int updateMcpSecret(@Param("id") Long id, @Param("value") String value);

    @Select("SELECT id,headers_json FROM t_agent_knowledge ORDER BY id FOR UPDATE")
    List<AgentKnowledgeDO> lockKnowledgeSecrets();

    @Update("UPDATE t_agent_knowledge SET headers_json=#{value} WHERE id=#{id}")
    int updateKnowledgeSecret(@Param("id") Long id, @Param("value") String value);

    @Select("SELECT id,environment_json FROM t_agent_definition ORDER BY id FOR UPDATE")
    List<AgentDefinitionDO> lockDefinitionSecrets();

    @Update("UPDATE t_agent_definition SET environment_json=#{value} WHERE id=#{id}")
    int updateDefinitionSecret(@Param("id") Long id, @Param("value") String value);

    @Select("SELECT id,snapshot_json FROM t_agent_release ORDER BY id FOR UPDATE")
    List<AgentReleaseDO> lockReleaseSecrets();

    @Update("UPDATE t_agent_release SET snapshot_json=#{value} WHERE id=#{id}")
    int updateReleaseSecret(@Param("id") String id, @Param("value") String value);

    @Select("SELECT id,profile_ciphertext FROM t_agent_memory_profile WHERE id>#{after} ORDER BY id LIMIT 100 FOR UPDATE")
    List<MemoryProfileDO> lockMemoryProfiles(long after);

    @Update("UPDATE t_agent_memory_profile SET profile_ciphertext=#{profileCiphertext} WHERE id=#{id}")
    void rotateProfile(MemoryProfileDO row);

    @Select("SELECT id,policy_snapshot_ciphertext,request_ciphertext FROM t_agent_memory_update_job WHERE id>#{after} ORDER BY id LIMIT 100 FOR UPDATE")
    List<MemoryJobDO> lockMemoryJobs(long after);

    @Update("UPDATE t_agent_memory_update_job SET policy_snapshot_ciphertext=#{policySnapshotCiphertext},request_ciphertext=#{requestCiphertext} WHERE id=#{id}")
    void rotateJob(MemoryJobDO row);

    @Select("SELECT id,patch_ciphertext,evidence_ciphertext FROM t_agent_memory_change WHERE id>#{after} ORDER BY id LIMIT 100 FOR UPDATE")
    List<MemoryChangeDO> lockMemoryChanges(long after);

    @Update("UPDATE t_agent_memory_change SET patch_ciphertext=#{patchCiphertext},evidence_ciphertext=#{evidenceCiphertext} WHERE id=#{id}")
    void rotateChange(MemoryChangeDO row);
}
