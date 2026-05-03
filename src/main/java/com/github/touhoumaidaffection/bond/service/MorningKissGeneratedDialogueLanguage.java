package com.github.touhoumaidaffection.bond.service;

import java.util.Locale;

final class MorningKissGeneratedDialogueLanguage {
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

    static String resolveGeneratedTextLanguage(String configuredLanguage, String tlmTtsLanguage, String tlmChatLanguage) {
        String configured = normalizeLanguageCodeForChat(configuredLanguage);
        if (!configured.isBlank()) {
            return configured;
        }
        String chat = normalizeLanguageCodeForChat(tlmChatLanguage);
        if (!chat.isBlank()) {
            return chat;
        }
        return normalizeLanguageCodeForChat(tlmTtsLanguage);
    }

    static String resolveGeneratedVoiceTextLanguage(String configuredLanguage, String tlmTtsLanguage, String tlmChatLanguage) {
        String configured = normalizeLanguageCodeForChat(configuredLanguage);
        if (!configured.isBlank()) {
            return configured;
        }
        String tts = normalizeLanguageCodeForChat(tlmTtsLanguage);
        if (!tts.isBlank()) {
            return tts;
        }
        return normalizeLanguageCodeForChat(tlmChatLanguage);
    }

    static String systemInstruction(String rawLanguage) {
        String language = normalizeLocaleCode(rawLanguage);
        if (language.isBlank()) {
            return "你正在生成 Minecraft 早安吻台词缓存。";
        }
        return "You are generating Minecraft Morning Kiss dialogue cache. "
                + generationInstruction(language);
    }

    private static String normalizeLocaleCode(String rawLanguage) {
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
        String languageName = switch (normalizeLanguageCodeForTts(language)) {
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
        return "Language override: output exactly 3 candidate lines in " + languageName
                + ". This language override has higher priority than the template, including any request for Chinese or another language. Keep each line under 18 words. Put one line per candidate; do not number, explain, repeat, translate the instruction text, or wrap lines in quotes.";
    }
}
