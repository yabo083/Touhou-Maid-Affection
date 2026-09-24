package com.github.touhoumaidaffection.bond.settings;

import com.github.touhoumaidaffection.ModConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the stable logical keys of {@link TmaSettingsKeys} onto the concrete {@code ModConfig}
 * entries. This is the only place that couples the panel protocol to the TOML layout.
 */
public final class TmaSettingsResolver {
    private TmaSettingsResolver() {
    }

    /** @return the current value of a whitelisted key as a wire string, or {@code ""} when unknown. */
    public static String read(String key) {
        ForgeConfigSpec.ConfigValue<?> configValue = valueOf(key);
        if (configValue == null) {
            return "";
        }
        Object current = configValue.get();
        return current == null ? "" : String.valueOf(current);
    }

    /**
     * Applies an already canonicalised value. Callers must validate through
     * {@link TmaSettingsKeys#normalize(String, String)} first and persist once with
     * {@link ModConfig#SPEC}{@code .save()}.
     */
    @SuppressWarnings("unchecked")
    public static void write(String key, String normalizedValue) {
        ForgeConfigSpec.ConfigValue<?> configValue = valueOf(key);
        if (configValue == null || normalizedValue == null) {
            return;
        }
TmaSettingsKeys.Type type = TmaSettingsKeys.typeOf(key);
        if (type == TmaSettingsKeys.Type.BOOLEAN) {
            ((ForgeConfigSpec.ConfigValue<Boolean>) configValue).set(Boolean.parseBoolean(normalizedValue));
        } else if (type == TmaSettingsKeys.Type.TEXT) {
            ForgeConfigSpec.ConfigValue<String> stringValue = (ForgeConfigSpec.ConfigValue<String>) configValue;
            // An empty value means "restore the built-in default template" rather than storing "".
            stringValue.set(normalizedValue.isEmpty() ? stringValue.getDefault() : normalizedValue);
        } else {
            ((ForgeConfigSpec.ConfigValue<String>) configValue).set(normalizedValue);
        }
    }

    /** Full authoritative state of every whitelisted key, in {@link TmaSettingsKeys#keys()} order. */
    public static List<TmaSettingsWire.Entry> snapshot() {
        List<TmaSettingsWire.Entry> entries = new ArrayList<>(TmaSettingsKeys.keys().size());
        for (String key : TmaSettingsKeys.keys()) {
            entries.add(new TmaSettingsWire.Entry(key, read(key)));
        }
        return List.copyOf(entries);
    }

    private static ForgeConfigSpec.ConfigValue<?> valueOf(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case TmaSettingsKeys.MORNING_KISS_ENABLED -> ModConfig.BOND_MORNING_KISS_ENABLED;
            case TmaSettingsKeys.MORNING_KISS_AUTO_ENABLED -> ModConfig.BOND_MORNING_KISS_AUTO_ENABLED;
            case TmaSettingsKeys.MORNING_KISS_AI_DIALOGUE_ENABLED -> ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED;
            case TmaSettingsKeys.MORNING_KISS_AI_TTS_ENABLED -> ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED;
            case TmaSettingsKeys.EMERGENCY_RESCUE_ENABLED -> ModConfig.BOND_EMERGENCY_RESCUE_ENABLED;
            case TmaSettingsKeys.RANDOM_GIFT_ENABLED -> ModConfig.BOND_RANDOM_GIFT_ENABLED;
            case TmaSettingsKeys.MAID_PRAYER_BUFF_ENABLED -> ModConfig.BUFF_ENABLED;
            case TmaSettingsKeys.MORNING_KISS_DISPLAY_LANGUAGE -> ModConfig.BOND_MORNING_KISS_DISPLAY_LANGUAGE;
            case TmaSettingsKeys.MORNING_KISS_VOICE_LANGUAGE -> ModConfig.BOND_MORNING_KISS_VOICE_LANGUAGE;
            case TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT -> ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PROMPT;
            default -> null;
        };
    }
}