package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.touhoumaidaffection.ModConfig;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class MimoLLMClient implements LLMClient {
    private final HttpClient httpClient;
    private final MimoLLMSite site;

    public MimoLLMClient(HttpClient httpClient, MimoLLMSite site) {
        this.httpClient = httpClient;
        this.site = site;
    }

    @Override
    public void chat(LLMCallback callback) {
        String selectedModel = callback.getMaid() == null
                ? ""
                : callback.getMaid().getAiChatManager().getLLMModel();
        String model = selectedModel == null || selectedModel.isBlank()
                ? MimoProtocol.DEFAULT_CHAT_MODEL
                : selectedModel;
        List<MimoProtocol.Message> mimoMessages = callback.getMessages().stream()
                .map(message -> new MimoProtocol.Message(message.role().getId(), message.message()))
                .toList();
        String body = MimoProtocol.buildChatRequest(
                model,
                mimoMessages,
                ModConfig.TMA_MIMO_MAX_COMPLETION_TOKENS.get()
        );
        HttpRequest.Builder builder = MimoHttp.requestBuilder(site.url(), site.secretKey())
                .POST(HttpRequest.BodyPublishers.ofString(body));
        site.headers().forEach(builder::header);
        HttpRequest request = builder.build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> handle(callback, response, error, request));
    }

    private void handle(LLMCallback callback, HttpResponse<String> response, Throwable error, HttpRequest request) {
        if (error != null) {
            callback.onFailure(request, error, 0);
            return;
        }
        if (!isSuccessful(response)) {
            callback.onFailure(request, new Throwable("HTTP Error Code: %d, Response: %s".formatted(response.statusCode(), response.body())), 1);
            return;
        }
        try {
            callback.onSuccess(toResponseChat(MimoProtocol.extractFirstText(response.body())));
        } catch (RuntimeException ex) {
            callback.onFailure(request, ex, 2);
        }
    }

    static ResponseChat toResponseChat(String text) {
        return new ResponseChat(text);
    }
}
