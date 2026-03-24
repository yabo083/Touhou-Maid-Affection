package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class EmergencyRescueLocalSoundConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(TouhouMaidAffection.MOD_ID)
            .resolve("rescue_sound.json");
    private static final Path DEFAULT_LOCAL_SOUND_PATH = Path.of(TouhouMaidAffection.MOD_ID, "rescue", "custom_rescue.ogg");

    private static LocalSoundSettings settings = LocalSoundSettings.defaults();
    private static boolean loaded;

    private EmergencyRescueLocalSoundConfig() {
    }

    public static LocalSoundSettings getSettings() {
        ensureLoaded();
        return settings;
    }

    public static Path resolveSoundFile(LocalSoundSettings settings) {
        if (settings == null || settings.filePath().isBlank()) {
            return null;
        }
        try {
            Path configured = Path.of(settings.filePath());
            if (configured.isAbsolute()) {
                return configured.normalize();
            }
            return FMLPaths.CONFIGDIR.get().resolve(configured).normalize();
        } catch (InvalidPathException ex) {
            TouhouMaidAffection.LOGGER.warn("Invalid emergency rescue local sound path '{}'", settings.filePath());
            return null;
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        settings = LocalSoundSettings.defaults();

        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            boolean enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            String filePath = root.has("filePath") ? root.get("filePath").getAsString() : settings.filePath();
            float volume = root.has("volume") ? root.get("volume").getAsFloat() : settings.volume();
            float pitch = root.has("pitch") ? root.get("pitch").getAsFloat() : settings.pitch();
            settings = new LocalSoundSettings(enabled, normalizeFilePath(filePath), clampVolume(volume), clampPitch(pitch));
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to load emergency rescue local sound config, recreating it.", ex);
            settings = LocalSoundSettings.defaults();
            save();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("enabled", settings.enabled());
            root.addProperty("filePath", settings.filePath());
            root.addProperty("volume", settings.volume());
            root.addProperty("pitch", settings.pitch());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to save emergency rescue local sound config.", ex);
        }
    }

    private static String normalizeFilePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LOCAL_SOUND_PATH.toString();
        }
        return raw.trim();
    }

    private static float clampVolume(float value) {
        if (!Float.isFinite(value)) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(2.0F, value));
    }

    private static float clampPitch(float value) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            return 1.0F;
        }
        return Math.max(0.01F, Math.min(2.0F, value));
    }

    public record LocalSoundSettings(boolean enabled, String filePath, float volume, float pitch) {
        public static LocalSoundSettings defaults() {
            return new LocalSoundSettings(false, DEFAULT_LOCAL_SOUND_PATH.toString(), 1.0F, 1.0F);
        }
    }
}
