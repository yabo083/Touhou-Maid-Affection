package com.github.touhoumaidaffection.bond.service;

/**
 * 早安吻 AI 缓存条目的语种匹配规则（纯逻辑，便于单测）。
 *
 * <p>条目匹配当前目标的规则：</p>
 * <ul>
 *     <li>文本语种：{@code normalizeChat(条目.textLanguage) == normalizeChat(目标显示语种)}；</li>
 *     <li>配音语种：条目为纯文本（{@code voiceLanguage} 为空）时不约束配音语种，
 *     否则要求 {@code normalizeTts(条目.voiceLanguage) == normalizeTts(目标配音文本语种)}。</li>
 * </ul>
 *
 * <p>目标语种为空串（未配置 / {@code auto} 未解析出）时视为不过滤，保持旧行为。</p>
 */
final class MorningKissCacheLanguageMatch {
    private MorningKissCacheLanguageMatch() {
    }

    static boolean matches(MorningKissGeneratedDialogueCache.Entry entry,
                           String targetTextLanguage,
                           String targetVoiceLanguage) {
        if (entry == null) {
            return false;
        }
        String targetText = MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForChat(targetTextLanguage);
        if (!targetText.isBlank()
                && !MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForChat(entry.textLanguage()).equals(targetText)) {
            return false;
        }
        String targetVoice = MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts(targetVoiceLanguage);
        if (targetVoice.isBlank()) {
            return true;
        }
        String entryVoice = MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts(entry.voiceLanguage());
        return entryVoice.isBlank() || entryVoice.equals(targetVoice);
    }
}