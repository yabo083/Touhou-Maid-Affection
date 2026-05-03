package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void acceptsConciseEnglishGeneratedLines() {
        List<String> lines = MorningKissGeneratedDialogueCache.normalizeLines("""
                Good morning, master. I hope today treats you gently.
                I saved a warm smile for you before the day begins.
                """);

        assertEquals(List.of(
                "Good morning, master. I hope today treats you gently.",
                "I saved a warm smile for you before the day begins."
        ), lines);
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
    void clearsEntriesForOnePoolOrOneEntryAndCanStripVoiceOnly() {
        MorningKissGeneratedDialogueCache cache = new MorningKissGeneratedDialogueCache(4);
        UUID maidUuid = UUID.randomUUID();

        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第一句", "第一句", "first.ogg",
                        "OggSdata".getBytes(StandardCharsets.US_ASCII)));
        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第二句", "第二句", "second.ogg",
                        "OggSdata".getBytes(StandardCharsets.US_ASCII)));
        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.EVENING,
                new MorningKissGeneratedDialogueCache.Entry("第三句", "第三句", "", new byte[0]));

        assertTrue(cache.clearVoiceAt(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING, 1));
        MorningKissGeneratedDialogueCache.Entry voiceCleared =
                cache.entryAt(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING, 1).orElseThrow();
        assertEquals("第二句", voiceCleared.text());
        assertFalse(voiceCleared.hasVoice());

        assertEquals(1, cache.removeAt(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING, 0));
        assertEquals("第二句", cache.pollFirst(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING)
                .orElseThrow().text());

        assertEquals(1, cache.clear(maidUuid, MorningKissScheduleRules.DialoguePool.EVENING));
        assertEquals(0, cache.size(maidUuid, MorningKissScheduleRules.DialoguePool.EVENING));
    }

    @Test
    void reportsCacheStatistics() {
        MorningKissGeneratedDialogueCache cache = new MorningKissGeneratedDialogueCache(2);
        UUID firstMaid = UUID.randomUUID();
        UUID secondMaid = UUID.randomUUID();

        cache.add(firstMaid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第一句", "第一句", "", new byte[0], "zh_cn", "", "灵梦"));
        cache.add(firstMaid, MorningKissScheduleRules.DialoguePool.EVENING,
                new MorningKissGeneratedDialogueCache.Entry("第二句", "第二句", "voice.ogg", "OggSdata".getBytes(StandardCharsets.US_ASCII), "en_us", "en", "灵梦"));
        cache.add(secondMaid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("第三句", "第三句", "", new byte[0], "ja_jp", "", "魔理沙"));

        MorningKissGeneratedDialogueCache.Stats stats = cache.stats(7L, 2);

        assertEquals(3, stats.totalEntries());
        assertEquals(2, stats.maidCount());
        assertEquals(1, stats.voiceEntries());
        assertEquals(7L, stats.revision());
        assertEquals(2, stats.inFlightRequests());
        assertEquals(2, stats.maids().size());
        MorningKissGeneratedDialogueCache.MaidStats first = stats.maids().stream()
                .filter(maid -> maid.maidName().equals("灵梦"))
                .findFirst()
                .orElseThrow();
        assertEquals(2, first.totalEntries());
        assertEquals(2, first.pools().size());
        assertTrue(first.pools().stream().anyMatch(pool ->
                pool.pool() == MorningKissScheduleRules.DialoguePool.EVENING
                        && pool.textLanguage().equals("en_us")
                        && pool.voiceLanguage().equals("en")
                        && pool.voiceEntries() == 1));
    }

    @Test
    void trimsCandidateLinesToRemainingPoolCapacity() {
        List<String> lines = List.of("第一句", "第二句", "第三句");

        assertEquals(List.of("第一句"), MorningKissGeneratedDialogueCache.limitToRemainingCapacity(lines, 3, 4));
        assertEquals(List.of("第一句", "第二句", "第三句"), MorningKissGeneratedDialogueCache.limitToRemainingCapacity(lines, 0, 4));
        assertEquals(List.of(), MorningKissGeneratedDialogueCache.limitToRemainingCapacity(lines, 4, 4));
    }

    @Test
    void refusesGeneratedEntriesBeyondConfiguredPoolTarget() {
        MorningKissGeneratedDialogueCache cache = new MorningKissGeneratedDialogueCache(8);
        UUID maidUuid = UUID.randomUUID();

        assertTrue(cache.addIfBelowTarget(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("中文", "中文", "zh.mp3", new byte[] {1},
                        "zh_cn", "zh", "灵梦"), 1));
        assertFalse(cache.addIfBelowTarget(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("English", "English", "en.mp3", new byte[] {1},
                        "en_us", "en", "灵梦"), 1));

        assertEquals(1, cache.size(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING));
        assertEquals("中文", cache.pollFirst(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING)
                .orElseThrow().text());
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
        assertEquals("", MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts("tlm"));
        assertEquals("en_us", MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForChat("en_us"));
    }

    @Test
    void followsTlmChatLanguageForGeneratedTextWhenTmaLanguageIsDefault() {
        assertEquals("zh_cn", MorningKissGeneratedDialogueLanguage.resolveGeneratedTextLanguage("tlm", "en_us", "zh_cn"));
        assertEquals("zh_cn", MorningKissGeneratedDialogueLanguage.resolveGeneratedTextLanguage("auto", "ja_jp", "zh_cn"));
        assertEquals("zh_cn", MorningKissGeneratedDialogueLanguage.resolveGeneratedTextLanguage("default", "", "zh_cn"));
        assertEquals("en_us", MorningKissGeneratedDialogueLanguage.resolveGeneratedTextLanguage("tlm", "en_us", ""));
        assertEquals("en_us", MorningKissGeneratedDialogueLanguage.resolveGeneratedTextLanguage("en_us", "zh_cn", "zh_cn"));
    }

    @Test
    void followsTlmTtsLanguageForPregeneratedVoiceTextWhenTmaLanguageIsDefault() {
        assertEquals("en_us", MorningKissGeneratedDialogueLanguage.resolveGeneratedVoiceTextLanguage("tlm", "en_us", "zh_cn"));
        assertEquals("ja_jp", MorningKissGeneratedDialogueLanguage.resolveGeneratedVoiceTextLanguage("auto", "ja_jp", "zh_cn"));
        assertEquals("zh_cn", MorningKissGeneratedDialogueLanguage.resolveGeneratedVoiceTextLanguage("default", "", "zh_cn"));
        assertEquals("en_us", MorningKissGeneratedDialogueLanguage.resolveGeneratedVoiceTextLanguage("en_us", "zh_cn", "zh_cn"));
    }

    @Test
    void persistsGeneratedEntriesAsExternallyEditableJsonAndVoiceFiles() throws Exception {
        MorningKissGeneratedDialogueCache cache = new MorningKissGeneratedDialogueCache(4);
        UUID maidUuid = UUID.randomUUID();
        byte[] voice = "OggSdata".getBytes(StandardCharsets.US_ASCII);
        cache.add(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING,
                new MorningKissGeneratedDialogueCache.Entry("早呀", "早呀", "generated/test.ogg", voice,
                        "zh_cn", "zh", "灵梦"));

        Path root = Files.createTempDirectory("tma-generated-cache");
        MorningKissGeneratedDialogueStorage.save(root, cache.snapshot());

        Path jsonPath = root.resolve("generated_morning_kiss")
                .resolve(maidUuid.toString())
                .resolve("morning")
                .resolve("001.json");
        Path voicePath = root.resolve("generated_morning_kiss")
                .resolve(maidUuid.toString())
                .resolve("morning")
                .resolve("001.ogg");
        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(voicePath));

        MorningKissGeneratedDialogueCache reloaded = new MorningKissGeneratedDialogueCache(4);
        reloaded.replaceAll(MorningKissGeneratedDialogueStorage.load(root));

        MorningKissGeneratedDialogueCache.Entry entry =
                reloaded.pollFirst(maidUuid, MorningKissScheduleRules.DialoguePool.MORNING).orElseThrow();
        assertEquals("早呀", entry.text());
        assertEquals("zh_cn", entry.textLanguage());
        assertTrue(entry.hasVoice());
        assertEquals(voice.length, entry.voiceData().length);
    }
}
