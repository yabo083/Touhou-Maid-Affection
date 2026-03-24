package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.SoundData;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmergencyRescueSoundPlayer {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("music.menu"));

    private EmergencyRescueSoundPlayer() {
    }

    public static void play(MaidRescuePopPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (payload.allowClientCustomSound() && tryPlayClientOverride(payload, minecraft)) {
            return;
        }

        ResourceLocation soundId = resolveSoundEventId(payload.rescueSoundEventId());
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
        minecraft.player.playSound(soundEvent, 1.0F, 1.0F);
    }

    private static boolean tryPlayClientOverride(MaidRescuePopPayload payload, Minecraft minecraft) {
        EmergencyRescueLocalSoundConfig.LocalSoundSettings settings = EmergencyRescueLocalSoundConfig.getSettings();
        if (!settings.enabled()) {
            return false;
        }

        Path soundPath = EmergencyRescueLocalSoundConfig.resolveSoundFile(settings);
        if (soundPath == null || !Files.isRegularFile(soundPath)) {
            return false;
        }

        String requiredFormat = normalizeRequiredFormat(payload.requiredClientCustomSoundFormat());
        if (!isFormatCompatible(soundPath, requiredFormat)) {
            TouhouMaidAffection.LOGGER.warn(
                    "Emergency rescue custom sound '{}' does not match required format '{}'",
                    soundPath,
                    requiredFormat
            );
            return false;
        }

        DecodedSound decoded = decodeLocalSound(soundPath);
        if (decoded == null) {
            return false;
        }

        double maxDuration = sanitizeMaxDuration(payload.maxClientCustomSoundDurationSeconds());
        if (decoded.durationSeconds() > maxDuration) {
            TouhouMaidAffection.LOGGER.warn(
                    "Emergency rescue custom sound '{}' is too long ({}s > {}s), fallback to server sound",
                    soundPath,
                    String.format(Locale.ROOT, "%.2f", decoded.durationSeconds()),
                    String.format(Locale.ROOT, "%.2f", maxDuration)
            );
            return false;
        }

        float volume = sanitizeVolume(settings.volume());
        float pitch = sanitizePitch(settings.pitch());
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue custom sound stream play: file='{}', type={}, duration={}s, volume={}, pitch={}",
                soundPath,
                decoded.oggType(),
                String.format(Locale.ROOT, "%.2f", decoded.durationSeconds()),
                String.format(Locale.ROOT, "%.2f", volume),
                String.format(Locale.ROOT, "%.2f", pitch)
        );
        try {
            minecraft.getSoundManager().play(new EmergencyRescueStreamSoundInstance(
                    STREAM_ANCHOR_SOUND_EVENT,
                    decoded.oggBytes(),
                    decoded.oggType(),
                    volume,
                    pitch
            ));
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
