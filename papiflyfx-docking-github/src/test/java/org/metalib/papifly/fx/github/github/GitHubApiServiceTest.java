package org.metalib.papifly.fx.github.github;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubApiServiceTest {

    @Test
    void fetchDefaultBranchParsesResponse() {
        StubHttpClient client = new StubHttpClient(response(200, "{\"default_branch\":\"main\"}"));
        GitHubApiService service = new GitHubApiService(client, Optional::empty, URI.create("https://example.test"), 1);

        String branch = service.fetchDefaultBranch("org", "repo");

        assertEquals("main", branch);
        assertEquals(1, client.calls());
    }

    @Test
    void createPullRequestParsesResponse() {
        StubHttpClient client = new StubHttpClient(
            response(201, "{\"number\":42,\"html_url\":\"https://github.com/org/repo/pull/42\"}")
        );
        GitHubApiService service = new GitHubApiService(client, Optional::empty, URI.create("https://example.test"), 1);

        PullRequestResult result = service.createPullRequest("org", "repo",
            new PullRequestDraft("title", "body", "feature", "main", false));

        assertEquals(42, result.number());
        assertEquals("https://github.com/org/repo/pull/42", result.url().toString());
        assertEquals(1, client.calls());
    }

    @Test
    void mapsAuthFailure() {
        StubHttpClient client = new StubHttpClient(response(401, "{}"));
        GitHubApiService service = new GitHubApiService(client, Optional::empty, URI.create("https://example.test"), 1);

        GitHubApiException exception = assertThrows(GitHubApiException.class,
            () -> service.fetchDefaultBranch("org", "repo"));

        assertEquals(GitHubApiException.Category.AUTHENTICATION, exception.category());
        assertEquals(401, exception.statusCode());
    }

    @Test
    void mapsValidationFailure() {
        StubHttpClient client = new StubHttpClient(response(422, "{}"));
        GitHubApiService service = new GitHubApiService(client, Optional::empty, URI.create("https://example.test"), 1);

        GitHubApiException exception = assertThrows(GitHubApiException.class,
            () -> service.createPullRequest("org", "repo",
                new PullRequestDraft("title", "", "feature", "main", false)));

        assertEquals(GitHubApiException.Category.VALIDATION, exception.category());
        assertEquals(422, exception.statusCode());
    }

    @Test
    void retriesTransientFailures() {
        StubHttpClient client = new StubHttpClient(
            response(503, "{}"),
            response(200, "{\"default_branch\":\"main\"}")
        );
        GitHubApiService service = new GitHubApiService(client, Optional::empty, URI.create("https://example.test"), 3);

        String branch = service.fetchDefaultBranch("org", "repo");

        assertEquals("main", branch);
        assertEquals(2, client.calls());
    }

    private static HttpResponse<String> response(int status, String body) {
        return new StubHttpResponse(status, body);
    }

    private static final class StubHttpClient extends HttpClient {

        private final List<HttpResponse<String>> responses;
        private final AtomicInteger calls;

        private StubHttpClient(HttpResponse<String>... responses) {
            this.responses = List.of(responses);
            this.calls = new AtomicInteger();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            int index = calls.getAndIncrement();
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) responses.get(index);
            return response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException();
        }

        private int calls() {
            return calls.get();
        }
    }

    private record StubHttpResponse(int statusCode, String body) implements HttpResponse<String> {

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("https://example.test")).build();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
        }

        @Override
        public URI uri() {
            return URI.create("https://example.test");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
