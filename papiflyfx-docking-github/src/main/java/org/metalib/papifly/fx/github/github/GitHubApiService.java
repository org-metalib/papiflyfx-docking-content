package org.metalib.papifly.fx.github.github;

import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubApiService {

    private static final Pattern DEFAULT_BRANCH_PATTERN = Pattern.compile("\"default_branch\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PR_NUMBER_PATTERN = Pattern.compile("\"number\"\\s*:\\s*(\\d+)");
    private static final Pattern PR_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final Supplier<Optional<String>> tokenSupplier;
    private final URI apiBaseUri;
    private final int maxRetries;

    public GitHubApiService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(),
            Optional::empty,
            URI.create("https://api.github.com"),
            3);
    }

    public GitHubApiService(HttpClient httpClient, Supplier<Optional<String>> tokenSupplier) {
        this(httpClient, tokenSupplier, URI.create("https://api.github.com"), 3);
    }

    public GitHubApiService(
        HttpClient httpClient,
        Supplier<Optional<String>> tokenSupplier,
        URI apiBaseUri,
        int maxRetries
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.tokenSupplier = Objects.requireNonNull(tokenSupplier, "tokenSupplier");
        this.apiBaseUri = Objects.requireNonNull(apiBaseUri, "apiBaseUri");
        this.maxRetries = Math.max(1, maxRetries);
    }

    public String fetchDefaultBranch(String owner, String repo) {
        URI uri = apiBaseUri.resolve("/repos/" + owner + "/" + repo);
        HttpRequest request = requestBuilder(uri).GET().build();
        HttpResponse<String> response = sendWithRetry(request);

        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        if (status >= 200 && status < 300) {
            String value = extract(DEFAULT_BRANCH_PATTERN, body);
            if (value == null || value.isBlank()) {
                throw new GitHubApiException(GitHubApiException.Category.UNEXPECTED,
                    "GitHub response did not include default branch", status);
            }
            return value;
        }

        throw mapFailure(status, body, "Failed to fetch default branch");
    }

    public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
        Objects.requireNonNull(draft, "draft");

        URI uri = apiBaseUri.resolve("/repos/" + owner + "/" + repo + "/pulls");
        String requestBody = "{" +
            "\"title\":\"" + escapeJson(draft.title()) + "\"," +
            "\"body\":\"" + escapeJson(draft.body()) + "\"," +
            "\"head\":\"" + escapeJson(draft.head()) + "\"," +
            "\"base\":\"" + escapeJson(draft.base()) + "\"" +
            "}";

        HttpRequest request = requestBuilder(uri)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        HttpResponse<String> response = sendWithRetry(request);

        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        if (status >= 200 && status < 300) {
            String numberValue = extract(PR_NUMBER_PATTERN, body);
            String urlValue = extract(PR_URL_PATTERN, body);
            if (numberValue == null || urlValue == null) {
                throw new GitHubApiException(GitHubApiException.Category.UNEXPECTED,
                    "GitHub response did not include PR number or URL", status);
            }
            return new PullRequestResult(Integer.parseInt(numberValue), URI.create(urlValue));
        }

        throw mapFailure(status, body, "Failed to create pull request");
    }

    private HttpRequest.Builder requestBuilder(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Content-Type", "application/json");
        tokenSupplier.get()
            .filter(token -> !token.isBlank())
            .ifPresent(token -> builder.header("Authorization", "Bearer " + token));
        return builder;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (isTransientStatus(status) && attempt < maxRetries) {
                    sleepBackoff(attempt);
                    continue;
                }
                return response;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new GitHubApiException(GitHubApiException.Category.NETWORK,
                    "GitHub request interrupted", -1, ex);
            } catch (IOException ex) {
                if (attempt >= maxRetries) {
                    throw new GitHubApiException(GitHubApiException.Category.NETWORK,
                        "GitHub request failed", -1, ex);
                }
                sleepBackoff(attempt);
            }
        }
    }

    private static boolean isTransientStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private static void sleepBackoff(int attempt) {
        long delayMillis = Math.min(2000L, 200L * (1L << Math.min(attempt, 4)));
        try {
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GitHubApiException(GitHubApiException.Category.NETWORK,
                "Retry interrupted", -1, ex);
        }
    }

    private static GitHubApiException mapFailure(int status, String body, String messagePrefix) {
        if (status == 401 || status == 403) {
            return new GitHubApiException(GitHubApiException.Category.AUTHENTICATION,
                messagePrefix + ": authentication failed", status);
        }
        if (status == 422) {
            return new GitHubApiException(GitHubApiException.Category.VALIDATION,
                messagePrefix + ": validation failed", status);
        }
        if (isTransientStatus(status)) {
            return new GitHubApiException(GitHubApiException.Category.TRANSIENT,
                messagePrefix + ": transient server error", status);
        }
        String compactBody = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (compactBody.length() > 120) {
            compactBody = compactBody.substring(0, 120);
        }
        String suffix = compactBody.isBlank() ? "" : " (" + compactBody + ")";
        return new GitHubApiException(GitHubApiException.Category.UNEXPECTED,
            messagePrefix + ": HTTP " + status + suffix, status);
    }

    private static String extract(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static String escapeJson(String value) {
        String text = value == null ? "" : value;
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
