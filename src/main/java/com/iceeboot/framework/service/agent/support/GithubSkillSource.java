package com.iceeboot.framework.service.agent.support;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.agent.AgentSkillGithubDTO;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** 只下载公开 GitHub 归档，手动校验重定向目标，不调用 Git 或读取本机凭据。 */
@Component
public class GithubSkillSource {
    public record Archive(byte[] bytes, String directory) {}
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public Archive download(AgentSkillGithubDTO request) {
        try {
            URI url = URI.create(request.getUrl().trim());
            if (!"https".equalsIgnoreCase(url.getScheme()) || !"github.com".equalsIgnoreCase(url.getHost())
                    || url.getUserInfo()!=null || url.getPort()!=-1 || url.getQuery()!=null || url.getFragment()!=null)
                throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_GITHUB_URL);
            String[] parts=url.getPath().replaceAll("^/|/+$","").split("/");
            if (parts.length<2 || !parts[0].matches("[a-zA-Z0-9-]{1,39}"))
                throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_GITHUB_URL);
            String repo=parts[1].replaceFirst("\\.git$","");
            if (!repo.matches("[a-zA-Z0-9_.-]{1,100}") || repo.equals(".") || repo.equals(".."))
                throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_GITHUB_URL);
            String ref=request.getRef(), path=request.getPath();
            if (parts.length>2) {
                if (parts.length<4 || !parts[2].equals("tree")) throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_GITHUB_URL);
                if (ref==null || ref.isBlank()) ref=parts[3];
                if ((path==null || path.isBlank()) && parts.length>4) path=String.join("/",Arrays.copyOfRange(parts,4,parts.length));
            }
            if (path!=null && !path.isBlank()) path=SkillArchiveParser.safePath(path.trim());
            String suffix=ref==null || ref.isBlank()?"":"/"+URLEncoder.encode(ref.trim(),StandardCharsets.UTF_8).replace("+","%20");
            URI target=URI.create("https://api.github.com/repos/"+parts[0]+"/"+repo+"/zipball"+suffix);
            for(int redirects=0;redirects<=3;redirects++) {
                validateTarget(target);
                HttpResponse<byte[]> response=send(target);
                if (response.statusCode()==200) return new Archive(response.body(),path);
                if (Set.of(301,302,303,307,308).contains(response.statusCode()) && redirects<3) {
                    target=target.resolve(response.headers().firstValue("Location").orElseThrow(
                            () -> new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DOWNLOAD)));
                } else throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DOWNLOAD);
            }
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DOWNLOAD);
        } catch (IceeBootException e) { throw e; }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DOWNLOAD,e);
        } catch (Exception e) { throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DOWNLOAD,e); }
    }
    private void validateTarget(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo()!=null
                || (uri.getPort()!=-1 && uri.getPort()!=443)
                || !Set.of("api.github.com","codeload.github.com").contains(uri.getHost()))
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_GITHUB_URL);
    }
    protected HttpResponse<byte[]> send(URI uri) throws java.io.IOException, InterruptedException {
        var pending = client.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
                .header("Accept","application/vnd.github+json").header("User-Agent","IceeBoot-Skill-Import").GET().build(),
                info -> new LimitedBodySubscriber());
        try {
            return pending.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            pending.cancel(true);
            if (e.getCause() instanceof IceeBootException business) throw business;
            throw new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_DOWNLOAD, e);
        } catch (InterruptedException e) {
            pending.cancel(true);
            throw e;
        }
    }
    /** 在订阅时限制体积，HTTP 请求超时覆盖整个响应体，避免先无限缓存再校验。 */
    static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(Flow.Subscription subscription) { this.subscription=subscription; subscription.request(1); }
        public void onNext(List<ByteBuffer> chunks) {
            for (ByteBuffer chunk:chunks) {
                if ((long)bytes.size()+chunk.remaining()>SkillArchiveParser.MAX_ARCHIVE_BYTES) {
                    subscription.cancel();
                    result.completeExceptionally(new IceeBootException(ResponseMessage.Agent.SKILL_IMPORT_LIMIT));
                    return;
                }
                byte[] value=new byte[chunk.remaining()];chunk.get(value);bytes.writeBytes(value);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { result.completeExceptionally(error); }
        public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
