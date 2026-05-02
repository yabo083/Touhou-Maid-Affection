package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.TTSCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MimoTTSClient implements TTSClient {
    private final HttpClient httpClient;
    private final MimoTTSSite site;

    public MimoTTSClient(HttpClient httpClient, MimoTTSSite site) {
        this.httpClient = httpClient;
        this.site = site;
    }

    @Override
    public void play(String text, TTSConfig config, TTSCallback callback) {
        String model = config == null || config.model() == null || config.model().isBlank()
                ? MimoProtocol.DEFAULT_TTS_MODEL
                : config.model();
        String body = MimoProtocol.buildTtsRequest(model, site.voicePrompt(), text, site.audioFormat());
        HttpRequest request = MimoHttp.requestBuilder(site.url(), site.secretKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> handle(callback, response, error, request));
    }

    private void handle(ResponseCallback<byte[]> callback, HttpResponse<String> response, Throwable error, HttpRequest request) {
        if (error != null) {
            callback.onFailure(request, error, 0);
            return;
        }
        if (!isSuccessful(response)) {
            callback.onFailure(request, new Throwable("HTTP Error Code: %d, Response: %s".formatted(response.statusCode(), response.body())), 1);
            return;
        }
        try {
            callback.onSuccess(MimoProtocol.extractFirstAudio(response.body()));
        } catch (RuntimeException ex) {
            callback.onFailure(request, ex, 2);
        }
    }
}
