package com.iceeboot.framework.service.agent.support;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.vo.agent.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/** 在内存中解析技能包，所有路径先校验，不向应用服务器解压或执行文件。 */
@Component
public class SkillArchiveParser {
    public static final int MAX_ARCHIVE_BYTES = 20 * 1024 * 1024;
    public static final int MAX_EXPANDED_BYTES = 50 * 1024 * 1024;
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ENTRIES = 2000;
    private static final int MAX_SKILLS = 50;
    private static final Pattern FRONTMATTER = Pattern.compile("\\A---[ \\t]*\\r?\\n(.*?)\\r?\\n---[ \\t]*(?:\\r?\\n|\\z)", Pattern.DOTALL);

    public static byte[] readBounded(InputStream input, int maximum) throws IOException {
        byte[] bytes = input.readNBytes(maximum + 1);
        if (bytes.length > maximum) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
        return bytes;
    }

    public static String safePath(String value) {
        if (value == null || value.length() > 500 || value.startsWith("/") || value.contains("\\")
                || value.contains(":") || value.chars().anyMatch(c -> c < 32 || c == 127))
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_PATH);
        String path = value.replaceAll("/+$", "");
        for (String part : path.split("/", -1))
            if (part.isBlank() || part.equals(".") || part.equals(".."))
                throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_PATH);
        return path;
    }

    public AgentSkillImportPreviewVO parse(byte[] archive, String filename, String directory, boolean github) {
        if (archive.length == 0 || archive.length > MAX_ARCHIVE_BYTES)
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
        String selectedDirectory = directory == null || directory.isBlank() ? "" : safePath(directory);
        Map<String,byte[]> files = new TreeMap<>();
        try {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".zip")) {
                try (var zip = ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(archive)).get()) {
                    var entries = zip.getEntries();
                    int count = 0, expanded = 0;
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (++count > MAX_ENTRIES) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
                        if (entry.getRawName()!=null) {
                            for(byte value:entry.getRawName())
                                if (value == '\\') throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_PATH);
                        }
                        String path = archivePath(entry.getName(),entry.isDirectory());
                        if (path == null) continue;
                        if (github && !selectedDirectory.isEmpty()) {
                            int slash = path.indexOf('/');
                            if (slash < 0 || !path.substring(slash + 1).startsWith(selectedDirectory + "/")) continue;
                        }
                        int type = entry.getUnixMode() & 0170000;
                        if (entry.isUnixSymlink() || (type != 0 && type != 0100000 && type != 0040000) || !zip.canReadEntryData(entry))
                            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_ARCHIVE);
                        if (entry.isDirectory()) continue;
                        byte[] bytes;
                        try (InputStream in = zip.getInputStream(entry)) { bytes = readBounded(in, MAX_FILE_BYTES); }
                        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                        crc.update(bytes);
                        if (entry.getCrc() != crc.getValue() || entry.getSize() != bytes.length)
                            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_ARCHIVE);
                        expanded += bytes.length;
                        add(files,path,bytes,expanded);
                    }
                }
            } else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
                try (var tar = new TarArchiveInputStream(limitExpanded(new GzipCompressorInputStream(new ByteArrayInputStream(archive))))) {
                    TarArchiveEntry entry;
                    int count = 0, expanded = 0;
                    while ((entry = tar.getNextEntry()) != null) {
                        if (++count > MAX_ENTRIES) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
                        String path = archivePath(entry.getName(),entry.isDirectory());
                        if (path == null) continue;
                        if (entry.isDirectory()) continue;
                        if (!entry.isFile() || entry.isSymbolicLink() || entry.isLink())
                            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_ARCHIVE);
                        byte[] bytes = readBounded(tar,MAX_FILE_BYTES);
                        expanded += bytes.length;
                        add(files,path,bytes,expanded);
                    }
                }
            } else throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_FORMAT);
            if (github) {
                Map<String,byte[]> relative = new TreeMap<>();
                for (var entry : files.entrySet()) {
                    int slash = entry.getKey().indexOf('/');
                    if (slash < 0) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_ARCHIVE);
                    relative.put(entry.getKey().substring(slash+1),entry.getValue());
                }
                files = relative;
            }
            String prefix = directory == null || directory.isBlank() ? "" : safePath(directory) + "/";
            List<String> roots = files.keySet().stream()
                    .filter(p -> p.startsWith(prefix) && (p.equals("SKILL.md") || p.endsWith("/SKILL.md")))
                    .map(p -> p.substring(0,p.length()-"SKILL.md".length())).toList();
            if (roots.isEmpty()) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_NOT_FOUND);
            if (roots.size() > MAX_SKILLS) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
            List<AgentSkillImportItemVO> skills = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            for (String root : roots) {
                String content = text(files.get(root + "SKILL.md"));
                if (content.isBlank() || content.length()>100000) throw new IceeBootException(ResponseMessage.Agent.SKILL_CONTENT_TOO_LONG);
                var match = FRONTMATTER.matcher(content);
                Map<?,?> metadata = Map.of();
                if (content.startsWith("---") && !match.find()) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_METADATA);
                match.reset();
                if (match.find()) {
                    LoaderOptions options = new LoaderOptions();
                    options.setAllowDuplicateKeys(false);
                    options.setMaxAliasesForCollections(0);
                    options.setCodePointLimit(100000);
                    options.setNestingDepthLimit(30);
                    Object parsed = new Yaml(new SafeConstructor(options)).load(match.group(1));
                    if (!(parsed instanceof Map<?,?> map)) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_METADATA);
                    metadata = map;
                } else warnings.add((root.isEmpty()?"根目录":root) + "：未找到 YAML 元数据，已根据目录生成名称，请确认。");
                String folder = root.isEmpty()?"skill":root.substring(0,root.length()-1);
                folder = folder.substring(folder.lastIndexOf('/')+1);
                String name = string(metadata,"name",folder);
                String description = string(metadata,"description","");
                if (name.isBlank() || name.length()>120 || description.length()>2000)
                    throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_METADATA);
                String code = name.replaceAll("[^a-zA-Z0-9_-]+","_").replaceAll("^_+|_+$","");
                if (code.isBlank()) code = "skill_" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))).substring(0,12);
                if (!Character.isLetter(code.charAt(0))) code = "skill_" + code;
                code = code.substring(0,Math.min(64,code.length()));
                Map<String,String> resources = new LinkedHashMap<>();
                int resourceSize = 0;
                for (var entry : files.entrySet()) {
                    String path = entry.getKey();
                    if (!path.startsWith(root) || path.equals(root+"SKILL.md")) continue;
                    if (roots.stream().anyMatch(child -> !child.equals(root) && child.startsWith(root) && path.startsWith(child))) continue;
                    String relative = path.substring(root.length());
                    String value = text(entry.getValue());
                    if (relative.equals("SKILL.md") || value.length()>1000000) throw new IceeBootException(ResponseMessage.Agent.SKILL_RESOURCE_CONTENT_TOO_LONG);
                    resources.put(relative,value);
                    resourceSize += value.length();
                    if (resources.size()>100 || resourceSize>5000000) throw new IceeBootException(ResponseMessage.Agent.SKILL_RESOURCES_TOO_LARGE);
                }
                AgentSkillImportItemVO item = new AgentSkillImportItemVO();
                item.setSourcePath(root.isEmpty()?".":root.substring(0,root.length()-1));
                item.setCode(code); item.setName(name); item.setDescription(description); item.setEnabled(true);
                item.setContent(content); item.setResources(resources); item.setToolIds(List.of());
                skills.add(item);
            }
            AgentSkillImportPreviewVO result = new AgentSkillImportPreviewVO();
            result.setSkills(skills); result.setWarnings(warnings);
            return result;
        } catch (IceeBootException e) { throw e; }
        catch (Exception e) { throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_ARCHIVE,e); }
    }

    /** 同时计入 tar 元数据和跳过的字节，避免 PAX 头在条目校验前无限展开。 */
    private InputStream limitExpanded(InputStream source) {
        return new FilterInputStream(source) {
            private long consumed;
            private void count(int amount) {
                if (amount > 0 && (consumed += amount) > MAX_EXPANDED_BYTES)
                    throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
            }
            @Override
            public int read() throws IOException {
                int value = in.read();
                if (value >= 0) count(1);
                return value;
            }
            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                int amount = in.read(bytes, offset, (int)Math.min(length, MAX_EXPANDED_BYTES - consumed + 1));
                count(amount);
                return amount;
            }
            @Override
            public long skip(long amount) throws IOException {
                byte[] buffer = new byte[8192];
                long skipped = 0;
                while (skipped < amount) {
                    int read = read(buffer, 0, (int)Math.min(buffer.length, amount - skipped));
                    if (read < 0) break;
                    skipped += read;
                }
                return skipped;
            }
        };
    }
    private String archivePath(String path, boolean directory) {
        if (directory && (path.equals(".") || path.equals("./"))) return null;
        while (path.startsWith("./")) path = path.substring(2);
        return safePath(path);
    }
    private void add(Map<String,byte[]> files,String path,byte[] bytes,int expanded) {
        if (expanded>MAX_EXPANDED_BYTES) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT);
        if (path.startsWith("__MACOSX/") || path.endsWith("/.DS_Store") || path.equals(".DS_Store") || path.startsWith(".git/") || path.contains("/.git/")) return;
        if (files.putIfAbsent(path,bytes)!=null) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_PATH);
    }
    private String string(Map<?,?> metadata,String key,String fallback) {
        Object value=metadata.get(key);
        if (value==null) return fallback;
        if (!(value instanceof String text)) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_METADATA);
        return text;
    }
    private String text(byte[] bytes) {
        try {
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (text.indexOf('\0')>=0) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_BINARY);
            return text.startsWith("\uFEFF")?text.substring(1):text;
        } catch (CharacterCodingException e) { throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_BINARY,e); }
    }
}
