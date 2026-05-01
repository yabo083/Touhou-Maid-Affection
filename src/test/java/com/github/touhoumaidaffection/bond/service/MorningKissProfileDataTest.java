package com.github.touhoumaidaffection.bond.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorningKissProfileDataTest {
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
                profile.dialogues().get(MorningKissScheduleRules.DialoguePool.MORNING));
        assertEquals(List.of("今天也要好好相处哦。"),
                profile.dialogues().get(MorningKissScheduleRules.DialoguePool.GENERAL));
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
                profile.dialogues().get(MorningKissScheduleRules.DialoguePool.MORNING));
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

        assertEquals(base.dialogues().get(MorningKissScheduleRules.DialoguePool.MORNING),
                profile.dialogues().get(MorningKissScheduleRules.DialoguePool.MORNING));
    }

    @Test
    void shouldMergeSoundAndAiSettingsFromJson() {
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
        assertEquals(List.of("morning_soft.ogg", "sub/fallback.ogg"), profile.voiceFiles());
        assertTrue(profile.aiDialogue().enabled());
        assertEquals("zh_cn", profile.aiDialogue().language());
        assertEquals("只回复一句早安吻台词，称呼玩家 {player}。", profile.aiDialogue().prompt());
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
        assertFalse(profile.aiDialogue().enabled());
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
                    "/absolute.ogg"
                  ]
                }
                """).getAsJsonObject();

        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.merge(
                MorningKissProfileParser.MorningKissProfile.defaults(), root);

        assertEquals(List.of("ok.ogg"), profile.voiceFiles());
    }
}
