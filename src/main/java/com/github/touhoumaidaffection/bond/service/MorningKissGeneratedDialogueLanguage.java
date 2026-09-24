package com.github.touhoumaidaffection.bond.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MorningKissGeneratedDialogueLanguage {
    private static final Gson GSON = new Gson();

    private MorningKissGeneratedDialogueLanguage() {
    }

    static String appendLanguageInstruction(String rawPrompt, String rawLanguage) {
        String prompt = rawPrompt == null ? "" : rawPrompt;
        String language = normalizeLocaleCode(rawLanguage);
        if (language.isBlank()) {
            return prompt + "\n请输出 3 句候选台词，每句单独一行，不要编号，不要解释，不要重复，不要加引号。";
        }
        return prompt + "\n" + generationInstruction(language);
    }

    static String normalizeLanguageCodeForTts(String rawLanguage) {
        String language = normalizeLocaleCode(rawLanguage);
        int underscore = language.indexOf('_');
        if (underscore > 0) {
            return language.substring(0, underscore);
        }
        return language;
    }

    static String normalizeLanguageCodeForChat(String rawLanguage) {
        return normalizeLocaleCode(rawLanguage);
    }

    /**
     * 显示文本语种取值链：AI 专用显式 locale → 全局显式 locale → TLM 聊天语言 → TLM TTS 语言。
     */
    static String resolveGeneratedTextLanguage(String configuredLanguage, String globalLanguage,
                                               String tlmTtsLanguage, String tlmChatLanguage) {
        String configured = normalizeLanguageCodeForChat(configuredLanguage);
        if (!configured.isBlank()) {
            return configured;
        }
        String global = normalizeLanguageCodeForChat(globalLanguage);
        if (!global.isBlank()) {
            return global;
        }
        String chat = normalizeLanguageCodeForChat(tlmChatLanguage);
        if (!chat.isBlank()) {
            return chat;
        }
        return normalizeLanguageCodeForChat(tlmTtsLanguage);
    }

    /**
     * 配音文本语种取值链：AI 专用显式 locale → 全局显式配音 locale → 继承（AI 显示 locale → 全局显示 locale）→ TLM 语言。
     *
     * <p>{@code inherit} 保留旧语义：优先继承显式显示语种，否则继续向下回退到 TLM。
     * {@code tlm}/{@code auto}/{@code default} 视为未指定，在全局配置缺省时跟随 TLM。</p>
     */
    static String resolveGeneratedVoiceTextLanguage(
            String configuredVoiceLanguage,
            String configuredTextLanguage,
            String globalVoiceLanguage,
            String globalDisplayLanguage,
            String tlmTtsLanguage,
            String tlmChatLanguage
    ) {
        String rawVoice = configuredVoiceLanguage == null
                ? ""
                : configuredVoiceLanguage.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        boolean inheritTextLanguage = rawVoice.isBlank() || "inherit".equals(rawVoice);
        if (!inheritTextLanguage) {
            String configuredVoice = normalizeLanguageCodeForChat(rawVoice);
            if (!configuredVoice.isBlank()) {
                return configuredVoice;
            }
        }
        String globalVoice = normalizeLanguageCodeForChat(globalVoiceLanguage);
        if (!globalVoice.isBlank()) {
            return globalVoice;
        }
        if (inheritTextLanguage) {
            String configuredText = normalizeLanguageCodeForChat(configuredTextLanguage);
            if (!configuredText.isBlank()) {
                return configuredText;
            }
            String globalDisplay = normalizeLanguageCodeForChat(globalDisplayLanguage);
            if (!globalDisplay.isBlank()) {
                return globalDisplay;
            }
        }
        String tts = normalizeLanguageCodeForChat(tlmTtsLanguage);
        if (!tts.isBlank()) {
            return tts;
        }
        return normalizeLanguageCodeForChat(tlmChatLanguage);
    }

    static boolean requiresTranslation(String textLanguage, String voiceLanguage) {
        String text = normalizeLanguageCodeForChat(textLanguage);
        String voice = normalizeLanguageCodeForChat(voiceLanguage);
        if (text.isBlank() || voice.isBlank() || text.equals(voice)) {
            return false;
        }
        String textBase = normalizeLanguageCodeForTts(text);
        String voiceBase = normalizeLanguageCodeForTts(voice);
        if (!textBase.equals(voiceBase)) {
            return true;
        }
        return text.indexOf('_') > 0 && voice.indexOf('_') > 0;
    }

    static String buildVoiceTranslationPrompt(List<String> displayLines, String targetLanguage) {
        List<String> safeLines = displayLines == null
                ? List.of()
                : displayLines.stream().filter(line -> line != null && !line.isBlank()).map(String::trim).toList();
        String language = normalizeLanguageCodeForChat(targetLanguage);
        return "Translate each Minecraft maid dialogue line into natural " + languageName(language) + ". "
                + "Preserve meaning, tone, names, and line order. Return only a JSON array containing exactly "
                + safeLines.size() + " translated strings. Do not add explanations, numbering, markdown, or extra lines.\n"
                + GSON.toJson(safeLines);
    }

    static List<String> parseVoiceTranslations(String raw, int expectedCount) {
        if (raw == null || raw.isBlank() || expectedCount < 1) {
            return List.of();
        }
        String value = stripCodeFence(raw.trim());
        try {
            JsonElement root = JsonParser.parseString(value);
            if (!root.isJsonArray()) {
                return List.of();
            }
            JsonArray array = root.getAsJsonArray();
            if (array.size() != expectedCount) {
                return List.of();
            }
            List<String> translations = new ArrayList<>(expectedCount);
            for (JsonElement element : array) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                    return List.of();
                }
                String line = MorningKissGeneratedDialogueCache.normalizeLine(element.getAsString());
                if (line.isBlank()) {
                    return List.of();
                }
                translations.add(line);
            }
            return List.copyOf(translations);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    static String systemInstruction(String rawLanguage) {
        String language = normalizeLocaleCode(rawLanguage);
        if (language.isBlank()) {
            return "你正在生成 Minecraft 早安吻台词缓存。";
        }
        return "You are generating Minecraft Morning Kiss dialogue cache. "
                + generationInstruction(language);
    }

    static String normalizeLocaleCode(String rawLanguage) {
        if (rawLanguage == null || rawLanguage.isBlank()) {
            return "";
        }
        String value = rawLanguage.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (value) {
            case "tlm", "auto", "default" -> "";
            default -> value;
        };
    }

    private static String generationInstruction(String language) {
        return "Language override: output exactly 3 candidate lines in " + languageName(language)
                + ". This language override has higher priority than the template, including any request for Chinese or another language. Keep each line under 18 words. Put one line per candidate; do not number, explain, repeat, translate the instruction text, or wrap lines in quotes.";
    }

    private static String languageName(String language) {
        String normalized = normalizeLanguageCodeForChat(language);
        return switch (normalized) {
            case "zh_cn", "zh_sg" -> "Simplified Chinese";
            case "zh_tw", "zh_hk", "zh_mo" -> "Traditional Chinese";
            case "en_us" -> "American English";
            case "en_gb" -> "British English";
            default -> switch (normalizeLanguageCodeForTts(normalized)) {
            case "en" -> "English";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "zh" -> "Chinese";
            case "fr" -> "French";
            case "de" -> "German";
            case "es" -> "Spanish";
            case "ru" -> "Russian";
            default -> "the language represented by locale code '" + language + "'";
            };
        };
    }

    private static String stripCodeFence(String raw) {
        if (!raw.startsWith("```")) {
            return raw;
        }
        int firstLineEnd = raw.indexOf('\n');
        int closingFence = raw.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
            return raw;
        }
        if (!raw.substring(closingFence + 3).isBlank()) {
            return raw;
        }
        return raw.substring(firstLineEnd + 1, closingFence).trim();
    }
}
