package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorningKissGeneratedDialogueCacheTest {
    @Test
    void normalizesMultipleGeneratedLines() {
        List<String> lines = MorningKissGeneratedDialogueCache.normalizeLines("""
                1. "早呀，今天也要一起努力哦。"
                - 主人，醒来了吗？

                这是一句非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的台词。
                """);

        assertEquals(List.of("早呀，今天也要一起努力哦。", "主人，醒来了吗？"), lines);
    }

    @Test
    void storesEntriesPerMaidAndDialoguePoolWithBoundedCapacity() {
        MorningKissGeneratedDialogueCache cache = new MorningKissGeneratedDialogueCache(2);
        UUID maidUuid = UUID.randomUUID();

        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第一句", "第一句", "", new byte[0]));
        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第二句", "第二句", "", new byte[0]));
        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第三句", "第三句", "", new byte[0]));

        assertEquals(2, cache.size(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING));
        Optional<MorningKissGeneratedDialogueCache.Entry> first =
                cache.pollFirst(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING);
        Optional<MorningKissGeneratedDialogueCache.Entry> second =
                cache.pollFirst(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING);

        assertEquals("第二句", first.orElseThrow().text());
        assertEquals("第三句", second.orElseThrow().text());
        assertFalse(cache.pollFirst(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING).isPresent());
    }

    @Test
    void clearsEntriesForOneMaidOrEntireCache() {
        MorningKissGeneratedDialogueCache cache = new MorningKissGeneratedDialogueCache(2);
        UUID firstMaid = UUID.randomUUID();
        UUID secondMaid = UUID.randomUUID();

        cache.add(firstMaid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第一句", "第一句", "", new byte[0]));
        cache.add(firstMaid, MorningKissScheduleRules.DialoguePool.EVENING,
                new MorningKissGeneratedDialogueCache.Entry("第二句", "第二句", "", new byte[0]));
        cache.add(secondMaid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第三句", "第三句", "", new byte[0]));

        assertEquals(2, cache.clear(firstMaid));
        assertEquals(0, cache.size(firstMaid, MorningKissScheduleRules.DialoguePool.MORNING));
        assertEquals(1, cache.size(secondMaid, MorningKissScheduleRules.DialoguePool.MORNING));

        assertEquals(1, cache.clearAll());
        assertEquals(0, cache.size(secondMaid, MorningKissScheduleRules.DialoguePool.MORNING));
    }

    @Test
    void acceptsTlmPlayableBytesForGeneratedVoiceCache() {
        assertEquals(Optional.of("ogg"), MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(
                "OggSdata".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.of("mp3"), MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(
                new byte[] {(byte) 0xFF, (byte) 0xFB, 0x10, 0x44}));
        assertEquals(Optional.empty(), MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(
                "RIFFdata".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.empty(), MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(
                new byte[]{'O', 'g', 'g'}));
    }

    @Test
    void appliesConfiguredLanguageToPregeneratedDialogueAndTts() {
        String prompt = MorningKissGeneratedDialogueLanguage.appendLanguageInstruction("Base prompt", "en_us");

        assertTrue(prompt.contains("English"));
        assertTrue(prompt.contains("one line"));
        assertEquals("en", MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts("en_us"));
        assertEquals("zh", MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts("zh_cn"));
        assertEquals("ja", MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts("ja_jp"));
    }
}
