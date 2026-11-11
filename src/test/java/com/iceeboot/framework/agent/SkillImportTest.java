package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.controller.agent.AgentSkillController;
import com.iceeboot.framework.dto.create.agent.*;
import com.iceeboot.framework.service.agent.support.SkillArchiveParser;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.AgentSkillImportItemVO;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class SkillImportTest {
    final SkillArchiveParser parser=new SkillArchiveParser();
    static byte[] text(String content) { return content.getBytes(StandardCharsets.UTF_8); }
    static byte[] zip(Map<String,byte[]> files) throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var zip=new ZipArchiveOutputStream(bytes)) {
            for(var entry:files.entrySet()) {
                zip.putArchiveEntry(new ZipArchiveEntry(entry.getKey()));
                zip.write(entry.getValue());zip.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }
    static AgentSkillDTO dto(AgentSkillImportItemVO item) {
        var dto=new AgentSkillDTO();dto.setCode(item.getCode());dto.setName(item.getName());
        dto.setDescription(item.getDescription());dto.setEnabled(item.isEnabled());dto.setContent(item.getContent());
        dto.setResources(item.getResources());dto.setToolIds(item.getToolIds());return dto;
    }
    static AgentSkillImportDTO batch(AgentSkillDTO... skills) {
        var dto=new AgentSkillImportDTO();dto.setSkills(List.of(skills));return dto;
    }
    @Test
    void previewsMultipleSkillsAndKeepsTextResourcesWithoutWritingAnything() throws Exception {
        byte[] archive=zip(Map.of(
                "pack/report/SKILL.md",text("---\nname: report-analysis\ndescription: >\n  Analyze reports\n  with context\n---\n# Instructions"),
                "pack/report/scripts/run.py",text("print('hello')"),
                "pack/report/references/guide.md",text("参考资料"),
                "pack/other/SKILL.md",text("# Second")));
        var preview=parser.parse(archive,"skills.zip",null,false);
        assertThat(preview.getSkills()).hasSize(2);
        var report=preview.getSkills().stream().filter(s->s.getCode().equals("report-analysis")).findFirst().orElseThrow();
        assertThat(report.getDescription()).contains("Analyze reports with context");
        assertThat(report.getResources()).containsEntry("scripts/run.py","print('hello')").containsEntry("references/guide.md","参考资料");
        assertThat(report.getToolIds()).isEmpty();
        assertThat(preview.getWarnings()).hasSize(1);
        var f=new AgentFixture();
        var upload=new AgentSkillArchiveDTO();upload.setFile(new MockMultipartFile("file","skills.zip","application/zip",archive));
        assertThat(f.skillImports().previewArchive(upload).getSkills()).hasSize(2);
        assertThat(f.configs.capabilities()).isEmpty();
        var request=new AgentSkillImportDTO();request.setSkills(preview.getSkills().stream().map(SkillImportTest::dto).toList());
        assertThat(f.skillImports().commit(request)).hasSize(2);
        assertThat(f.configs.capabilities()).hasSize(2);
    }
    @Test
    void nestedSkillsAreNotIncludedAsParentResourcesAndGithubDirectoryIsApplied() throws Exception {
        var bytes=zip(Map.of("repo-sha/skills/parent/SKILL.md",text("# Parent"),
                "repo-sha/skills/parent/child/SKILL.md",text("# Child"),
                "repo-sha/skills/parent/child/script.py",text("pass"),
                "repo-sha/unrelated/SKILL.md",text("# Other"),
                "repo-sha/unrelated/big.dat",new byte[6*1024*1024]));
        var result=parser.parse(bytes,"repo.zip","skills/parent",true);
        assertThat(result.getSkills()).hasSize(2);
        assertThat(result.getSkills().getFirst().getSourcePath()).startsWith("skills/parent");
        var parent=result.getSkills().stream().filter(s->s.getSourcePath().equals("skills/parent")).findFirst().orElseThrow();
        assertThat(parent.getResources()).isEmpty();
    }
    @Test
    void acceptsTarGzipWithConventionalDotRoot() throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var tar=new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
            var root=new TarArchiveEntry("./");tar.putArchiveEntry(root);tar.closeArchiveEntry();
            byte[] content=text("# Tar skill");
            var entry=new TarArchiveEntry("./demo/SKILL.md");entry.setSize(content.length);
            tar.putArchiveEntry(entry);tar.write(content);tar.closeArchiveEntry();
        }
        assertThat(parser.parse(bytes.toByteArray(),"skills.tar.gz",null,false).getSkills()).hasSize(1);
    }
    @Test
    void rejectsTraversalLinksDuplicatesBinaryUnsafeYamlAndOversizedEntries() throws Exception {
        for(String path:List.of("../SKILL.md","/SKILL.md","demo/../../SKILL.md","demo\\SKILL.md","C:/SKILL.md")) {
            var raw=new ByteArrayOutputStream();
            try(var zip=new java.util.zip.ZipOutputStream(raw)) {
                zip.putNextEntry(new java.util.zip.ZipEntry(path));zip.write(text("# bad"));zip.closeEntry();
            }
            byte[] archive=raw.toByteArray();
            assertThatThrownBy(()->parser.parse(archive,"bad.zip",null,false)).as(path).isInstanceOf(IceeBootException.class);
        }
        var bytes=new ByteArrayOutputStream();
        try(var zip=new ZipArchiveOutputStream(bytes)) {
            var entry=new ZipArchiveEntry("SKILL.md");entry.setUnixMode(0120777);
            zip.putArchiveEntry(entry);zip.write(text("target"));zip.closeArchiveEntry();
        }
        assertThatThrownBy(()->parser.parse(bytes.toByteArray(),"link.zip",null,false)).hasMessageContaining("链接");
        byte[] binary=zip(Map.of("SKILL.md",text("# Skill"),"asset.bin",new byte[]{0,1,2}));
        assertThatThrownBy(()->parser.parse(binary,"binary.zip",null,false)).hasMessageContaining("UTF-8");
        byte[] yaml=zip(Map.of("SKILL.md",text("---\nname: !!java.net.URL [https://example.invalid]\n---\nbody")));
        assertThatThrownBy(()->parser.parse(yaml,"yaml.zip",null,false)).isInstanceOf(IceeBootException.class);
        byte[] duplicateYaml=zip(Map.of("SKILL.md",text("---\nname: one\nname: two\n---\nbody")));
        assertThatThrownBy(()->parser.parse(duplicateYaml,"yaml.zip",null,false)).isInstanceOf(IceeBootException.class);
        byte[] large=zip(Map.of("SKILL.md",text("# Skill"),"large.txt",new byte[6*1024*1024]));
        assertThatThrownBy(()->parser.parse(large,"large.zip",null,false)).hasMessageContaining("最大");
        assertThatThrownBy(()->parser.parse(new byte[]{1,2,3},"broken.zip",null,false)).isInstanceOf(IceeBootException.class);
        assertThatThrownBy(()->parser.parse(zip(Map.of("README.md",text("none"))),"none.zip",null,false)).hasMessageContaining("未找到");
        var duplicate=new ByteArrayOutputStream();
        try(var zip=new ZipArchiveOutputStream(duplicate)) {
            for(int i=0;i<2;i++) {zip.putArchiveEntry(new ZipArchiveEntry("SKILL.md"));zip.write(text("# duplicate"));zip.closeArchiveEntry();}
        }
        assertThatThrownBy(()->parser.parse(duplicate.toByteArray(),"duplicate.zip",null,false)).hasMessageContaining("重复");
    }
    @Test
    void conflictsAndTamperedRequestsRollbackWholeBatch() throws Exception {
        var f=new AgentFixture();
        var item=parser.parse(zip(Map.of("SKILL.md",text("---\nname: existing\ndescription: test\n---\nbody"))),"skill.zip",null,false).getSkills().getFirst();
        f.skillImports().commit(batch(dto(item)));
        var first=dto(item);first.setCode("new_skill");
        assertThatThrownBy(()->f.skillImports().commit(batch(first,dto(item)))).isInstanceOf(RuntimeException.class);
        assertThat(f.configs.capabilities()).hasSize(1);
        assertThatThrownBy(()->f.skillImports().commit(batch(first,first))).hasMessageContaining("重复编码");
        var bad=dto(item);bad.setCode("bad_path");bad.setResources(Map.of("../escape","no"));
        assertThatThrownBy(()->f.skillImports().commit(batch(first,bad))).isInstanceOf(IceeBootException.class);
        assertThat(f.configs.capabilities()).hasSize(1);
        bad.setCode("1 invalid");
        assertThatThrownBy(()->f.skillImports().commit(batch(bad))).isInstanceOf(IceeBootException.class);
    }
    @Test
    void multipartPreviewAndConfirmationUseExistingPermissionsAndVoContracts() throws Exception {
        var f=new AgentFixture();var users=mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        var mvc=MockMvcBuilders.standaloneSetup(new AgentSkillController(f.skillsService(),users,f.skillImports()))
                .setControllerAdvice(new AgentConsoleExceptionHandler()).build();
        byte[] bytes=zip(Map.of("SKILL.md",text("---\nname: upload\ndescription: test\n---\nbody")));
        try(var tokens=mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            mvc.perform(multipart("/v1/agent-console/skills/import/archive").file(new MockMultipartFile("file","skill.zip","application/zip",bytes)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.skills[0].code").value("upload"));
            assertThat(f.configs.capabilities()).isEmpty();
            var request=batch(dto(parser.parse(bytes,"skill.zip",null,false).getSkills().getFirst()));
            mvc.perform(post("/v1/agent-console/skills/import").contentType(MediaType.APPLICATION_JSON).content(f.json.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result[0].code").value("upload"));
            mvc.perform(multipart("/v1/agent-console/skills/import/archive")).andExpect(status().isBadRequest());
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(2L);
            mvc.perform(post("/v1/agent-console/skills/import/github").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://github.com/owner/repo\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}
