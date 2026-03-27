package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

public final class EmergencyRescueCustomVoiceConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path RESCUE_ROOT = FMLPaths.CONFIGDIR.get()
            .resolve(TouhouMaidAffection.MOD_ID)
            .resolve("rescue");
    private static final Path MAIDS_ROOT = RESCUE_ROOT.resolve("maids");
    private static final Path COMMON_ROOT = RESCUE_ROOT.resolve("common");
    private static final Path LEGACY_CUSTOM_FILE = RESCUE_ROOT.resolve("custom_rescue.ogg");
    private static final Path SERVER_SYNC_ROOT = RESCUE_ROOT.resolve("server_synced");
    private static final String MAID_CONFIG_NAME = "voice.json";

    private EmergencyRescueCustomVoiceConfig() {
    }

    public static Path rescueRoot() {
        ensureBaseDirs();
        return RESCUE_ROOT;
    }

    public static Path localMaidDir(String maidUuid, String maidDisplayName) {
        ensureBaseDirs();
        Path dir = MAIDS_ROOT.resolve(buildMaidFolderName(maidUuid, maidDisplayName));
        ensureDirectory(dir);
        return dir;
    }

    public static Path localCommonDir() {
        ensureBaseDirs();
        ensureDirectory(COMMON_ROOT);
        return COMMON_ROOT;
    }

    public static Path legacyCustomFile() {
        ensureBaseDirs();
        return LEGACY_CUSTOM_FILE;
    }

    public static Path syncedRootForServer(String serverId) {
        ensureBaseDirs();
        String safeServerId = sanitizeServerId(serverId);
        Path root = SERVER_SYNC_ROOT.resolve(safeServerId);
        ensureDirectory(root);
        ensureDirectory(root.resolve("maids"));
        ensureDirectory(root.resolve("common"));
        return root;
    }

    public static Path syncedMaidDir(String serverId, String maidUuid, String maidDisplayName) {
        Path root = syncedRootForServer(serverId);
        Path dir = root.resolve("maids").resolve(buildMaidFolderName(maidUuid, maidDisplayName));
        ensureDirectory(dir);
        return dir;
    }

    public static Path syncedCommonDir(String serverId) {
        Path root = syncedRootForServer(serverId);
        Path dir = root.resolve("common");
        ensureDirectory(dir);
        return dir;
    }

    public static MaidCustomSettings loadOrCreateMaidSettings(Path maidDir, EmergencyRescueVoiceSettings defaults) {
        if (maidDir == null) {
            return MaidCustomSettings.defaults();
        }
        ensureDirectory(maidDir);
        Path configPath = maidDir.resolve(MAID_CONFIG_NAME);
        MaidCustomSettings fallback = MaidCustomSettings.fromVoiceSettings(defaults);
        if (!Files.isRegularFile(configPath)) {
            saveSettings(configPath, fallback);
            return fallback;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String sourceModeRaw = root.has("source_mode") ? root.get("source_mode").getAsString() : fallback.sourceMode().serializedName();
            String playModeRaw = root.has("play_mode") ? root.get("play_mode").getAsString() : fallback.playMode().serializedName();
            String fixedFile = root.has("fixed_file") ? root.get("fixed_file").getAsString() : fallback.fixedFile();
            boolean useCommonFallback = root.has("use_common_fallback")
                    ? root.get("use_common_fallback").getAsBoolean()
                    : fallback.useCommonFallback();
            return new MaidCustomSettings(
                    EmergencyRescueVoiceSettings.SourceMode.fromSerializedName(sourceModeRaw),
                    EmergencyRescueVoiceSettings.CustomPlayMode.fromSerializedName(playModeRaw),
                    normalizeFileName(fixedFile),
                    useCommonFallback
            );
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to read rescue voice config '{}', rewriting defaults.", configPath, ex);
            saveSettings(configPath, fallback);
            return fallback;
        }
    }

    public static void saveSettings(Path configPath, MaidCustomSettings settings) {
        if (configPath == null || settings == null) {
            return;
        }
        try {
            ensureDirectory(configPath.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("source_mode", settings.sourceMode().serializedName());
            root.addProperty("play_mode", settings.playMode().serializedName());
            root.addProperty("fixed_file", settings.fixedFile());
            root.addProperty("use_common_fallback", settings.useCommonFallback());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to save rescue voice config '{}'", configPath, ex);
        }
    }

    public static String buildMaidFolderName(String maidUuid, String maidDisplayName) {
        String shortId = extractShortId(maidUuid);
        String name = sanitizeName(maidDisplayName);
        return name + "_" + shortId;
    }

    public static Path maidConfigPath(Path maidDir) {
        if (maidDir == null) {
            return null;
        }
        ensureDirectory(maidDir);
        return maidDir.resolve(MAID_CONFIG_NAME);
    }

    public static void saveMaidSettings(Path maidDir, EmergencyRescueVoiceSettings settings) {
        if (maidDir == null) {
            return;
        }
        saveSettings(maidConfigPath(maidDir), MaidCustomSettings.fromVoiceSettings(settings));
    }

    public static String sanitizeServerId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default_server";
        }
        String sanitized = raw.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "default_server";
        }
        return sanitized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeFileName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().replace('\\', '/');
        if (trimmed.contains("/") || trimmed.contains("..")) {
            return "";
        }
        return trimmed;
    }

    private static String sanitizeName(String rawName) {
        String base = rawName == null || rawName.isBlank() ? "maid" : rawName.trim();
        base = Normalizer.normalize(base, Normalizer.Form.NFKC);
        base = base.replaceAll("[\\r\\n\\t]", " ");
        base = base.replaceAll("[^\\p{L}\\p{N} _.-]", "_");
        base = base.replaceAll("\\s+", "_");
        base = base.replaceAll("_+", "_");
        base = base.replaceAll("^[_.-]+", "");
        base = base.replaceAll("[_.-]+$", "");
        if (base.isBlank()) {
            return "maid";
        }
        return base.length() > 40 ? base.substring(0, 40) : base;
    }

    private static String extractShortId(String maidUuid) {
        if (maidUuid != null && !maidUuid.isBlank()) {
            String normalized = maidUuid.trim().toLowerCase(Locale.ROOT);
            String hexOnly = normalized.replaceAll("[^0-9a-f]", "");
            if (hexOnly.length() >= 8) {
                return hexOnly.substring(0, 8);
            }
            if (!hexOnly.isBlank()) {
                return String.format(Locale.ROOT, "%-8s", hexOnly).replace(' ', '0');
            }
        }
        return "00000000";
    }

    private static void ensureBaseDirs() {
        ensureDirectory(RESCUE_ROOT);
        ensureDirectory(MAIDS_ROOT);
        ensureDirectory(COMMON_ROOT);
        ensureDirectory(SERVER_SYNC_ROOT);
    }

    private static void ensureDirectory(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path);
        } catch (IOException | InvalidPathException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to create rescue voice directory '{}'", path, ex);
        }
    }

    public record MaidCustomSettings(
            EmergencyRescueVoiceSettings.SourceMode sourceMode,
            EmergencyRescueVoiceSettings.CustomPlayMode playMode,
            String fixedFile,
            boolean useCommonFallback
    ) {
        public MaidCustomSettings {
            sourceMode = sourceMode == null ? EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS : sourceMode;
            playMode = playMode == null ? EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM : playMode;
            fixedFile = fixedFile == null ? "" : fixedFile.trim();
        }

        public static MaidCustomSettings defaults() {
            return new MaidCustomSettings(EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS, EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM, "", true);
        }

        public static MaidCustomSettings fromVoiceSettings(EmergencyRescueVoiceSettings settings) {
            if (settings == null) {
                return defaults();
            }
            return new MaidCustomSettings(settings.sourceMode(), settings.customPlayMode(), settings.fixedFile(), settings.useCommonFallback());
        }
    }
}
