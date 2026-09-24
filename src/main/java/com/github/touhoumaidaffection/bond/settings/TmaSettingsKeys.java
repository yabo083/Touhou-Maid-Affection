package com.github.touhoumaidaffection.bond.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pure-logic description of the server-authoritative settings exposed by the in-game settings panel.
 *
 * <p>Deliberately free of Minecraft/NeoForge types so the whitelist, the value grammar and the
 * normalisation rules stay unit-testable. Keys are stable logical names and must never mirror the
 * TOML paths of {@code ModConfig}: the toml layout may move without breaking the wire protocol or
 * the client cache.
 */
public final class TmaSettingsKeys {
    // Feature switches (boolean values)
    public static final String MORNING_KISS_ENABLED = "morning_kiss.enabled";
    public static final String MORNING_KISS_AUTO_ENABLED = "morning_kiss.auto_enabled";
    public static final String MORNING_KISS_AI_DIALOGUE_ENABLED = "morning_kiss.ai_dialogue_enabled";
    public static final String MORNING_KISS_AI_TTS_ENABLED = "morning_kiss.ai_tts_enabled";
    public static final String EMERGENCY_RESCUE_ENABLED = "emergency_rescue.enabled";
    public static final String RANDOM_GIFT_ENABLED = "random_gift.enabled";
    public static final String MAID_PRAYER_BUFF_ENABLED = "maid_prayer_buff.enabled";

    // Language values (auto / tlm / inherit / default / explicit locale)
    public static final String MORNING_KISS_DISPLAY_LANGUAGE = "morning_kiss.display_language";
    public static final String MORNING_KISS_VOICE_LANGUAGE = "morning_kiss.voice_language";
    public static final String MORNING_KISS_AI_DIALOGUE_LANGUAGE = "morning_kiss.ai_dialogue_language";
    public static final String MORNING_KISS_AI_DIALOGUE_VOICE_LANGUAGE = "morning_kiss.ai_dialogue_voice_language";

    public static final String LABEL_KEY_PREFIX = "bond.settings.key.";

    /** Maximum accepted length of a language value. */
    public static final int MAX_LANGUAGE_LENGTH = 32;

    /** Legacy keyword semantics shared by every language setting. */
    public static final List<String> LANGUAGE_KEYWORDS = List.of("auto", "tlm", "inherit", "default");

    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,8}(?:_[a-z0-9]{1,8})?");

    private static final Map<String, Type> WHITELIST = createWhitelist();

    private TmaSettingsKeys() {
    }

    /** Value grammar of a whitelisted key. */
    public enum Type {
        BOOLEAN,
        LANGUAGE
    }

    private static Map<String, Type> createWhitelist() {
        Map<String, Type> keys = new LinkedHashMap<>();
        keys.put(MORNING_KISS_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_AUTO_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_AI_DIALOGUE_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_AI_TTS_ENABLED, Type.BOOLEAN);
        keys.put(EMERGENCY_RESCUE_ENABLED, Type.BOOLEAN);
        keys.put(RANDOM_GIFT_ENABLED, Type.BOOLEAN);
        keys.put(MAID_PRAYER_BUFF_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_DISPLAY_LANGUAGE, Type.LANGUAGE);
        keys.put(MORNING_KISS_VOICE_LANGUAGE, Type.LANGUAGE);
        keys.put(MORNING_KISS_AI_DIALOGUE_LANGUAGE, Type.LANGUAGE);
        keys.put(MORNING_KISS_AI_DIALOGUE_VOICE_LANGUAGE, Type.LANGUAGE);
        // Keep insertion order: the panel renders switches, then languages, then volumes.
        return java.util.Collections.unmodifiableMap(keys);
    }

    /** Whitelisted logical keys in a stable, display-friendly order. */
    public static List<String> keys() {
        return List.copyOf(WHITELIST.keySet());
    }

    public static boolean isWhitelisted(String key) {
        return key != null && WHITELIST.containsKey(key);
    }

    /** @return the value grammar of {@code key}, or {@code null} when the key is not whitelisted. */
    public static Type typeOf(String key) {
        return key == null ? null : WHITELIST.get(key);
    }

    /** Translation key of the display label of a whitelisted logical key. */
    public static String labelKey(String key) {
        return LABEL_KEY_PREFIX + key;
    }

    /**
     * Validates and canonicalises one key/value pair.
     *
     * @return the canonical wire value, or {@link Optional#empty()} when the key is unknown or the
     *         value does not match the grammar of that key.
     */
    public static Optional<String> normalize(String key, String rawValue) {
        Type type = typeOf(key);
        if (type == null || rawValue == null) {
            return Optional.empty();
        }
        return switch (type) {
            case BOOLEAN -> parseBoolean(rawValue).map(value -> value ? "true" : "false");
            case LANGUAGE -> normalizeLanguage(rawValue);
        };
    }

    /**
     * Validates a whole request batch with all-or-nothing semantics: as soon as one entry is
     * invalid the entire batch is rejected, so a caller can never apply a partial update.
     *
     * @return the canonicalised entries, or {@link Optional#empty()} when any entry is invalid.
     */
    public static Optional<List<TmaSettingsWire.Entry>> normalizeAll(List<TmaSettingsWire.Entry> requested) {
        if (requested == null || requested.isEmpty()) {
            return Optional.of(List.of());
        }
        List<TmaSettingsWire.Entry> normalized = new java.util.ArrayList<>(requested.size());
        for (TmaSettingsWire.Entry entry : requested) {
            if (entry == null) {
                return Optional.empty();
            }
            Optional<String> value = normalize(entry.key(), entry.value());
            if (value.isEmpty()) {
                return Optional.empty();
            }
            normalized.add(new TmaSettingsWire.Entry(entry.key(), value.get()));
        }
        return Optional.of(List.copyOf(normalized));
    }

    /** Parses {@code true}/{@code false} case-insensitively; anything else is rejected. */
    public static Optional<Boolean> parseBoolean(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value)) {
            return Optional.of(Boolean.TRUE);
        }
        if ("false".equals(value)) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }

    public static boolean isAllowedLanguage(String rawValue) {
        return normalizeLanguage(rawValue).isPresent();
    }

    /**
     * Canonicalises a language value.
     *
     * <p>Accepted: the legacy keywords {@code auto}/{@code tlm}/{@code inherit}/{@code default}
     * (case-insensitive, canonicalised to lower case) or a lower-case locale such as {@code zh_cn},
     * {@code ja_jp}, {@code en_us} (two to eight lower-case letters with an optional
     * {@code _subtag}). Upper-case locales such as {@code ZH_CN} are rejected on purpose so a typo
     * cannot silently become a locale that never matches.
     */
    public static Optional<String> normalizeLanguage(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        String value = rawValue.trim();
        if (value.isEmpty() || value.length() > MAX_LANGUAGE_LENGTH) {
            return Optional.empty();
        }
        String lowerCase = value.toLowerCase(Locale.ROOT);
        if (LANGUAGE_KEYWORDS.contains(lowerCase)) {
            return Optional.of(lowerCase);
        }
        return value.equals(lowerCase) && LOCALE.matcher(value).matches() ? Optional.of(value) : Optional.empty();
    }
}