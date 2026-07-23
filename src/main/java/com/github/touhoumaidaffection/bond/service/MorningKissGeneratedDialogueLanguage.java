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

    static String resolveGeneratedVoiceTextLanguage(
            String configuredVoiceLanguage,
            String configuredTextLanguage,
            String tlmTtsLanguage,
            String tlmChatLanguage
    ) {
        String rawVoice = configuredVoiceLanguage == null
                ? ""
                : configuredVoiceLanguage.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        boolean inheritTextLanguage = rawVoice.isBlank() || "inherit".equals(rawVoice);
        if (inheritTextLanguage) {
            String configuredText = normalizeLanguageCodeForChat(configuredTextLanguage);
            if (!configuredText.isBlank()) {
                return configuredText;
            }
        } else {
            String configuredVoice = normalizeLanguageCodeForChat(rawVoice);
            if (!configuredVoice.isBlank()) {
                return configuredVoice;
            }
        }
        String tts = normalizeLanguageCodeForChat(tlmTtsLanguage);
        if (!tts.isBlank()) {
            return tts;
        }
        return normalizeLanguageCodeForChat(tlmChatLanguage);
    }

    static boolean requiresTranslation(String textLanguage, String voiceLanguage) {
        String text = normalizeLanguageCodeForTts(textLanguage);
        String voice = normalizeLanguageCodeForTts(voiceLanguage);
        return !text.isBlank() && !voice.isBlank() && !text.equals(voice);
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
        return "Language override: output exactly 3 candidate lines in " + languageName(language)
                + ". This language override has higher priority than the template, including any request for Chinese or another language. Keep each line under 18 words. Put one line per candidate; do not number, explain, repeat, translate the instruction text, or wrap lines in quotes.";
    }

    private static String languageName(String language) {
        return switch (normalizeLanguageCodeForTts(language)) {
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
