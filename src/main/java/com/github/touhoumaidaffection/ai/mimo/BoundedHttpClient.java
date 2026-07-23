package com.github.touhoumaidaffection.ai.mimo;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class BoundedHttpClient extends HttpClient {
    private final HttpClient delegate;
    private final long maxResponseBytes;

    BoundedHttpClient(HttpClient delegate, long maxResponseBytes) {
        this.delegate = delegate;
        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        return delegate.send(request, BoundedHttpResponse.limit(responseBodyHandler, maxResponseBytes));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
    ) {
        return delegate.sendAsync(request, BoundedHttpResponse.limit(responseBodyHandler, maxResponseBytes));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler
    ) {
        HttpResponse.PushPromiseHandler<T> boundedPushHandler = (initiatingRequest, pushPromiseRequest, acceptor) ->
                pushPromiseHandler.applyPushPromise(
                        initiatingRequest,
                        pushPromiseRequest,
                        pushedBodyHandler -> acceptor.apply(
                                BoundedHttpResponse.limit(pushedBodyHandler, maxResponseBytes)
                        )
                );
        return delegate.sendAsync(
                request,
                BoundedHttpResponse.limit(responseBodyHandler, maxResponseBytes),
                boundedPushHandler
        );
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        return delegate.newWebSocketBuilder();
    }

    @Override
    public void shutdown() {
        // This adapter does not own the shared TLM HTTP client.
    }

    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void shutdownNow() {
    }

    @Override
    public void close() {
    }
}
