package com.iceeboot.framework.mapper.agent;
import com.iceeboot.framework.entity.agent.AgentFileDO;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface AgentFileMapper {
    @Insert("INSERT INTO t_agent_file(id,agent_id,user_id,session_id,kind,name,size_bytes,sha256) VALUES(#{id},#{agentId},#{userId},#{sessionId},#{kind},#{name},#{sizeBytes},#{sha256})")
    int insert(AgentFileDO row);
    @Insert("INSERT INTO t_agent_file_chunk(file_id,part_no,payload) VALUES(#{id},#{part},#{payload})")
    int chunk(@Param("id") String id,@Param("part") int part,@Param("payload") byte[] payload);
    @Select("SELECT payload FROM t_agent_file_chunk WHERE file_id=#{id} ORDER BY part_no")
    List<byte[]> chunks(String id);
    @Select("SELECT * FROM t_agent_file WHERE agent_id=#{agent} AND user_id=#{user} AND session_id=#{session} AND id=#{id}")
    AgentFileDO file(@Param("agent") long agent,@Param("user") String user,@Param("session") String session,@Param("id") String id);
    @Select("SELECT DISTINCT f.* FROM t_agent_file f JOIN t_agent_run_file rf ON rf.file_id=f.id WHERE f.agent_id=#{agent} AND f.user_id=#{user} AND f.session_id=#{session} AND rf.run_id=#{run} ORDER BY f.create_time,f.id LIMIT 100")
    List<AgentFileDO> runFiles(@Param("agent") long agent,@Param("user") String user,@Param("session") String session,@Param("run") String run);
    @Insert("INSERT IGNORE INTO t_agent_run_file(run_id,file_id) VALUES(#{run},#{file})")
    int bind(@Param("run") String run,@Param("file") String file);
    @Insert("INSERT IGNORE INTO t_agent_run_file(run_id,file_id,purpose) VALUES(#{run},#{file},'OUTPUT')")
    int bindOutput(@Param("run") String run,@Param("file") String file);
    @Select("SELECT f.* FROM t_agent_file f JOIN t_agent_run_file rf ON rf.file_id=f.id WHERE f.agent_id=#{agent} AND f.user_id=#{user} AND f.session_id=#{session} AND rf.run_id=#{run} AND rf.purpose='INPUT' ORDER BY f.id LIMIT 5")
    List<AgentFileDO> inputs(@Param("agent") long agent,@Param("user") String user,@Param("session") String session,@Param("run") String run);
    @Select("SELECT * FROM t_agent_file f WHERE agent_id=#{agent} AND user_id=#{user} AND session_id=#{session} AND kind='ATTACHMENT' AND NOT EXISTS(SELECT 1 FROM t_agent_run_file rf WHERE rf.file_id=f.id) ORDER BY f.create_time,f.id LIMIT 100")
    List<AgentFileDO> pending(@Param("agent") long agent,@Param("user") String user,@Param("session") String session);
    @Select("SELECT id FROM t_agent_file WHERE agent_id=#{agent} AND user_id=#{user} AND session_id=#{session} ORDER BY id FOR UPDATE")
    List<String> lockScope(@Param("agent") long agent,@Param("user") String user,@Param("session") String session);
    @Select("SELECT COALESCE(SUM(size_bytes),0) FROM t_agent_file WHERE agent_id=#{agent} AND user_id=#{user} AND session_id=#{session}")
    long storedBytes(@Param("agent") long agent,@Param("user") String user,@Param("session") String session);
    @Select("SELECT COUNT(*) FROM t_agent_run_file WHERE file_id=#{id}")
    int references(String id);
    @Delete("DELETE FROM t_agent_file_chunk WHERE file_id=#{id}")
    int deleteChunks(String id);
    @Delete("DELETE FROM t_agent_file WHERE id=#{id}")
    int delete(String id);
    @Select("SELECT * FROM t_agent_file WHERE agent_id=#{agent} AND user_id=#{user} AND session_id=#{session} AND id=#{id} FOR UPDATE")
    AgentFileDO lock(@Param("agent") long agent,@Param("user") String user,@Param("session") String session,@Param("id") String id);
    @Select("SELECT COUNT(*) FROM t_agent_run_file rf JOIN t_agent_file f ON f.id=rf.file_id WHERE rf.run_id=#{run} AND rf.purpose='OUTPUT'")
    int artifactCount(String run);
}
