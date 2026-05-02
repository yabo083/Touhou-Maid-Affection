package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModSounds;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.network.VoicePreviewDataPackPlayPayload;
import com.github.touhoumaidaffection.network.VoicePreviewRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VoicePreviewPlayback {
    private VoicePreviewPlayback() {
    }

    public static boolean playMorningKiss(EntityMaid maid, String voiceId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || maid == null || voiceId == null || voiceId.isBlank()) {
            return false;
        }
        if (VoicePoolIds.BUILTIN_MORNING_KISS.equals(voiceId)) {
            TouhouMaidAffection.LOGGER.info("Morning kiss preview playing built-in kiss sound");
            minecraft.player.playSound(ModSounds.KISS.get(), 1.0F, 1.0F);
            return true;
        }
        if (VoicePoolIds.isDataPack(voiceId)) {
            TouhouMaidAffection.LOGGER.info("Morning kiss preview requesting data-pack voice '{}'", voiceId);
            PacketDistributor.sendToServer(new VoicePreviewRequestPayload(
                    maid.getUUID(),
                    VoicePreviewRequestPayload.FEATURE_MORNING_KISS,
                    voiceId
            ));
            return true;
        }
        if (!VoicePoolIds.isTlm(voiceId)) {
            return false;
        }
        String soundPackId = safeSoundPackId(maid);
        String clipKey = VoicePoolIds.value(voiceId);
        MorningKissVoiceIndex.VoiceEntry entry = MorningKissVoiceIndex.getEntry(soundPackId, clipKey);
        MorningKissVoiceIndex.VoiceData voiceData = MorningKissVoiceIndex.loadVoiceData(soundPackId, clipKey);
        if (entry == null || voiceData == null) {
            TouhouMaidAffection.LOGGER.warn("Morning kiss preview failed: pack={}, clip={}, entry={}, data={}",
                    soundPackId, clipKey, entry != null, voiceData != null);
            minecraft.player.displayClientMessage(Component.translatable("bond.voice_pool.preview.failed"), true);
            return false;
        }
        TouhouMaidAffection.LOGGER.info("Morning kiss preview playing TLM voice: pack={}, clip={}, file={}, bytes={}",
                soundPackId, clipKey, voiceData.fileName(), voiceData.data().length);
        minecraft.getSoundManager().play(new VoicePreviewTlmSoundInstance(
                voiceData.data(),
                voiceData.fileName(),
                1.0F,
                1.0F
        ));
        return true;
    }

    public static boolean playEmergencyRescue(EntityMaid maid, String voiceId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || maid == null || voiceId == null || voiceId.isBlank()) {
            return false;
        }
        if (VoicePoolIds.isDataPack(voiceId)) {
            TouhouMaidAffection.LOGGER.info("Emergency rescue preview requesting data-pack voice '{}'", voiceId);
            PacketDistributor.sendToServer(new VoicePreviewRequestPayload(
                    maid.getUUID(),
                    VoicePreviewRequestPayload.FEATURE_EMERGENCY_RESCUE,
                    voiceId
            ));
            return true;
        }
        if (!VoicePoolIds.isTlm(voiceId)) {
            return false;
        }
        String soundPackId = safeSoundPackId(maid);
        String clipKey = VoicePoolIds.value(voiceId);
        RescueTlmVoiceIndex.VoiceEntry entry = RescueTlmVoiceIndex.getEntry(soundPackId, clipKey);
        RescueTlmVoiceIndex.VoiceData voiceData = RescueTlmVoiceIndex.loadVoiceData(soundPackId, clipKey);
        if (entry == null || voiceData == null) {
            TouhouMaidAffection.LOGGER.warn("Emergency rescue preview failed: pack={}, clip={}, entry={}, data={}",
                    soundPackId, clipKey, entry != null, voiceData != null);
            minecraft.player.displayClientMessage(Component.translatable("bond.voice_pool.preview.failed"), true);
            return false;
        }
        TouhouMaidAffection.LOGGER.info("Emergency rescue preview playing TLM voice: pack={}, clip={}, file={}, bytes={}",
                soundPackId, clipKey, voiceData.fileName(), voiceData.data().length);
        minecraft.getSoundManager().play(new VoicePreviewTlmSoundInstance(
                voiceData.data(),
                voiceData.fileName(),
                1.0F,
                1.0F
        ));
        return true;
    }

    public static void playDataPackVoice(VoicePreviewDataPackPlayPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || payload.data().length == 0) {
            return;
        }
        TouhouMaidAffection.LOGGER.info("Preview data-pack voice received: feature={}, file={}, bytes={}",
                payload.feature(), payload.fileName(), payload.data().length);
        boolean mp3 = isMp3(payload.data());
        OggReader.Type oggType = mp3 ? null : getOggType(payload.data(), payload.fileName());
        if (oggType == null && !mp3) {
            minecraft.player.displayClientMessage(Component.translatable("bond.voice_pool.preview.failed"), true);
            return;
        }
        var entity = minecraft.level.getEntity(payload.maidEntityId());
        double x = entity == null ? minecraft.player.getX() : entity.getX();
        double y = entity == null ? minecraft.player.getY() : entity.getY();
        double z = entity == null ? minecraft.player.getZ() : entity.getZ();
        minecraft.getSoundManager().play(new VoicePreviewDataPackSoundInstance(
                payload.data(),
                oggType,
                mp3,
                payload.fileName(),
                entity,
                x,
                y,
                z,
                1.0F,
                1.0F
        ));
    }

    private static OggReader.Type getOggType(byte[] data, String fileName) {
        try {
            OggReader.Type type = OggReader.getOggType(data);
            if (type == OggReader.Type.VORBIS || type == OggReader.Type.OPUS) {
                return type;
            }
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to inspect preview data-pack voice '{}'", fileName, ex);
        }
        return null;
    }

    private static boolean isMp3(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }
        return (data[0] == 'I' && data[1] == 'D' && data[2] == '3')
                || ((data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0);
    }

    private static String safeSoundPackId(EntityMaid maid) {
        return maid == null || maid.getSoundPackId() == null ? "" : maid.getSoundPackId();
    }
}
