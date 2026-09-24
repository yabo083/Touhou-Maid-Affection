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
    public static final String MORNING_KISS_IMMEDIATE_FALLBACK_ENABLED = "morning_kiss.immediate_fallback_enabled";
    public static final String EMERGENCY_RESCUE_ENABLED = "emergency_rescue.enabled";
    public static final String RANDOM_GIFT_ENABLED = "random_gift.enabled";
    public static final String MAID_PRAYER_BUFF_ENABLED = "maid_prayer_buff.enabled";

    // Language values (auto / tlm / inherit / default / explicit locale)
    public static final String MORNING_KISS_DISPLAY_LANGUAGE = "morning_kiss.display_language";
    public static final String MORNING_KISS_VOICE_LANGUAGE = "morning_kiss.voice_language";

    // Free text values (Morning Kiss prompt template)
    public static final String MORNING_KISS_TEXT_PROMPT = "morning_kiss.text_prompt";

    // Integer values (AI dialogue cache policy); the bounds mirror the matching ModConfig
    // defineInRange calls, so a value accepted here can never be clamped by the config system.
    public static final String MORNING_KISS_CACHE_TARGET_PER_POOL = "morning_kiss.cache_target_per_pool";
    public static final String MORNING_KISS_CACHE_SCAN_INTERVAL_TICKS = "morning_kiss.cache_scan_interval_ticks";
    public static final String MORNING_KISS_CACHE_CONSUME_ON_USE = "morning_kiss.cache_consume_on_use";

    /** Bounds of {@link #MORNING_KISS_CACHE_TARGET_PER_POOL} ({@code aiDialogueCacheTargetPerPool}). */
    public static final int CACHE_TARGET_PER_POOL_MIN = 1;
    public static final int CACHE_TARGET_PER_POOL_MAX = 8;

    /** Bounds of {@link #MORNING_KISS_CACHE_SCAN_INTERVAL_TICKS} ({@code aiDialogueScanIntervalTicks}). */
    public static final int CACHE_SCAN_INTERVAL_TICKS_MIN = 20;
    public static final int CACHE_SCAN_INTERVAL_TICKS_MAX = 72000;

    public static final String LABEL_KEY_PREFIX = "bond.settings.key.";
    public static final String SUB_LABEL_KEY_PREFIX = "bond.settings.sub.";

    /** Maximum accepted length of a language value. */
    public static final int MAX_LANGUAGE_LENGTH = 32;

    /** Maximum accepted length of a {@link Type#TEXT} value (prompt template). */
    public static final int MAX_TEXT_LENGTH = 1024;

    /** Legacy keyword semantics shared by every language setting. */
    public static final List<String> LANGUAGE_KEYWORDS = List.of("auto", "tlm", "inherit", "default");

    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,8}(?:_[a-z0-9]{1,8})?");

    private static final Map<String, Type> WHITELIST = createWhitelist();

    private TmaSettingsKeys() {
    }

    /** Value grammar of a whitelisted key. */
    public enum Type {
        BOOLEAN,
        LANGUAGE,
        TEXT,
        INT
    }

    private static Map<String, Type> createWhitelist() {
        Map<String, Type> keys = new LinkedHashMap<>();
        keys.put(MORNING_KISS_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_AUTO_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_AI_DIALOGUE_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_AI_TTS_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_IMMEDIATE_FALLBACK_ENABLED, Type.BOOLEAN);
        keys.put(EMERGENCY_RESCUE_ENABLED, Type.BOOLEAN);
        keys.put(RANDOM_GIFT_ENABLED, Type.BOOLEAN);
        keys.put(MAID_PRAYER_BUFF_ENABLED, Type.BOOLEAN);
        keys.put(MORNING_KISS_DISPLAY_LANGUAGE, Type.LANGUAGE);
        keys.put(MORNING_KISS_VOICE_LANGUAGE, Type.LANGUAGE);
        keys.put(MORNING_KISS_TEXT_PROMPT, Type.TEXT);
        keys.put(MORNING_KISS_CACHE_TARGET_PER_POOL, Type.INT);
        keys.put(MORNING_KISS_CACHE_SCAN_INTERVAL_TICKS, Type.INT);
        keys.put(MORNING_KISS_CACHE_CONSUME_ON_USE, Type.BOOLEAN);
        // Keep insertion order: the panel renders switches, then languages, then free text.
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

    /** Translation key of the secondary description line shown under {@link #labelKey(String)}. */
    public static String subKey(String key) {
        return SUB_LABEL_KEY_PREFIX + key;
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
            case TEXT -> normalizeText(rawValue);
            case INT -> normalizeInt(key, rawValue);
        };
    }

    /**
     * Validates an integer value against the bounds of that key.
     *
     * <p>Only a run of ASCII digits is accepted: signs, decimal points, exponents, blanks and empty
     * strings are all rejected, and so is any value outside the configured range. The bounds mirror
     * the {@code defineInRange} call of the matching {@code ModConfig} entry.
     */
    public static Optional<String> normalizeInt(String key, String rawValue) {
        int[] bounds = intBounds(key);
        if (bounds == null || rawValue == null) {
            return Optional.empty();
        }
        String value = rawValue.trim();
        if (value.isEmpty() || value.length() > 6) {
            return Optional.empty();
        }
        for (int index = 0; index < value.length(); index++) {
            char digit = value.charAt(index);
            if (digit < '0' || digit > '9') {
                return Optional.empty();
            }
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException overflow) {
            return Optional.empty();
        }
        return parsed >= bounds[0] && parsed <= bounds[1] ? Optional.of(Integer.toString(parsed)) : Optional.empty();
    }

    /** @return {@code {min, max}} of an {@link Type#INT} key, or {@code null} for any other key. */
    public static int[] intBounds(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case MORNING_KISS_CACHE_TARGET_PER_POOL -> new int[]{CACHE_TARGET_PER_POOL_MIN, CACHE_TARGET_PER_POOL_MAX};
            case MORNING_KISS_CACHE_SCAN_INTERVAL_TICKS ->
                    new int[]{CACHE_SCAN_INTERVAL_TICKS_MIN, CACHE_SCAN_INTERVAL_TICKS_MAX};
            default -> null;
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
     * Validates a free text value (prompt template).
     *
     * <p>Any non-null string up to {@link #MAX_TEXT_LENGTH} characters is accepted verbatim. The
     * empty string is valid on purpose: the server interprets it as "restore the built-in default
     * template" instead of storing an empty prompt.
     */
    public static Optional<String> normalizeText(String rawValue) {
        if (rawValue == null || rawValue.length() > MAX_TEXT_LENGTH) {
            return Optional.empty();
        }
        return Optional.of(rawValue);
    }

    /**
     * Canonicalises a language value for display.
     *
     * <p>Every legacy keyword ({@code tlm}/{@code inherit}/{@code default}, case-insensitive) is shown
     * as {@code auto} because they all mean "follow the game / maid AI language"; an explicit locale
     * is returned unchanged so the dropdown still shows what is stored. Blank values fall back to
     * {@code auto} as well.</p>
     */
    public static String languageForDisplay(String rawValue) {
        if (rawValue == null) {
            return "auto";
        }
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return "auto";
        }
        String lowerCase = value.toLowerCase(Locale.ROOT);
        return LANGUAGE_KEYWORDS.contains(lowerCase) ? "auto" : value;
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