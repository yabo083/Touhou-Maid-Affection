package com.github.touhoumaidaffection.ai.mimo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MimoProtocolTest {
    @Test
    void extractsAssistantContentFromOpenAiStyleResponse() {
        String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "早安，主人。"
                      }
                    }
                  ]
                }
                """;

        assertEquals("早安，主人。", MimoProtocol.extractFirstText(response));
    }

    @Test
    void extractsReasoningContentWhenContentIsEmpty() {
        String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "",
                        "reasoning_content": "测试转写"
                      }
                    }
                  ]
                }
                """;

        assertEquals("测试转写", MimoProtocol.extractFirstText(response));
    }

    @Test
    void decodesTtsAudioDataFromMessageAudio() {
        byte[] audio = "wav-bytes".getBytes(StandardCharsets.UTF_8);
        String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "audio": {
                          "data": "%s"
                        }
                      }
                    }
                  ]
                }
                """.formatted(Base64.getEncoder().encodeToString(audio));

        assertArrayEquals(audio, MimoProtocol.extractFirstAudio(response));
    }

    @Test
    void buildsTtsRequestWithVoiceDesignUserPromptAndAssistantText() {
        String request = MimoProtocol.buildTtsRequest("mimo-v2.5-tts-voicedesign", "温柔女声", "欢迎回来", "");

        JsonObject root = JsonParser.parseString(request).getAsJsonObject();
        JsonArray messages = root.getAsJsonArray("messages");
        assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("温柔女声", messages.get(0).getAsJsonObject().get("content").getAsString());
        assertEquals("assistant", messages.get(1).getAsJsonObject().get("role").getAsString());
        assertEquals("欢迎回来", messages.get(1).getAsJsonObject().get("content").getAsString());
        assertEquals("mp3", root.getAsJsonObject("audio").get("format").getAsString());
    }

    @Test
    void buildsTtsRequestWithExplicitLanguageInstruction() {
        String request = MimoProtocol.buildTtsRequest("mimo-v2.5-tts-voicedesign", "温柔女声", "Welcome back", "", "en");

        JsonObject root = JsonParser.parseString(request).getAsJsonObject();
        JsonArray messages = root.getAsJsonArray("messages");
        assertEquals("en", root.get("language").getAsString());
        assertEquals("en", root.getAsJsonObject("audio").get("language").getAsString());
        String userPrompt = messages.get(0).getAsJsonObject().get("content").getAsString();
        assertEquals(true, userPrompt.contains("English"));
        assertEquals(true, userPrompt.contains("Speak"));
    }

    @Test
    void normalizesUnsupportedTtsAudioFormatToMp3() {
        String request = MimoProtocol.buildTtsRequest("mimo-v2.5-tts-voicedesign", "温柔女声", "欢迎回来", "ogg");

        JsonObject root = JsonParser.parseString(request).getAsJsonObject();
        assertEquals("mp3", root.getAsJsonObject("audio").get("format").getAsString());
    }

    @Test
    void buildsPlainChatRequest() {
        String request = MimoProtocol.buildChatRequest(
                "mimo-v2.5",
                List.of(new MimoProtocol.Message("user", "你好")),
                256
        );

        JsonObject root = JsonParser.parseString(request).getAsJsonObject();
        assertEquals("mimo-v2.5", root.get("model").getAsString());
        assertEquals("你好", root.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
    }
}
