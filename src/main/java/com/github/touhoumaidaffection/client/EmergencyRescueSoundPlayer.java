package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.SoundData;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class EmergencyRescueSoundPlayer {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("music.menu"));
    private static final Map<String, Integer> SEQUENCE_INDEX_CACHE = new HashMap<>();

    private EmergencyRescueSoundPlayer() {
    }

    public static void play(MaidRescuePopPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (tryPlayByConfiguredSource(payload, minecraft)) {
            return;
        }

        ResourceLocation soundId = resolveSoundEventId(payload.rescueSoundEventId());
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
        minecraft.player.playSound(soundEvent, 1.0F, 1.0F);
    }

    public static void invalidateCaches() {
        SEQUENCE_INDEX_CACHE.clear();
    }

    private static boolean tryPlayByConfiguredSource(MaidRescuePopPayload payload, Minecraft minecraft) {
        EmergencyRescueVoiceSettings voiceSettings = EmergencyRescueVoiceSettings.of(
                payload.rescueVoiceSourceMode(),
                payload.rescueVoiceTlmMode(),
                payload.rescueVoiceTlmGroup(),
                payload.rescueVoiceTlmClip(),
                payload.rescueVoiceCustomPlayMode(),
                payload.rescueVoiceFixedFile(),
                payload.rescueVoiceUseCommonFallback()
        );
        if (voiceSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK) {
            if (tryPlayTlmPackVoice(payload, voiceSettings, minecraft)) {
                return true;
            }
            if (payload.allowClientCustomSound() && tryPlayCustomFileVoice(payload, voiceSettings, minecraft)) {
                return true;
            }
            return false;
        }
        if (payload.allowClientCustomSound() && tryPlayCustomFileVoice(payload, voiceSettings, minecraft)) {
            return true;
        }
        return false;
    }

    private static boolean tryPlayTlmPackVoice(MaidRescuePopPayload payload, EmergencyRescueVoiceSettings settings, Minecraft minecraft) {
        String soundPackId = payload.maidSoundPackId();
        if (soundPackId == null || soundPackId.isBlank()) {
            debugLog("Emergency rescue TLM voice skipped: empty sound pack id");
            return false;
        }
        List<RescueTlmVoiceIndex.VoiceEntry> pool = selectTlmVoicePool(soundPackId, settings);
        if (pool.isEmpty()) {
            debugLog("Emergency rescue TLM voice skipped: no entries in pack {}", soundPackId);
            return false;
        }
        RandomSource random = minecraft.level != null ? minecraft.level.random : RandomSource.create();
        RescueTlmVoiceIndex.VoiceEntry entry = pool.get(random.nextInt(pool.size()));
        SoundBuffer soundBuffer = RescueTlmVoiceIndex.loadSoundBuffer(soundPackId, entry.clipKey());
        if (soundBuffer == null) {
            debugLog("Emergency rescue TLM voice buffer missing: pack={}, clip={}", soundPackId, entry.clipKey());
            return false;
        }
        try {
            minecraft.getSoundManager().play(new EmergencyRescueCustomSoundInstance(
                    SoundEvent.createVariableRangeEvent(entry.soundEventId()),
                    soundBuffer,
                    minecraft.player.getX(),
                    minecraft.player.getY(),
                    minecraft.player.getZ(),
                    1.0F,
                    1.0F
            ));
            debugLog("Emergency rescue TLM voice played: pack={}, clip={}", soundPackId, entry.clipKey());
            return true;
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to play emergency rescue TLM voice entry '{}'", entry.clipKey(), ex);
            return false;
        }
    }

    private static List<RescueTlmVoiceIndex.VoiceEntry> selectTlmVoicePool(String soundPackId, EmergencyRescueVoiceSettings settings) {
        List<RescueTlmVoiceIndex.VoiceEntry> pool = switch (settings.tlmPlayMode()) {
            case RANDOM_ALL -> RescueTlmVoiceIndex.getEntries(soundPackId);
            case RANDOM_GROUP -> RescueTlmVoiceIndex.getEntriesForGroup(soundPackId, settings.tlmSelectedGroup());
            case SPECIFIC_CLIP -> {
                RescueTlmVoiceIndex.VoiceEntry entry = RescueTlmVoiceIndex.getEntry(soundPackId, settings.tlmSelectedClip());
                yield entry == null ? List.of() : List.of(entry);
            }
        };
        if (pool.isEmpty()) {
            pool = RescueTlmVoiceIndex.getEntries(soundPackId);
        }
        return pool;
    }

    private static boolean tryPlayCustomFileVoice(MaidRescuePopPayload payload, EmergencyRescueVoiceSettings settings, Minecraft minecraft) {
        String requiredFormat = normalizeRequiredFormat(payload.requiredClientCustomSoundFormat());
        double maxDuration = sanitizeMaxDuration(payload.maxClientCustomSoundDurationSeconds());
        float volume = sanitizeVolume(EmergencyRescueLocalSoundConfig.getSettings().volume());
        float pitch = sanitizePitch(EmergencyRescueLocalSoundConfig.getSettings().pitch());

        Path localMaidDir = EmergencyRescueCustomVoiceConfig.localMaidDir(payload.maidUuid(), payload.maidDisplayName());
        EmergencyRescueVoiceSettings defaults = new EmergencyRescueVoiceSettings(
                settings.sourceMode(),
                settings.tlmPlayMode(),
                settings.tlmSelectedGroup(),
                settings.tlmSelectedClip(),
                settings.customPlayMode(),
                settings.fixedFile(),
                settings.useCommonFallback()
        );
        EmergencyRescueCustomVoiceConfig.MaidCustomSettings localSettings =
                EmergencyRescueCustomVoiceConfig.loadOrCreateMaidSettings(localMaidDir, defaults);
        boolean fallbackToCommon = localSettings.useCommonFallback();
        if (!settings.useCommonFallback() && fallbackToCommon) {
            fallbackToCommon = false;
        }

        String serverId = EmergencyRescueServerSoundSyncClient.getActiveServerId();
        List<Path> localMaidFiles = listAudioFiles(localMaidDir);
        if (tryPlayFromPool("local_maid", payload.maidUuid(), localSettings, localMaidFiles, requiredFormat, maxDuration, volume, pitch, minecraft)) {
            return true;
        }

        if (fallbackToCommon) {
            List<Path> localCommonFiles = listAudioFiles(EmergencyRescueCustomVoiceConfig.localCommonDir());
            if (tryPlayFromPool("local_common", payload.maidUuid(), localSettings, localCommonFiles, requiredFormat, maxDuration, volume, pitch, minecraft)) {
                return true;
            }
        }

        List<Path> syncedMaidFiles = listAudioFiles(EmergencyRescueCustomVoiceConfig.syncedMaidDir(serverId, payload.maidUuid(), payload.maidDisplayName()));
        if (tryPlayFromPool("synced_maid", payload.maidUuid(), localSettings, syncedMaidFiles, requiredFormat, maxDuration, volume, pitch, minecraft)) {
            return true;
        }

        if (fallbackToCommon) {
            List<Path> syncedCommonFiles = listAudioFiles(EmergencyRescueCustomVoiceConfig.syncedCommonDir(serverId));
            if (tryPlayFromPool("synced_common", payload.maidUuid(), localSettings, syncedCommonFiles, requiredFormat, maxDuration, volume, pitch, minecraft)) {
                return true;
            }
        }

        Path legacyFile = EmergencyRescueCustomVoiceConfig.legacyCustomFile();
        if (Files.isRegularFile(legacyFile) && tryPlaySingleFile("legacy", legacyFile, requiredFormat, maxDuration, volume, pitch, minecraft)) {
            return true;
        }
        return false;
    }

    private static boolean tryPlayFromPool(
            String scope,
            String maidId,
            EmergencyRescueCustomVoiceConfig.MaidCustomSettings settings,
            List<Path> pool,
            String requiredFormat,
            double maxDuration,
            float volume,
            float pitch,
            Minecraft minecraft
    ) {
        if (pool.isEmpty()) {
            return false;
        }
        List<Path> ordered = orderPool(scope, maidId, settings, pool);
        for (Path candidate : ordered) {
            if (tryPlaySingleFile(scope, candidate, requiredFormat, maxDuration, volume, pitch, minecraft)) {
                return true;
            }
        }
        return false;
    }

    private static List<Path> orderPool(
            String scope,
            String maidId,
            EmergencyRescueCustomVoiceConfig.MaidCustomSettings settings,
            List<Path> pool
    ) {
        List<Path> sorted = pool.stream()
                .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(ArrayList::new));
        if (sorted.isEmpty()) {
            return sorted;
        }
        String key = scope + "|" + maidId;
        switch (settings.playMode()) {
            case RANDOM -> {
                java.util.Collections.shuffle(sorted);
                return sorted;
            }
            case FIXED -> {
                if (!settings.fixedFile().isBlank()) {
                    for (int i = 0; i < sorted.size(); i++) {
                        if (settings.fixedFile().equalsIgnoreCase(sorted.get(i).getFileName().toString())) {
                            Path fixed = sorted.remove(i);
                            sorted.add(0, fixed);
                            return sorted;
                        }
                    }
                }
                return sorted;
            }
            case SEQUENTIAL -> {
                int current = SEQUENCE_INDEX_CACHE.getOrDefault(key, 0);
                if (current < 0) {
                    current = 0;
                }
                int offset = sorted.isEmpty() ? 0 : current % sorted.size();
                List<Path> rotated = new ArrayList<>(sorted.size());
                for (int i = 0; i < sorted.size(); i++) {
                    rotated.add(sorted.get((offset + i) % sorted.size()));
                }
                SEQUENCE_INDEX_CACHE.put(key, offset + 1);
                return rotated;
            }
            default -> {
                return sorted;
            }
        }
    }

    private static boolean tryPlaySingleFile(
            String scope,
            Path soundPath,
            String requiredFormat,
            double maxDuration,
            float volume,
            float pitch,
            Minecraft minecraft
    ) {
        if (soundPath == null || !Files.isRegularFile(soundPath)) {
            return false;
        }
        if (!isFormatCompatible(soundPath, requiredFormat)) {
            debugLog("Emergency rescue custom sound skipped (format mismatch): {}", soundPath);
            return false;
        }
        DecodedSound decoded = decodeLocalSound(soundPath);
        if (decoded == null) {
            return false;
        }
        if (decoded.durationSeconds() > maxDuration) {
            debugLog(
                    "Emergency rescue custom sound skipped (duration {}s > {}s): {}",
                    String.format(Locale.ROOT, "%.2f", decoded.durationSeconds()),
                    String.format(Locale.ROOT, "%.2f", maxDuration),
                    soundPath
            );
            return false;
        }
        try {
            minecraft.getSoundManager().play(new EmergencyRescueStreamSoundInstance(
                    STREAM_ANCHOR_SOUND_EVENT,
                    decoded.oggBytes(),
                    decoded.oggType(),
                    volume,
                    pitch
            ));
            TouhouMaidAffection.LOGGER.info(
                    "Emergency rescue custom sound stream play: scope='{}', file='{}', type={}, duration={}s, volume={}, pitch={}",
                    scope,
                    soundPath,
                    decoded.oggType(),
                    String.format(Locale.ROOT, "%.2f", decoded.durationSeconds()),
                    String.format(Locale.ROOT, "%.2f", volume),
                    String.format(Locale.ROOT, "%.2f", pitch)
            );
            return true;
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn(
                    "Failed to play emergency rescue custom sound '{}', fallback to server sound",
                    soundPath,
                    ex
            );
            return false;
        }
    }

    private static List<Path> listAudioFiles(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .toList();
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to list rescue custom voice files in '{}'", directory, ex);
            return List.of();
        }
    }

    private static String normalizeRequiredFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ogg";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    private static boolean isFormatCompatible(Path soundPath, String requiredFormat) {
        if (requiredFormat.isBlank()) {
            return true;
        }
        String fileName = soundPath.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith("." + requiredFormat);
    }

    private static double sanitizeMaxDuration(double raw) {
        if (!Double.isFinite(raw) || raw <= 0.0D) {
            return 4.0D;
        }
        return Math.max(0.1D, Math.min(30.0D, raw));
    }

    private static float sanitizeVolume(float raw) {
        if (!Float.isFinite(raw)) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(2.0F, raw));
    }

    private static float sanitizePitch(float raw) {
        if (!Float.isFinite(raw) || raw <= 0.0F) {
            return 1.0F;
        }
        // OpenAL pitch must be positive; keep a small guard above zero.
        return Math.max(0.01F, Math.min(2.0F, raw));
    }

    private static void debugLog(String message, Object... args) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG != null && ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG.get()) {
            TouhouMaidAffection.LOGGER.info(message, args);
        }
    }

    private static DecodedSound decodeLocalSound(Path soundPath) {
        byte[] oggBytes;
        OggReader.Type oggType;
        try {
            oggBytes = Files.readAllBytes(soundPath);
            oggType = OggReader.getOggType(oggBytes);
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn(
                    "Failed to read emergency rescue custom sound '{}', fallback to server sound",
                    soundPath,
                    ex
            );
            return null;
        }
        if (oggType == OggReader.Type.UNKNOWN) {
            TouhouMaidAffection.LOGGER.warn(
                    "Emergency rescue custom sound '{}' is not a supported OGG Vorbis/Opus stream, fallback to server sound",
                    soundPath
            );
            return null;
        }

        List<SoundData> sounds = new ArrayList<>(1);
        OggReader.readSoundDataFromFile(soundPath.toFile(), sounds, MarkerManager.getMarker("EmergencyRescueLocalSound"));
        if (sounds.isEmpty()) {
            TouhouMaidAffection.LOGGER.warn(
                    "Emergency rescue custom sound '{}' produced no decodable audio data, fallback to server sound",
                    soundPath
            );
            return null;
        }
        SoundData sound = sounds.getFirst();
        double durationSeconds = estimateDurationSeconds(sound);
        if (durationSeconds <= 0.0D) {
            durationSeconds = 0.0D;
        }
        return new DecodedSound(oggBytes, oggType, durationSeconds);
    }

    private static double estimateDurationSeconds(SoundData sound) {
        if (sound == null || sound.audioFormat() == null || sound.byteBuffer() == null) {
            return -1.0D;
        }
        int frameSize = sound.audioFormat().getFrameSize();
        float frameRate = sound.audioFormat().getFrameRate();
        if (frameSize <= 0 || frameRate <= 0) {
            return -1.0D;
        }
        int bytes = sound.byteBuffer().duplicate().remaining();
        return bytes / (double) frameSize / frameRate;
    }

    private static ResourceLocation resolveSoundEventId(String raw) {
        if (raw != null && !raw.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(raw.trim());
            if (parsed != null) {
                return parsed;
            }
        }
        return ResourceLocation.withDefaultNamespace("entity.player.levelup");
    }

    private record DecodedSound(byte[] oggBytes, OggReader.Type oggType, double durationSeconds) {
    }
}
