package com.github.touhoumaidaffection.bond.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorningKissProfileDataTest {
    private static List<String> dialogueTexts(
            MorningKissProfileParser.MorningKissProfile profile,
            MorningKissScheduleRules.DialoguePool pool
    ) {
        return profile.dialogues().getOrDefault(pool, List.of()).stream()
                .map(MorningKissDataPackEntries.DialogueLine::text)
                .toList();
    }

    private static List<String> voiceFileNames(MorningKissProfileParser.MorningKissProfile profile) {
        return profile.voiceFiles().stream().map(MorningKissDataPackEntries.VoiceFile::file).toList();
    }

    @Test
    void shouldMergeCustomDialoguePoolsFromJson() {
        JsonObject root = JsonParser.parseString("""
                {
                  "dialogue_mode": "replace",
                  "dialogue": {
                    "morning": ["早呀，{player}，{maid}来叫你起床啦。"],
                    "general": ["今天也要好好相处哦。"]
                  }
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals(List.of("早呀，{player}，{maid}来叫你起床啦。"),
                dialogueTexts(profile, MorningKissScheduleRules.DialoguePool.MORNING));
        assertEquals(List.of("今天也要好好相处哦。"),
                dialogueTexts(profile, MorningKissScheduleRules.DialoguePool.GENERAL));
        assertEquals(MorningKissProfileParser.DialogueMode.REPLACE, profile.dialogueMode());
    }

    @Test
    void shouldParseAppendDialogueMode() {
        JsonObject root = JsonParser.parseString("""
                {
                  "dialogue_mode": "append",
                  "dialogue": {
                    "morning": ["追加台词"]
                  }
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals(MorningKissProfileParser.DialogueMode.APPEND, profile.dialogueMode());
        assertEquals(List.of("追加台词"),
                dialogueTexts(profile, MorningKissScheduleRules.DialoguePool.MORNING));
    }

    @Test
    void shouldIgnoreBlankDialoguePools() {
        JsonObject root = JsonParser.parseString("""
                {
                  "dialogue": {
                    "morning": ["", "   "]
                  }
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile base = MorningKissProfileParser.MorningKissProfile.defaults();
        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(base, root);

        assertEquals(dialogueTexts(base, MorningKissScheduleRules.DialoguePool.MORNING),
                dialogueTexts(profile, MorningKissScheduleRules.DialoguePool.MORNING));
    }

    @Test
    void shouldMergeSoundSettingsAndIgnoreAiDialogueFromJson() {
        JsonObject root = JsonParser.parseString("""
                {
                  "kiss_sound_event": "example_pack:soft_kiss",
                  "voice_mode": "append",
                  "voice_files": ["morning_soft.ogg", "sub/fallback.ogg"],
                  "ai_dialogue": {
                    "enabled": true,
                    "language": "zh_cn",
                    "prompt": "只回复一句早安吻台词，称呼玩家 {player}。"
                  }
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals("example_pack:soft_kiss", profile.kissSoundEventId());
        assertEquals(MorningKissProfileParser.VoiceMode.APPEND, profile.voiceMode());
        assertEquals(List.of("morning_soft.ogg", "sub/fallback.ogg"), voiceFileNames(profile));
        assertTrue(profile.voiceFiles().stream().allMatch(entry -> entry.language().isBlank()));
    }

    @Test
    void shouldKeepFallbackSoundWhenConfiguredIdIsInvalid() {
        JsonObject root = JsonParser.parseString("""
                {
                  "kiss_sound_event": "not a resource id",
                  "ai_dialogue": {
                    "enabled": true,
                    "prompt": ""
                  }
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals(MorningKissProfileParser.MorningKissProfile.DEFAULT_KISS_SOUND_EVENT_ID,
                profile.kissSoundEventId());
    }

    @Test
    void shouldRejectUnsafeVoiceFilePaths() {
        JsonObject root = JsonParser.parseString("""
                {
                  "voice_files": [
                    "ok.ogg",
                    "../escape.ogg",
                    "nested\\\\bad.ogg",
                    "wrong.wav",
                    "/absolute.ogg",
                    "Bad.ogg",
                    "bad name.ogg"
                  ]
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals(List.of("ok.ogg"), voiceFileNames(profile));
    }

    @Test
    void shouldParseObjectFormDialogueEntriesWithNormalisedLanguage() {
        JsonObject root = JsonParser.parseString("""
                {
                  "dialogue": {
                    "morning": [
                      "未标记台词",
                      {"text": "中文台词", "language": "zh-CN"},
                      {"text": "日文台词", "language": "JA_jp"},
                      {"text": "auto 视为未标记", "language": "auto"},
                      {"text": "   "},
                      {"language": "zh_cn"},
                      42
                    ]
                  }
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        List<MorningKissDataPackEntries.DialogueLine> lines =
                profile.dialogues().get(MorningKissScheduleRules.DialoguePool.MORNING);

        assertEquals(4, lines.size());
        assertEquals(new MorningKissDataPackEntries.DialogueLine("未标记台词", ""), lines.get(0));
        assertEquals(new MorningKissDataPackEntries.DialogueLine("中文台词", "zh_cn"), lines.get(1));
        assertEquals(new MorningKissDataPackEntries.DialogueLine("日文台词", "ja_jp"), lines.get(2));
        assertEquals(new MorningKissDataPackEntries.DialogueLine("auto 视为未标记", ""), lines.get(3));
    }

    @Test
    void shouldParseObjectFormVoiceFilesWithLanguageAndSubtitle() {
        JsonObject root = JsonParser.parseString("""
                {
                  "voice_files": [
                    "legacy.ogg",
                    {"file": "jp.ogg", "language": "ja_jp"},
                    {"file": "zh.ogg", "language": "zh-CN", "text": "配对字幕", "text_language": "zh_cn"},
                    {"language": "ja_jp"},
                    {"file": "../escape.ogg", "language": "ja_jp"},
                    "wrong.wav",
                    {"file": "jp.ogg", "language": "zh_cn"}
                  ]
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        List<MorningKissDataPackEntries.VoiceFile> files = profile.voiceFiles();
        assertEquals(List.of("legacy.ogg", "jp.ogg", "zh.ogg"), voiceFileNames(profile));
        assertEquals(new MorningKissDataPackEntries.VoiceFile("legacy.ogg", "", "", ""), files.get(0));
        assertEquals(new MorningKissDataPackEntries.VoiceFile("jp.ogg", "ja_jp", "", ""), files.get(1));
        assertEquals(new MorningKissDataPackEntries.VoiceFile("zh.ogg", "zh_cn", "配对字幕", "zh_cn"), files.get(2));
    }

    @Test
    void shouldBoundVoiceSubtitleLength() {
        String oversized = "字".repeat(400);
        JsonObject root = JsonParser.parseString("""
                {
                  "voice_files": [
                    {"file": "long.ogg", "text": "%s"}
                  ]
                }
                """.formatted(oversized)).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals(1, profile.voiceFiles().size());
        assertTrue(profile.voiceFiles().get(0).text().isBlank());
    }

    @Test
    void shouldCapVoiceFileCountAtSixtyFour() {
        StringBuilder json = new StringBuilder("{\"voice_files\":[");
        for (int index = 0; index < 70; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append("voice_").append(index).append(".ogg\"");
        }
        json.append("]}");

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(),
                JsonParser.parseString(json.toString()).getAsJsonObject());

        assertEquals(64, profile.voiceFiles().size());
        assertEquals("voice_0.ogg", profile.voiceFiles().get(0).file());
        assertEquals("voice_63.ogg", profile.voiceFiles().get(63).file());
    }
}