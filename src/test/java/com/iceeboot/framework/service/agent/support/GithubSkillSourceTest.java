package com.iceeboot.framework.service.agent.support;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iceeboot.framework.dto.create.agent.AgentSkillGithubDTO;
import com.iceeboot.common.exception.IceeBootException;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Flow;
class GithubSkillSourceTest {
    static class Source extends GithubSkillSource {
        final List<URI> requests=new ArrayList<>();
        final ArrayDeque<HttpResponse<byte[]>> responses=new ArrayDeque<>();
        protected HttpResponse<byte[]> send(URI uri) {requests.add(uri);return responses.remove();}
    }
    static HttpResponse<byte[]> response(int code,String location,byte[] bytes) {
        HttpResponse<byte[]> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(code);when(response.body()).thenReturn(bytes);
        when(response.headers()).thenReturn(HttpHeaders.of(location==null?Map.of():Map.of("Location",List.of(location)),(a,b)->true));return response;
    }
    static AgentSkillGithubDTO dto(String url) {var value=new AgentSkillGithubDTO();value.setUrl(url);return value;}
    @Test void followsOnlyGithubArchiveRedirectsAndSupportsTreeLinks() {
        var source=new Source();source.responses.add(response(302,"https://codeload.github.com/owner/repo/zip/main",new byte[0]));
        source.responses.add(response(200,null,new byte[]{1}));
        var result=source.download(dto("https://github.com/owner/repo/tree/main/skills/demo"));
        assertThat(result.directory()).isEqualTo("skills/demo");
        assertThat(source.requests.getFirst().toString()).isEqualTo("https://api.github.com/repos/owner/repo/zipball/main");
        assertThat(result.bytes()).containsExactly((byte)1);
    }
    @Test void defaultBranchAndExplicitRefsAndDirectoryAreSupported() {
        var source=new Source();source.responses.add(response(200,null,new byte[]{1}));
        source.download(dto("https://github.com/owner/repo.git"));
        assertThat(source.requests.getFirst().getPath()).endsWith("/zipball");
        var request=dto("https://github.com/owner/repo/tree/main/old");request.setRef("feature/new");request.setPath("skills/new");
        source.responses.add(response(200,null,new byte[]{1}));
        assertThat(source.download(request).directory()).isEqualTo("skills/new");
        assertThat(source.requests.getLast().toString()).endsWith("/feature%2Fnew");
    }
    @Test void rejectsUntrustedUrlsRedirectsErrorsAndTraversalBeforeReadingRemoteContent() {
        for(String url:List.of("http://github.com/owner/repo","https://github.com.evil.test/owner/repo","https://user@github.com/owner/repo","https://github.com:8443/owner/repo","https://127.0.0.1/repo/test","https://github.com/owner/repo?token=x")) {
            var source=new Source();assertThatThrownBy(()->source.download(dto(url))).isInstanceOf(IceeBootException.class);
            assertThat(source.requests).isEmpty();
        }
        var source=new Source();source.responses.add(response(302,"http://127.0.0.1/private",new byte[0]));
        assertThatThrownBy(()->source.download(dto("https://github.com/owner/repo"))).isInstanceOf(IceeBootException.class);
        assertThat(source.requests).hasSize(1);
        var request=dto("https://github.com/owner/repo");request.setPath("../private");
        assertThatThrownBy(()->new Source().download(request)).isInstanceOf(IceeBootException.class);
        source.responses.add(response(429,null,new byte[0]));
        assertThatThrownBy(()->source.download(dto("https://github.com/owner/repo"))).hasMessageContaining("下载失败");
    }
    @Test void bodySubscriberCancelsBeforeBufferingOversizedResponses() {
        var subscriber=new GithubSkillSource.LimitedBodySubscriber();
        var subscription=mock(Flow.Subscription.class);subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[SkillArchiveParser.MAX_ARCHIVE_BYTES+1])));
        verify(subscription).cancel();
        assertThatThrownBy(()->subscriber.getBody().toCompletableFuture().join()).hasCauseInstanceOf(IceeBootException.class);
    }
}
