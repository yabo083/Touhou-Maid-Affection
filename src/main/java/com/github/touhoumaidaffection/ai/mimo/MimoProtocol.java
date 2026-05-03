package com.github.touhoumaidaffection.ai.mimo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Base64;
import java.util.List;
import java.util.Locale;

public final class MimoProtocol {
    public static final String DEFAULT_URL = "https://api.xiaomimimo.com/v1/chat/completions";
    public static final String DEFAULT_CHAT_MODEL = "mimo-v2.5";
    public static final String DEFAULT_TTS_MODEL = "mimo-v2.5-tts-voicedesign";
    public static final String DEFAULT_TTS_VOICE_PROMPT = "温柔、清澈、亲近的年轻女性声音，语速自然，适合 Minecraft 女仆角色。";
    public static final String DEFAULT_TTS_AUDIO_FORMAT = "mp3";

    private static final Gson GSON = new Gson();

    private MimoProtocol() {
    }

    public static String buildChatRequest(String model, List<Message> messages, int maxCompletionTokens) {
        JsonObject root = baseRequest(model, maxCompletionTokens);
        JsonArray jsonMessages = new JsonArray();
        for (Message message : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            item.addProperty("content", message.content());
            jsonMessages.add(item);
        }
        root.add("messages", jsonMessages);
        return GSON.toJson(root);
    }

    public static String buildTtsRequest(String model, String voicePrompt, String text, String audioFormat) {
        return buildTtsRequest(model, voicePrompt, text, audioFormat, "");
    }

    public static String buildTtsRequest(String model, String voicePrompt, String text, String audioFormat, String language) {
        String normalizedLanguage = normalizeTtsLanguage(language);
        JsonObject root = new JsonObject();
        root.addProperty("model", blankToDefault(model, DEFAULT_TTS_MODEL));
        if (!normalizedLanguage.isBlank()) {
            root.addProperty("language", normalizedLanguage);
        }

        JsonArray messages = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", ttsVoicePromptWithLanguage(voicePrompt, normalizedLanguage));
        messages.add(user);

        JsonObject assistant = new JsonObject();
        assistant.addProperty("role", "assistant");
        assistant.addProperty("content", text == null ? "" : text);
        messages.add(assistant);
        root.add("messages", messages);

        JsonObject audio = new JsonObject();
        audio.addProperty("format", normalizeTtsAudioFormat(audioFormat));
        if (!normalizedLanguage.isBlank()) {
            audio.addProperty("language", normalizedLanguage);
        }
        root.add("audio", audio);
        return GSON.toJson(root);
    }

    public static String normalizeUrl(String raw) {
        String value = blankToDefault(raw, DEFAULT_URL).trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("api.xiaomimimo.com/") || value.startsWith("xiaomimimo.com/")) {
            return "https://" + value;
        }
        return value;
    }

    public static String extractFirstText(String responseBody) {
        JsonObject message = firstMessage(responseBody);
        if (message == null) {
            return "";
        }
        String content = getString(message, "content");
        if (!content.isBlank()) {
            return content;
        }
        return getString(message, "reasoning_content");
    }

    public static byte[] extractFirstAudio(String responseBody) {
        JsonObject message = firstMessage(responseBody);
        if (message == null || !message.has("audio") || !message.get("audio").isJsonObject()) {
            return new byte[0];
        }
        String encoded = getString(message.getAsJsonObject("audio"), "data");
        if (encoded.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(encoded);
    }

    private static JsonObject baseRequest(String model, int maxCompletionTokens) {
        JsonObject root = new JsonObject();
        root.addProperty("model", blankToDefault(model, DEFAULT_CHAT_MODEL));
        root.addProperty("max_completion_tokens", Math.max(1, maxCompletionTokens));
        return root;
    }

    private static JsonObject firstMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        JsonElement rootElement = JsonParser.parseString(responseBody);
        if (!rootElement.isJsonObject()) {
            return null;
        }
        JsonObject root = rootElement.getAsJsonObject();
        JsonArray choices = root.has("choices") && root.get("choices").isJsonArray()
                ? root.getAsJsonArray("choices")
                : new JsonArray();
        if (choices.isEmpty() || !choices.get(0).isJsonObject()) {
            return null;
        }
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        if (!firstChoice.has("message") || !firstChoice.get("message").isJsonObject()) {
            return null;
        }
        return firstChoice.getAsJsonObject("message");
    }

    private static String getString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static String blankToDefault(String raw, String fallback) {
        return raw == null || raw.isBlank() ? fallback : raw;
    }

    public static String normalizeTtsAudioFormat(String raw) {
        String value = blankToDefault(raw, DEFAULT_TTS_AUDIO_FORMAT).toLowerCase(Locale.ROOT).trim();
        if ("mp3".equals(value)) {
            return value;
        }
        return DEFAULT_TTS_AUDIO_FORMAT;
    }

    public static String normalizeTtsLanguage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        int underscore = value.indexOf('_');
        if (underscore > 0) {
            value = value.substring(0, underscore);
        }
        return switch (value) {
            case "zh", "en", "ja", "ko", "fr", "de", "es", "ru" -> value;
            default -> value;
        };
    }

    private static String ttsVoicePromptWithLanguage(String voicePrompt, String language) {
        String prompt = blankToDefault(voicePrompt, DEFAULT_TTS_VOICE_PROMPT);
        String languageName = switch (language) {
            case "zh" -> "Chinese";
            case "en" -> "English";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "fr" -> "French";
            case "de" -> "German";
            case "es" -> "Spanish";
            case "ru" -> "Russian";
            case "" -> "";
            default -> "language code '" + language + "'";
        };
        if (languageName.isBlank()) {
            return prompt;
        }
        return prompt + "\nLanguage override: Speak the assistant text in " + languageName
                + ". Keep the original meaning and do not translate voice-style instructions.";
    }

    public record Message(String role, String content) {
    }
}
