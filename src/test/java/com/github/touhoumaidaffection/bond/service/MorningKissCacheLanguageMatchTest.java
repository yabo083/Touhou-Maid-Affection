package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorningKissCacheLanguageMatchTest {
    private static MorningKissGeneratedDialogueCache.Entry textOnly(String text, String textLanguage) {
        return new MorningKissGeneratedDialogueCache.Entry(text, text, "", new byte[0], textLanguage, "");
    }

    private static MorningKissGeneratedDialogueCache.Entry voiced(String text, String textLanguage, String voiceLanguage) {
        return new MorningKissGeneratedDialogueCache.Entry(text, text, "voice.ogg",
                "OggSdata".getBytes(StandardCharsets.US_ASCII), textLanguage, voiceLanguage);
    }

    @Test
    void matchesTextLanguageAfterChatNormalization() {
        assertTrue(MorningKissCacheLanguageMatch.matches(textOnly("你好", "zh_cn"), "zh_cn", ""));
        // 大小写与 -/_ 在归一化后等价。
        assertTrue(MorningKissCacheLanguageMatch.matches(textOnly("你好", "zh-CN"), "zh_cn", ""));
        assertFalse(MorningKissCacheLanguageMatch.matches(textOnly("你好", "zh_cn"), "ja_jp", ""));
    }

    @Test
    void comparesVoiceLanguageAfterTtsNormalizationOnBothSides() {
        // 条目存 TTS 归一化码 ja，目标给完整 locale ja_jp：两边归一化后相等。
        assertTrue(MorningKissCacheLanguageMatch.matches(voiced("こんにちは", "ja_jp", "ja"), "ja_jp", "ja_jp"));
        // 反向不对称：条目存完整 locale，目标是短码。
        assertTrue(MorningKissCacheLanguageMatch.matches(voiced("こんにちは", "ja_jp", "ja_jp"), "ja_jp", "ja"));
        assertFalse(MorningKissCacheLanguageMatch.matches(voiced("こんにちは", "ja_jp", "ja"), "ja_jp", "en"));
    }

    @Test
    void ignoresVoiceLanguageForTextOnlyEntries() {
        assertTrue(MorningKissCacheLanguageMatch.matches(textOnly("你好", "zh_cn"), "zh_cn", "ja_jp"));
        assertTrue(MorningKissCacheLanguageMatch.matches(textOnly("你好", "zh_cn"), "zh_cn", "en"));
    }

    @Test
    void blankTargetLanguageDisablesFiltering() {
        assertTrue(MorningKissCacheLanguageMatch.matches(textOnly("你好", "zh_cn"), "", ""));
        assertTrue(MorningKissCacheLanguageMatch.matches(voiced("こんにちは", "ja_jp", "ja"), "", ""));
        // 仅文本目标为空时，配音维度仍不约束。
        assertTrue(MorningKissCacheLanguageMatch.matches(voiced("こんにちは", "ja_jp", "ja"), "", "ja"));
    }

    @Test
    void rejectsNullEntry() {
        assertFalse(MorningKissCacheLanguageMatch.matches(null, "zh_cn", "zh"));
    }
}