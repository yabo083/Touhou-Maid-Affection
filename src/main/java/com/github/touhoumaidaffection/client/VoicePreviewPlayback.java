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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class VoicePreviewPlayback {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("minecraft", "music.menu"));

    private VoicePreviewPlayback() {
    }

    public static boolean playMorningKiss(EntityMaid maid, String voiceId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || maid == null || voiceId == null || voiceId.isBlank()) {
            return false;
        }
        if (VoicePoolIds.BUILTIN_MORNING_KISS.equals(voiceId)) {
            minecraft.player.playSound(ModSounds.KISS.get(), 1.0F, 1.0F);
            return true;
        }
        if (VoicePoolIds.isDataPack(voiceId)) {
            TouhouMaidAffection.CHANNEL.sendToServer(new VoicePreviewRequestPayload(
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
        MorningKissVoiceIndex.VoiceEntry entry = MorningKissVoiceIndex.getEntry(soundPackId, VoicePoolIds.value(voiceId));
        MorningKissVoiceIndex.VoiceData voiceData = MorningKissVoiceIndex.loadVoiceData(soundPackId, VoicePoolIds.value(voiceId));
        if (entry == null || voiceData == null) {
            TouhouMaidAffection.LOGGER.warn("Morning kiss preview failed: pack={}, voiceId={}, entry={}, data={}",
                    soundPackId, voiceId, entry != null, voiceData != null);
            minecraft.player.displayClientMessage(Component.translatable("bond.voice_pool.preview.failed"), true);
            return false;
        }
        TouhouMaidAffection.LOGGER.info("Morning kiss preview playing TLM voice '{}' from pack '{}'", voiceId, soundPackId);
        minecraft.getSoundManager().play(new MorningKissVoiceSoundInstance(
                STREAM_ANCHOR_SOUND_EVENT,
                voiceData.data(),
                voiceData.fileName(),
                maid,
                maid.getX(),
                maid.getY(),
                maid.getZ(),
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
            TouhouMaidAffection.CHANNEL.sendToServer(new VoicePreviewRequestPayload(
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
        RescueTlmVoiceIndex.VoiceEntry entry = RescueTlmVoiceIndex.getEntry(soundPackId, VoicePoolIds.value(voiceId));
        RescueTlmVoiceIndex.VoiceData voiceData = RescueTlmVoiceIndex.loadVoiceData(soundPackId, VoicePoolIds.value(voiceId));
        if (entry == null || voiceData == null) {
            TouhouMaidAffection.LOGGER.warn("Emergency rescue preview failed: pack={}, voiceId={}, entry={}, data={}",
                    soundPackId, voiceId, entry != null, voiceData != null);
            minecraft.player.displayClientMessage(Component.translatable("bond.voice_pool.preview.failed"), true);
            return false;
        }
        TouhouMaidAffection.LOGGER.info("Emergency rescue preview playing TLM voice '{}' from pack '{}'", voiceId, soundPackId);
        minecraft.getSoundManager().play(new EmergencyRescueTlmSoundInstance(
                STREAM_ANCHOR_SOUND_EVENT,
                voiceData.data(),
                voiceData.fileName(),
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
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
        boolean mp3 = isMp3(payload.data(), payload.fileName());
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
                payload.feature(),
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

    private static boolean isMp3(byte[] data, String fileName) {
        if (data == null || data.length < 3) {
            return false;
        }
        boolean magic = (data[0] == 'I' && data[1] == 'D' && data[2] == '3')
                || ((data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0);
        if (magic) {
            TouhouMaidAffection.LOGGER.info("Detected preview MP3 voice '{}'", fileName);
        }
        return magic;
    }

    private static String safeSoundPackId(EntityMaid maid) {
        return maid == null || maid.getSoundPackId() == null ? "" : maid.getSoundPackId();
    }
}
