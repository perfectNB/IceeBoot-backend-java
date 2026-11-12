package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.framework.entity.agent.AgentSessionStateDO;
import com.iceeboot.framework.mapper.agent.AgentConversationMapper;
import io.agentscope.core.session.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** 压缩的不可变日志块快照。head、writer、导出游标始终直读数据库，保留 SDK 的 fencing/CAS 语义。 */
@Slf4j
public final class CachedJournalSessionLog implements SessionLog {
    private static final String CACHE_PATH = "iceeboot/replay-cache-v1";
    private static final int MAX_SNAPSHOT_BYTES = 64 * 1024 * 1024;
    private static final int SNAPSHOT_FORMAT = 1;
    private final JournalSessionLog delegate;
    private final JournalStorage storage;
    private final RunRecorder recorder;

    public CachedJournalSessionLog(AgentConversationMapper mapper, long agent, String user, String session, RunRecorder recorder) {
        this.storage = new JournalStorage(mapper, agent, user, session);
        this.delegate = new JournalSessionLog(storage, "session");
        this.recorder = recorder;
    }
    public Head head() { return delegate.head(); }
    public Writer acquire(String owner, Duration ttl) { return delegate.acquire(owner, ttl); }
    public Head sealWriter(String owner) { return delegate.sealWriter(owner); }
    public void renew(Writer writer, Duration ttl) { delegate.renew(writer, ttl); }
    public void release(Writer writer) {
        delegate.release(writer);
        storage.saveSnapshot();
    }
    public Head commit(Writer writer, String batchId, long expected, List<SessionEvent> events) {
        return delegate.commit(writer, batchId, expected, events);
    }
    public SessionLog inbox() { return delegate.inbox(); }
    public long exportCursor(String consumer) { return delegate.exportCursor(consumer); }
    public void advanceExportCursor(String consumer, long expected, long next) { delegate.advanceExportCursor(consumer, expected, next); }
    public List<SessionEvent> readAfter(long after, int limit) { return delegate.readAfter(after, limit); }
    public Iterable<SessionEvent> scan(long after, long through) {
        if (recorder != null) recorder.beginStage("CONTEXT_RESTORE", "恢复会话上下文");
        String status = "FAILED";
        try {
            List<SessionEvent> events = new ArrayList<>();
            delegate.scan(after, through).forEach(events::add);
            status = "SUCCEEDED";
            return events;
        } finally {
            if (recorder != null) recorder.endStage("CONTEXT_RESTORE", status);
        }
    }

    private static final class JournalStorage implements AtomicSessionStorage {
        private final AgentConversationMapper mapper;
        private final long agent;
        private final String user, session;
        private final Map<String, Value> immutable = new HashMap<>();
        private boolean loaded, dirty;
        private long snapshotRevision;
        JournalStorage(AgentConversationMapper mapper, long agent, String user, String session) {
            this.mapper = mapper; this.agent = agent; this.user = user; this.session = session;
        }
        private static boolean immutablePath(String path) {
            return path.startsWith("session/") && (path.contains("/commits/") || path.contains("/blobs/"));
        }
        public synchronized Value read(String path) {
            if (immutablePath(path)) {
                loadSnapshot();
                Value cached = immutable.get(path);
                if (cached != null) return cached;
            }
            AgentSessionStateDO row = mapper.state(agent, user, session, path);
            Value value = row == null ? null : new Value(row.getRevision(), row.getPayload());
            if (value != null && immutablePath(path)) { immutable.put(path, value); dirty = true; }
            return value;
        }
        public synchronized boolean compareAndSet(String path, long expected, byte[] payload) {
            boolean saved;
            if (expected == 0) {
                try { saved = mapper.insertState(agent, user, session, path, payload) == 1; }
                catch (DuplicateKeyException race) { saved = false; }
            } else saved = mapper.updateState(agent, user, session, path, expected, payload) == 1;
            if (saved && immutablePath(path)) {
                loadSnapshot();
                immutable.put(path, new Value(expected + 1, payload)); dirty = true;
            }
            return saved;
        }
        public List<String> listPaths(String prefix) {
            return mapper.paths(agent, user, session).stream().filter(path -> path.startsWith(prefix)).toList();
        }
        private void loadSnapshot() {
            if (loaded) return;
            loaded = true;
            AgentSessionStateDO row = mapper.state(agent, user, session, CACHE_PATH);
            if (row == null) return;
            snapshotRevision = row.getRevision();
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(row.getPayload()))) {
                byte[] bytes = gzip.readNBytes(MAX_SNAPSHOT_BYTES + 1);
                if (bytes.length > MAX_SNAPSHOT_BYTES) { log.warn("Session replay cache exceeds size limit; using journal"); return; }
                var input = new DataInputStream(new ByteArrayInputStream(bytes));
                if (input.readInt() != SNAPSHOT_FORMAT) return;
                int count = input.readInt();
                Map<String, Value> restored = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    String path = input.readUTF(); long revision = input.readLong(); int size = input.readInt();
                    if (!immutablePath(path) || size < 0 || size > input.available()) return;
                    restored.put(path, new Value(revision, input.readNBytes(size)));
                }
                if (input.available() == 0) immutable.putAll(restored);
            } catch (IOException invalidCache) {
                // 派生缓存损坏不能覆盖或破坏原始日志；下次成功释放 writer 后重建。
                log.warn("Session replay cache unavailable; using journal");
            }
        }
        synchronized void saveSnapshot() {
            if (!dirty) return;
            try {
                var raw = new ByteArrayOutputStream();
                var output = new DataOutputStream(raw);
                output.writeInt(SNAPSHOT_FORMAT); output.writeInt(immutable.size());
                for (var entry : immutable.entrySet()) {
                    output.writeUTF(entry.getKey()); output.writeLong(entry.getValue().version());
                    byte[] bytes = entry.getValue().bytes(); output.writeInt(bytes.length); output.write(bytes);
                    if (raw.size() > MAX_SNAPSHOT_BYTES) return;
                }
                var packed = new ByteArrayOutputStream();
                try (var gzip = new GZIPOutputStream(packed)) { raw.writeTo(gzip); }
                boolean saved;
                if (snapshotRevision == 0) {
                    try { saved = mapper.insertState(agent, user, session, CACHE_PATH, packed.toByteArray()) == 1; }
                    catch (DuplicateKeyException race) { saved = false; }
                } else saved = mapper.updateState(agent, user, session, CACHE_PATH, snapshotRevision, packed.toByteArray()) == 1;
                if (saved) { snapshotRevision++; dirty = false; }
            } catch (IOException | org.springframework.dao.DataAccessException cacheFailure) {
                log.warn("Session replay cache update skipped; authoritative journal retained");
            }
        }
    }
}
