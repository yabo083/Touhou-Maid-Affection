package com.github.touhoumaidaffection.ai.mimo;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

final class BoundedHttpResponse {
    private BoundedHttpResponse() {
    }

    static <T> HttpResponse.BodyHandler<T> limit(HttpResponse.BodyHandler<T> delegate, long maxBytes) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        return responseInfo -> {
            long contentLength = responseInfo.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (contentLength > maxBytes) {
                return new RejectedBodySubscriber<>(maxBytes);
            }
            return new LimitedBodySubscriber<>(delegate.apply(responseInfo), maxBytes);
        };
    }

    private static final class RejectedBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
        private final CompletableFuture<T> body;

        private RejectedBodySubscriber(long maxBytes) {
            body = CompletableFuture.failedFuture(responseTooLarge(maxBytes));
        }

        @Override
        public CompletionStage<T> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.cancel();
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }

    private static final class LimitedBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
        private final HttpResponse.BodySubscriber<T> delegate;
        private final long maxBytes;
        private Flow.Subscription subscription;
        private long receivedBytes;
        private boolean done;

        private LimitedBodySubscriber(HttpResponse.BodySubscriber<T> delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<T> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (done) {
                return;
            }
            long chunkBytes = 0;
            for (ByteBuffer buffer : buffers) {
                chunkBytes += buffer.remaining();
                if (chunkBytes > maxBytes - receivedBytes) {
                    rejectOversizedResponse();
                    return;
                }
            }
            receivedBytes += chunkBytes;
            delegate.onNext(buffers);
        }

        @Override
        public void onError(Throwable throwable) {
            if (done) {
                return;
            }
            done = true;
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            delegate.onComplete();
        }

        private void rejectOversizedResponse() {
            done = true;
            if (subscription != null) {
                subscription.cancel();
            }
            delegate.onError(responseTooLarge(maxBytes));
        }
    }

    private static IllegalStateException responseTooLarge(long maxBytes) {
        return new IllegalStateException("HTTP response exceeded %d bytes".formatted(maxBytes));
    }
}
