package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.util.JsonUtils;
import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.filesystem.remote.store.StoreItem;
import java.util.*;

/** 为 SDK 的工具结果引用提供会话范围内的加密数据库文件存储。 */
public final class ToolResultFileStore implements BaseStore {
    public static final String PATH = "/.tool-results/";
    private static final String FILE_USER = "tool-result-files";
    private static final String FILE_KEY = "file";
    private static final int MAX_CAS_ATTEMPTS = 8;
    private final AgentStateStore states;
    public ToolResultFileStore(AgentStateStore states) { this.states = states; }
    // value 是 SDK RemoteFilesystem 规定的动态文件字典，不是业务 DTO。
    public record StoredFile(String key, Map<String,Object> value) implements State {}
    private String prefix(List<String> namespace) { return JsonUtils.getJsonCodec().toJson(namespace) + "\n"; }
    private String slot(List<String> namespace, String key) { return prefix(namespace) + key; }
    public StoreItem get(List<String> namespace, String key) {
        var current = states.getVersioned(FILE_USER,slot(namespace,key),FILE_KEY,StoredFile.class);
        return current.value() == null ? null : new StoreItem(key,current.value().value(),current.version());
    }
    public boolean putIfVersion(List<String> namespace, String key, Map<String,Object> value, long expected) {
        String slot = slot(namespace,key);
        var current = states.getVersioned(FILE_USER,slot,FILE_KEY,StoredFile.class);
        long visibleVersion = current.value() == null ? 0 : current.version();
        if (visibleVersion != expected) return false;
        try {
            states.saveIfVersion(FILE_USER,slot,FILE_KEY,new StoredFile(key,value),current.version());
            return true;
        } catch (IceeBootException conflict) {
            if (Objects.equals(conflict.getCode(),409)) return false;
            throw conflict;
        }
    }
    public void put(List<String> namespace, String key, Map<String,Object> value) {
        for(int attempt=0;attempt<MAX_CAS_ATTEMPTS;attempt++) {
            StoreItem current = get(namespace,key);
            if (putIfVersion(namespace,key,value,current == null ? 0 : current.version())) return;
        }
        throw new IceeBootException(409,ResponseMessage.Agent.EXECUTION_SESSION_BUSY);
    }
    public List<StoreItem> search(List<String> namespace, int limit, int offset) {
        return states.listSessionIds(FILE_USER).stream().filter(id -> id.startsWith(prefix(namespace))).sorted()
                .skip(offset).limit(limit).map(id -> {
                    var file=states.getVersioned(FILE_USER,id,FILE_KEY,StoredFile.class);
                    return file.value() == null ? null : new StoreItem(file.value().key(),file.value().value(),file.version());
                }).filter(Objects::nonNull).toList();
    }
    public void delete(List<String> namespace,String key) { states.delete(FILE_USER,slot(namespace,key),FILE_KEY); }
}
