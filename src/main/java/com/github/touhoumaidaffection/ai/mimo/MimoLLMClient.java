package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAIClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.ChatCompletionResponse;
import com.google.gson.Gson;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

final class MimoLLMClient extends LLMOpenAIClient {
    private static final Gson GSON = new Gson();

    MimoLLMClient(HttpClient httpClient, MimoLLMSite site) {
        super(httpClient, site);
    }

    @Override
    protected void handle(
            LLMCallback callback,
            HttpResponse<String> response,
            Throwable error,
            HttpRequest request
    ) {
        HttpResponse<String> safeResponse = response;
        if (response != null && shouldSummarize(response)) {
            String safeBody = response.statusCode() >= 200 && response.statusCode() < 300
                    ? "{}"
                    : MimoHttp.summarizeErrorBody(response.body());
            safeResponse = new StringBodyResponse(response, safeBody);
        }
        super.handle(callback, safeResponse, error, request);
    }

    private static boolean shouldSummarize(HttpResponse<String> response) {
        return shouldSummarize(response.statusCode(), response.body());
    }

    static boolean shouldSummarize(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            return true;
        }
        try {
            ChatCompletionResponse parsed = GSON.fromJson(body, ChatCompletionResponse.class);
            return parsed == null || parsed.getFirstChoice() == null;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private record StringBodyResponse(HttpResponse<String> delegate, String body) implements HttpResponse<String> {
        @Override
        public int statusCode() {
            return delegate.statusCode();
        }

        @Override
        public HttpRequest request() {
            return delegate.request();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return delegate.previousResponse();
        }

        @Override
        public HttpHeaders headers() {
            return delegate.headers();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return delegate.sslSession();
        }

        @Override
        public URI uri() {
            return delegate.uri();
        }

        @Override
        public HttpClient.Version version() {
            return delegate.version();
        }
    }
}
