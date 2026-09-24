package com.github.touhoumaidaffection.bond.settings;

import com.github.touhoumaidaffection.bond.service.MorningKissScheduleRules;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the settings panel against translation keys that exist in code but not in the shipped
 * language files: such a key renders as its raw id (for example
 * {@code bond.settings.status.pool.general} in the "by maid" list), which is what these assertions
 * prevent from coming back.
 *
 * <p>The test reads the language files of the main source set straight from the classpath, so it
 * fails as soon as a key is added to the panel without being added to both languages.
 */
class TmaSettingsLangKeysTest {
    private static final List<String> LANGUAGES = List.of("zh_cn", "en_us");

    @Test
    void everyDialoguePoolHasALabelInBothLanguages() {
        List<String> required = new ArrayList<>();
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            required.add("bond.settings.status.pool." + pool.name().toLowerCase(Locale.ROOT));
        }
        assertKeysPresent(required);
    }

    @Test
    void everyEditableSettingHasALabelAndSubtitleInBothLanguages() {
        List<String> required = new ArrayList<>();
        for (String key : TmaSettingsKeys.keys()) {
            if (TmaSettingsKeys.typeOf(key) == TmaSettingsKeys.Type.INT) {
                // Numeric rows reuse the cache-policy labels of the status tab instead of key.*.
                continue;
            }
            required.add(TmaSettingsKeys.labelKey(key));
            required.add(TmaSettingsKeys.subKey(key));
        }
        assertKeysPresent(required);
    }

    @Test
    void statusTabAndPromptKeysExistInBothLanguages() {
        assertKeysPresent(List.of(
                "bond.settings.prompt.legend",
                "bond.settings.prompt.reset",
                "bond.settings.number.tip",
                "bond.settings.status.section.switches",
                "bond.settings.status.section.languages",
                "bond.settings.status.section.cache_policy",
                "bond.settings.status.section.cache_stats",
                "bond.settings.status.section.maids",
                "bond.settings.status.switch.morning_kiss",
                "bond.settings.status.switch.ai_dialogue",
                "bond.settings.status.switch.ai_tts",
                "bond.settings.status.switch.fallback",
                "bond.settings.status.language.display",
                "bond.settings.status.language.voice",
                "bond.settings.status.cache_policy.target",
                "bond.settings.status.cache_policy.scan",
                "bond.settings.status.cache_policy.consume"
        ));
    }

    private static void assertKeysPresent(List<String> keys) {
        for (String language : LANGUAGES) {
            JsonObject translations = load(language);
            for (String key : keys) {
                assertTrue(translations.has(key), language + " is missing the translation key " + key);
            }
        }
    }

    private static JsonObject load(String language) {
        String path = "/assets/touhou_maid_affection/lang/" + language + ".json";
        try (InputStream stream = TmaSettingsLangKeysTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing language resource " + path);
            }
            JsonObject translations = new Gson()
                    .fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            if (translations == null) {
                throw new IllegalStateException("empty language resource " + path);
            }
            return translations;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to read " + path, exception);
        }
    }
}