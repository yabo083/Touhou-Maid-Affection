package com.github.touhoumaidaffection.ai.mimo;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

final class MimoHttp {
    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(30);

    private MimoHttp() {
    }

    static HttpRequest.Builder requestBuilder(String url, String apiKey) {
        String safeApiKey = apiKey == null ? "" : apiKey;
        return HttpRequest.newBuilder()
                .uri(URI.create(MimoProtocol.normalizeUrl(url)))
                .timeout(MAX_TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + safeApiKey)
                .header("api-key", safeApiKey);
    }
}
