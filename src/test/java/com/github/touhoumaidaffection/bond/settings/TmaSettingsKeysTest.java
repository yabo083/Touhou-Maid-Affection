package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TmaSettingsKeysTest {
    @Test
    void whitelistCoversEveryFeatureSwitchLanguageAndText() {
        assertEquals(10, TmaSettingsKeys.keys().size());
        assertTrue(TmaSettingsKeys.isWhitelisted("morning_kiss.enabled"));
        assertTrue(TmaSettingsKeys.isWhitelisted("maid_prayer_buff.enabled"));
        assertTrue(TmaSettingsKeys.isWhitelisted("morning_kiss.display_language"));
        assertTrue(TmaSettingsKeys.isWhitelisted("morning_kiss.voice_language"));
        assertTrue(TmaSettingsKeys.isWhitelisted(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT));
        assertFalse(TmaSettingsKeys.isWhitelisted("morning_kiss.ai_dialogue_language"));
        assertFalse(TmaSettingsKeys.isWhitelisted("morning_kiss.ai_dialogue_voice_language"));
        assertEquals(TmaSettingsKeys.Type.BOOLEAN, TmaSettingsKeys.typeOf("random_gift.enabled"));
        assertEquals(TmaSettingsKeys.Type.LANGUAGE, TmaSettingsKeys.typeOf("morning_kiss.display_language"));
        assertEquals(TmaSettingsKeys.Type.TEXT, TmaSettingsKeys.typeOf(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT));
    }

    @Test
    void textAcceptsAnythingUpToTheLengthLimitIncludingEmpty() {
        assertEquals(Optional.of(""), TmaSettingsKeys.normalize(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT, ""));
        assertEquals(Optional.of("你正在扮演 {maid}"),
                TmaSettingsKeys.normalize(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT, "你正在扮演 {maid}"));
        assertEquals(Optional.empty(), TmaSettingsKeys.normalize(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT, null));

        String atLimit = "x".repeat(TmaSettingsKeys.MAX_TEXT_LENGTH);
        assertEquals(Optional.of(atLimit), TmaSettingsKeys.normalizeText(atLimit));
        String overLimit = "x".repeat(TmaSettingsKeys.MAX_TEXT_LENGTH + 1);
        assertEquals(Optional.empty(), TmaSettingsKeys.normalizeText(overLimit));
    }

    @Test
    void batchRejectsAnOverlongTextEntry() {
        String overLimit = "x".repeat(TmaSettingsKeys.MAX_TEXT_LENGTH + 1);
        assertTrue(TmaSettingsKeys.normalizeAll(List.of(
                new TmaSettingsWire.Entry(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT, overLimit)
        )).isEmpty());
        assertTrue(TmaSettingsKeys.normalizeAll(List.of(
                new TmaSettingsWire.Entry(TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT, "")
        )).isPresent());
    }

    @Test
    void unknownKeysAreRejected() {
        assertFalse(TmaSettingsKeys.isWhitelisted("morning_kiss.unknown"));
        assertFalse(TmaSettingsKeys.isWhitelisted("morningKissBehavior.enabled"));
        assertFalse(TmaSettingsKeys.isWhitelisted(null));
        assertNull(TmaSettingsKeys.typeOf("nope"));
        assertTrue(TmaSettingsKeys.normalize("nope", "true").isEmpty());
    }

    @Test
    void booleansAcceptOnlyTrueAndFalseIgnoringCase() {
        assertEquals(Optional.of("true"), TmaSettingsKeys.normalize("morning_kiss.enabled", "TRUE"));
        assertEquals(Optional.of("false"), TmaSettingsKeys.normalize("morning_kiss.enabled", " False "));
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.enabled", "yes").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.enabled", "1").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.enabled", "").isEmpty());
    }

    @Test
    void languagesAcceptKeywordsAndLowerCaseLocales() {
        for (String keyword : List.of("auto", "tlm", "inherit", "default")) {
            assertEquals(Optional.of(keyword), TmaSettingsKeys.normalize("morning_kiss.display_language", keyword));
        }
        assertEquals(Optional.of("auto"), TmaSettingsKeys.normalize("morning_kiss.display_language", "AUTO"));
        for (String locale : List.of("zh_cn", "ja_jp", "en_us", "zh_tw", "ko_kr", "pt_br")) {
            assertEquals(Optional.of(locale), TmaSettingsKeys.normalize("morning_kiss.voice_language", locale));
        }
    }

    @Test
    void languagesRejectUpperCasingIllegalCharactersAndOverlongValues() {
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "ZH_CN").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "zh-CN").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "zh cn").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "z").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "zh_cn_extra_subtag").isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "zh_cn_" + "a".repeat(40)).isEmpty());
        assertTrue(TmaSettingsKeys.normalize("morning_kiss.voice_language", "a".repeat(TmaSettingsKeys.MAX_LANGUAGE_LENGTH + 1)).isEmpty());

        // The longest structurally valid locale is a full-length language and region subtag.
        assertEquals(Optional.of("abcdefgh_abcdefgh"),
                TmaSettingsKeys.normalize("morning_kiss.voice_language", "abcdefgh_abcdefgh"));
    }

    @Test
    void batchValidationRejectsTheWholeRequestWhenOneEntryIsInvalid() {
        Optional<List<TmaSettingsWire.Entry>> accepted = TmaSettingsKeys.normalizeAll(List.of(
                new TmaSettingsWire.Entry("morning_kiss.enabled", "TRUE"),
                new TmaSettingsWire.Entry("morning_kiss.display_language", "zh_cn")
        ));
        assertTrue(accepted.isPresent());
        assertEquals(List.of(
                new TmaSettingsWire.Entry("morning_kiss.enabled", "true"),
                new TmaSettingsWire.Entry("morning_kiss.display_language", "zh_cn")
        ), accepted.get());

        assertTrue(TmaSettingsKeys.normalizeAll(List.of(
                new TmaSettingsWire.Entry("morning_kiss.enabled", "true"),
                new TmaSettingsWire.Entry("morning_kiss.display_language", "ZH_CN")
        )).isEmpty());

        assertTrue(TmaSettingsKeys.normalizeAll(List.of(
                new TmaSettingsWire.Entry("morning_kiss.enabled", "true"),
                new TmaSettingsWire.Entry("not.a.key", "true")
        )).isEmpty());

        assertTrue(TmaSettingsKeys.normalizeAll(List.of(
                new TmaSettingsWire.Entry("morning_kiss.enabled", "maybe")
        )).isEmpty());
    }

    @Test
    void emptyBatchIsAccepted() {
        assertEquals(Optional.of(List.of()), TmaSettingsKeys.normalizeAll(List.of()));
        assertEquals(Optional.of(List.of()), TmaSettingsKeys.normalizeAll(null));
    }

    @Test
    void labelKeysAreDerivedFromLogicalKeys() {
        assertEquals("bond.settings.key.morning_kiss.enabled", TmaSettingsKeys.labelKey("morning_kiss.enabled"));
    }
}