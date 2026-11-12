package com.iceeboot.framework.controller.agent;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.core.domain.R;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.agent.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/agent-console")
public class AgentWorkbenchController {
    private final IAgentWorkbenchService workbench;
    private final IAgentConfigurationService configs;
    private final IAgentFileService files;
    private final IAgentRecoveryService recovery;
    private final ISysUserService users;
    private final AgentApiConverter converter;
    private String admin() {
        Long user=TokenUtils.getCurrentUserId();
        if(user==null || !users.getUserRoles(user).contains("superAdmin")) throw new IceeBootException(403,ResponseMessage.Agent.SUPER_ADMIN_REQUIRED);
        return user.toString();
    }
    @PostMapping("/agents/{id}/preflight")
    @RequirePermission(dynamic=true)
    @Operation(summary="预检技能与运行环境")
    public R<AgentPreflightVO> preflight(@PathVariable long id) {
        admin();return R.ok(workbench.preflight(id));
    }
    @GetMapping("/agents/{id}/releases/diff")
    @RequirePermission(dynamic=true)
    @Operation(summary="比较智能体发布版本")
    public R<AgentReleaseDiffVO> diff(@PathVariable long id,@RequestParam String from,@RequestParam String to) {
        admin();return R.ok(workbench.diff(id,from,to));
    }
    @PostMapping("/agents/{id}/releases/{releaseId}/rollback")
    @RequirePermission(dynamic=true)
    @Operation(summary="回滚智能体发布版本")
    public R<AgentReleaseVO> rollback(@PathVariable long id,@PathVariable String releaseId) {
        return R.ok(converter.convert(configs.rollback(id,releaseId,admin()),AgentReleaseVO.class));
    }
    @PostMapping("/agents/{id}/sessions/{sessionId}/files")
    @RequirePermission(dynamic=true)
    @Operation(summary="上传会话附件")
    public R<AgentFileVO> upload(@PathVariable long id,@PathVariable String sessionId,@RequestPart MultipartFile file) {
        String user=admin();
        if(file.isEmpty() || file.getSize()>IAgentFileService.MAX_BYTES) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
        try { return R.ok(files.upload(id,user,sessionId,file.getOriginalFilename(),file.getBytes())); }
        catch(java.io.IOException failure) { throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED,failure); }
    }
    @GetMapping("/agents/{id}/sessions/{sessionId}/files")
    @RequirePermission(dynamic=true)
    @Operation(summary="查询本轮附件和生成文件")
    public R<List<AgentFileVO>> files(@PathVariable long id,@PathVariable String sessionId,@RequestParam String runId) {
        return R.ok(files.list(id,admin(),sessionId,runId));
    }
    @GetMapping("/agents/{id}/sessions/{sessionId}/files/{fileId}")
    @RequirePermission(dynamic=true)
    @Operation(summary="下载会话文件")
    public ResponseEntity<byte[]> download(@PathVariable long id,@PathVariable String sessionId,@PathVariable String fileId) {
        var file=files.download(id,admin(),sessionId,fileId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store")
                .header("X-Content-Type-Options","nosniff").body(file.content());
    }
    @DeleteMapping("/agents/{id}/sessions/{sessionId}/files/{fileId}")
    @RequirePermission(dynamic=true)
    @Operation(summary="删除未使用的会话附件")
    public R<Void> deleteFile(@PathVariable long id,@PathVariable String sessionId,@PathVariable String fileId) {
        files.delete(id,admin(),sessionId,fileId);return R.ok();
    }
    @PostMapping("/agents/{id}/sessions/{sessionId}/runs/{runId}/recovery")
    @RequirePermission(dynamic=true)
    @Operation(summary="准备失败重试会话")
    public R<AgentRecoveryVO> recovery(@PathVariable long id,@PathVariable String sessionId,@PathVariable String runId) {
        return R.ok(recovery.prepare(id,admin(),sessionId,runId));
    }
}
