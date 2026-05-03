package com.github.touhoumaidaffection.bond.service;

import java.util.Locale;

final class MorningKissGeneratedDialogueLanguage {
    private MorningKissGeneratedDialogueLanguage() {
    }

    static String appendLanguageInstruction(String rawPrompt, String rawLanguage) {
        String prompt = rawPrompt == null ? "" : rawPrompt;
        String language = normalizeLocaleCode(rawLanguage);
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

    private static String normalizeLocaleCode(String rawLanguage) {
        if (rawLanguage == null || rawLanguage.isBlank()) {
            return "zh_cn";
        }
        return rawLanguage.trim().toLowerCase(Locale.ROOT).replace('-', '_');
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
                + ". Ignore conflicting language requirements in the template. Put one line per candidate; do not number, explain, repeat, or wrap lines in quotes.";
    }
}
