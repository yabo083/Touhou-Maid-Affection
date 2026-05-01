package com.github.touhoumaidaffection.bond.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InteractionVoiceProfileParserTest {
    @Test
    void shouldKeepMorningKissAndEmergencyRescueVoicePoolsSeparate() {
        JsonObject root = JsonParser.parseString("""
                {
                  "morning_kiss": {
                    "voice_mode": "replace",
                    "voice_files": ["morning.ogg"]
                  },
                  "emergency_rescue": {
                    "voice_mode": "append",
                    "voice_files": ["rescue.ogg"],
                    "sound_event": "minecraft:entity.player.levelup"
                  }
                }
                """).getAsJsonObject();

        InteractionVoiceProfileParser.InteractionVoiceProfile profile =
                InteractionVoiceProfileParser.merge(InteractionVoiceProfileParser.InteractionVoiceProfile.defaults(), root);

        InteractionVoiceProfileParser.MaidContext maid = new InteractionVoiceProfileParser.MaidContext(
                "00000000-0000-0000-0000-000000000001",
                "Hakurei Reimu",
                "touhou_little_maid:hakurei_reimu",
                "tlm:reimu",
                "ysm:reimu"
        );

        InteractionVoiceProfileParser.FeatureVoiceProfile morning =
                profile.resolve(InteractionVoiceProfileParser.Feature.MORNING_KISS, maid);
        InteractionVoiceProfileParser.FeatureVoiceProfile rescue =
                profile.resolve(InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE, maid);

        assertEquals(List.of("morning.ogg"), morning.voiceFiles());
        assertEquals(List.of("rescue.ogg"), rescue.voiceFiles());
        assertEquals(InteractionVoiceProfileParser.VoiceMode.REPLACE, morning.voiceMode());
        assertEquals(InteractionVoiceProfileParser.VoiceMode.APPEND, rescue.voiceMode());
        assertEquals("minecraft:entity.player.levelup", rescue.rescueOptions().soundEventId());
    }

    @Test
    void shouldApplyMatchingMaidEntriesInOrderWithoutMixingFeatures() {
        JsonObject root = JsonParser.parseString("""
                {
                  "morning_kiss": {
                    "voice_files": ["default/morning.ogg"]
                  },
                  "emergency_rescue": {
                    "voice_files": ["default/rescue.ogg"]
                  },
                  "maids": [
                    {
                      "match": { "model": "touhou_little_maid:hakurei_reimu" },
                      "morning_kiss": {
                        "voice_files": ["reimu/morning.ogg"]
                      },
                      "emergency_rescue": {
                        "voice_files": ["reimu/rescue.ogg"]
                      }
                    },
                    {
                      "match": { "uuid": "00000000-0000-0000-0000-000000000001" },
                      "morning_kiss": {
                        "voice_mode": "append",
                        "voice_files": ["uuid/morning.ogg"]
                      }
                    }
                  ]
                }
                """).getAsJsonObject();

        InteractionVoiceProfileParser.InteractionVoiceProfile profile =
                InteractionVoiceProfileParser.merge(InteractionVoiceProfileParser.InteractionVoiceProfile.defaults(), root);

        InteractionVoiceProfileParser.MaidContext reimu = new InteractionVoiceProfileParser.MaidContext(
                "00000000-0000-0000-0000-000000000001",
                "Hakurei Reimu",
                "touhou_little_maid:hakurei_reimu",
                "tlm:reimu",
                "ysm:reimu"
        );
        InteractionVoiceProfileParser.MaidContext other = new InteractionVoiceProfileParser.MaidContext(
                "00000000-0000-0000-0000-000000000002",
                "Kirisame Marisa",
                "touhou_little_maid:kirisame_marisa",
                "tlm:marisa",
                "ysm:marisa"
        );

        assertEquals(List.of("uuid/morning.ogg"),
                profile.resolve(InteractionVoiceProfileParser.Feature.MORNING_KISS, reimu).voiceFiles());
        assertEquals(InteractionVoiceProfileParser.VoiceMode.APPEND,
                profile.resolve(InteractionVoiceProfileParser.Feature.MORNING_KISS, reimu).voiceMode());
        assertEquals(List.of("reimu/rescue.ogg"),
                profile.resolve(InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE, reimu).voiceFiles());
        assertEquals(List.of("default/morning.ogg"),
                profile.resolve(InteractionVoiceProfileParser.Feature.MORNING_KISS, other).voiceFiles());
        assertEquals(List.of("default/rescue.ogg"),
                profile.resolve(InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE, other).voiceFiles());
    }

    @Test
    void shouldRejectUnsafeVoiceFilePathsFromEveryFeature() {
        JsonObject root = JsonParser.parseString("""
                {
                  "morning_kiss": {
                    "voice_files": ["ok/morning.ogg", "../escape.ogg", "bad\\\\slash.ogg", "wrong.wav"]
                  },
                  "maids": [
                    {
                      "match": { "name": "Hakurei Reimu" },
                      "emergency_rescue": {
                        "voice_files": ["ok/rescue.ogg", "/absolute.ogg", "nested/../bad.ogg"]
                      }
                    }
                  ]
                }
                """).getAsJsonObject();

        InteractionVoiceProfileParser.InteractionVoiceProfile profile =
                InteractionVoiceProfileParser.merge(InteractionVoiceProfileParser.InteractionVoiceProfile.defaults(), root);
        InteractionVoiceProfileParser.MaidContext maid = new InteractionVoiceProfileParser.MaidContext(
                "",
                "Hakurei Reimu",
                "",
                "",
                ""
        );

        assertEquals(List.of("ok/morning.ogg"),
                profile.resolve(InteractionVoiceProfileParser.Feature.MORNING_KISS, maid).voiceFiles());
        assertEquals(List.of("ok/rescue.ogg"),
                profile.resolve(InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE, maid).voiceFiles());
    }
}
