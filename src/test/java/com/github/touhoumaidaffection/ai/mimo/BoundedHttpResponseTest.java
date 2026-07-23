package com.github.touhoumaidaffection.ai.mimo;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedHttpResponseTest {
    @Test
    void acceptsResponseBodiesAtTheConfiguredLimit() {
        HttpResponse.BodySubscriber<String> subscriber = subscriber(4);

        subscriber.onSubscribe(NOOP_SUBSCRIPTION);
        subscriber.onNext(List.of(ByteBuffer.wrap("1234".getBytes(StandardCharsets.UTF_8))));
        subscriber.onComplete();

        assertEquals("1234", subscriber.getBody().toCompletableFuture().join());
    }

    @Test
    void rejectsResponseBodiesBeforeTheyExceedTheConfiguredLimit() {
        HttpResponse.BodySubscriber<String> subscriber = subscriber(4);

        subscriber.onSubscribe(NOOP_SUBSCRIPTION);
        subscriber.onNext(List.of(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8))));

        assertThrows(CompletionException.class, () -> subscriber.getBody().toCompletableFuture().join());
    }

    @Test
    void rejectsKnownOversizedResponsesFromContentLength() {
        HttpResponse.BodySubscriber<String> subscriber = subscriber(4, Map.of("Content-Length", List.of("5")));

        subscriber.onSubscribe(NOOP_SUBSCRIPTION);

        assertThrows(CompletionException.class, () -> subscriber.getBody().toCompletableFuture().join());
    }

    private static HttpResponse.BodySubscriber<String> subscriber(long maxBytes) {
        return subscriber(maxBytes, Map.of());
    }

    private static HttpResponse.BodySubscriber<String> subscriber(long maxBytes, Map<String, List<String>> headers) {
        return BoundedHttpResponse.limit(HttpResponse.BodyHandlers.ofString(), maxBytes).apply(new HttpResponse.ResponseInfo() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(headers, (name, value) -> true);
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        });
    }

    private static final Flow.Subscription NOOP_SUBSCRIPTION = new Flow.Subscription() {
        @Override
        public void request(long count) {
        }

        @Override
        public void cancel() {
        }
    };
}
