package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

import java.util.List;

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

        if (tryPlayDataPackVoice(payload, minecraft)) {
            return;
        }

        EmergencyRescueVoiceSettings voiceSettings = readVoiceSettings(payload);
        if (tryPlayTlmPackVoice(payload, voiceSettings, minecraft)) {
            return;
        }

        ResourceLocation soundId = resolveSoundEventId(payload.rescueSoundEventId());
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
        minecraft.player.playSound(soundEvent, 1.0F, 1.0F);
    }

    public static void invalidateCaches() {
    }

    private static EmergencyRescueVoiceSettings readVoiceSettings(MaidRescuePopPayload payload) {
        return EmergencyRescueVoiceSettings.of(
                payload.rescueVoiceSourceMode(),
                payload.rescueVoiceTlmMode(),
                payload.rescueVoiceTlmGroup(),
                payload.rescueVoiceTlmClip(),
                "",
                "",
                true
        );
    }

    private static boolean tryPlayDataPackVoice(MaidRescuePopPayload payload, Minecraft minecraft) {
        byte[] data = payload.dataPackVoiceData();
        if (data == null || data.length == 0) {
            return false;
        }
        try {
            OggReader.Type oggType = OggReader.getOggType(data);
            if (oggType == OggReader.Type.UNKNOWN) {
                TouhouMaidAffection.LOGGER.warn(
                        "Emergency rescue data-pack voice '{}' is not a supported OGG Vorbis/Opus stream, fallback to TLM/default sound.",
                        payload.dataPackVoiceFileName()
                );
                return false;
            }
            minecraft.getSoundManager().play(new EmergencyRescueStreamSoundInstance(
                    STREAM_ANCHOR_SOUND_EVENT,
                    data,
                    oggType,
                    1.0F,
                    1.0F
            ));
            TouhouMaidAffection.LOGGER.info(
                    "Emergency rescue data-pack voice played: file='{}', type={}, bytes={}",
                    payload.dataPackVoiceFileName(),
                    oggType,
                    data.length
            );
            return true;
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to play emergency rescue data-pack voice '{}'",
                    payload.dataPackVoiceFileName(), ex);
            return false;
        }
    }

    private static boolean tryPlayTlmPackVoice(MaidRescuePopPayload payload, EmergencyRescueVoiceSettings settings, Minecraft minecraft) {
        String soundPackId = payload.maidSoundPackId();
        if (soundPackId == null || soundPackId.isBlank()) {
            debugLog("Emergency rescue TLM voice skipped: empty sound pack id");
            return false;
        }
        if (VoicePoolIds.isTlm(payload.rescueVoiceSelectedId())) {
            RescueTlmVoiceIndex.VoiceEntry entry = RescueTlmVoiceIndex.getEntry(soundPackId, VoicePoolIds.value(payload.rescueVoiceSelectedId()));
            return entry != null && playTlmEntry(soundPackId, entry, minecraft);
        }
        List<RescueTlmVoiceIndex.VoiceEntry> pool = selectTlmVoicePool(soundPackId, settings);
        if (pool.isEmpty()) {
            debugLog("Emergency rescue TLM voice skipped: no entries in pack {}", soundPackId);
            return false;
        }
        RandomSource random = minecraft.level != null ? minecraft.level.random : RandomSource.create();
        int startIndex = random.nextInt(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            RescueTlmVoiceIndex.VoiceEntry entry = pool.get((startIndex + i) % pool.size());
            if (playTlmEntry(soundPackId, entry, minecraft)) {
                return true;
            }
        }
        debugLog("Emergency rescue TLM voice skipped: no playable entries in pack {}", soundPackId);
        return false;
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

    private static boolean playTlmEntry(String soundPackId, RescueTlmVoiceIndex.VoiceEntry entry, Minecraft minecraft) {
        RescueTlmVoiceIndex.VoiceData voiceData = RescueTlmVoiceIndex.loadVoiceData(soundPackId, entry.clipKey());
        if (voiceData == null) {
            debugLog("Emergency rescue TLM voice data missing: pack={}, clip={}", soundPackId, entry.clipKey());
            return false;
        }
        try {
            minecraft.getSoundManager().play(new EmergencyRescueTlmSoundInstance(
                    SoundEvent.createVariableRangeEvent(entry.soundEventId()),
                    voiceData.data(),
                    voiceData.fileName(),
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

    private static ResourceLocation resolveSoundEventId(String raw) {
        if (raw != null && !raw.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(raw.trim());
            if (parsed != null) {
                return parsed;
            }
        }
        return ResourceLocation.withDefaultNamespace("entity.player.levelup");
    }

    private static void debugLog(String message, Object... args) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG != null && ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG.get()) {
            TouhouMaidAffection.LOGGER.info(message, args);
        }
    }
}
