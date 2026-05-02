package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.MorningKissDataVoicePlayPayload;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class MorningKissVoicePlayback {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("music.menu"));

    private MorningKissVoicePlayback() {
    }

    public static void play(MorningKissVoicePlayPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || payload.soundPackId().isBlank()) {
            return;
        }

        if (VoicePoolIds.BUILTIN_MORNING_KISS.equals(payload.selectedVoiceId())) {
            return;
        }
        MorningKissVoiceIndex.VoiceEntry entry = selectEntry(payload.soundPackId(), payload);
        if (entry == null) {
            return;
        }

        MorningKissVoiceIndex.VoiceData voiceData = MorningKissVoiceIndex.loadVoiceData(payload.soundPackId(), entry.clipKey());
        if (voiceData == null) {
            return;
        }

        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(entry.soundEventId());
        Entity entity = minecraft.level.getEntity(payload.maidEntityId());
        if (entity instanceof EntityMaid maid) {
            minecraft.getSoundManager().play(new MorningKissVoiceSoundInstance(soundEvent, voiceData.data(), voiceData.fileName(), maid, maid.getX(), maid.getY(), maid.getZ(), 1.0F, 1.0F));
            return;
        }

        double x = minecraft.player.getX();
        double y = minecraft.player.getY();
        double z = minecraft.player.getZ();
        minecraft.getSoundManager().play(new MorningKissVoiceSoundInstance(soundEvent, voiceData.data(), voiceData.fileName(), null, x, y, z, 1.0F, 1.0F));
    }

    public static void playDataPackVoice(MorningKissDataVoicePlayPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || payload.data().length == 0) {
            return;
        }
        TouhouMaidAffection.LOGGER.info("Received morning kiss data-pack voice '{}' ({} bytes)",
                payload.fileName(), payload.data().length);
        boolean mp3 = isMp3(payload.data(), payload.fileName());
        OggReader.Type oggType = mp3 ? null : getOggType(payload.data(), payload.fileName());
        if (oggType == null && !mp3) {
            return;
        }

        Entity entity = minecraft.level.getEntity(payload.maidEntityId());
        double x = entity != null ? entity.getX() : minecraft.player.getX();
        double y = entity != null ? entity.getY() : minecraft.player.getY();
        double z = entity != null ? entity.getZ() : minecraft.player.getZ();
        minecraft.getSoundManager().play(new MorningKissDataVoiceSoundInstance(
                STREAM_ANCHOR_SOUND_EVENT,
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
                TouhouMaidAffection.LOGGER.info("Detected morning kiss data-pack {} voice '{}'", type, fileName);
                return type;
            }
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to inspect morning kiss data-pack voice '{}'", fileName, ex);
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
            TouhouMaidAffection.LOGGER.info("Detected morning kiss MP3 voice '{}'", fileName);
        }
        return magic;
    }

    private static MorningKissVoiceIndex.VoiceEntry selectEntry(String soundPackId, MorningKissVoicePlayPayload payload) {
        if (VoicePoolIds.isTlm(payload.selectedVoiceId())) {
            return MorningKissVoiceIndex.getEntry(soundPackId, VoicePoolIds.value(payload.selectedVoiceId()));
        }
        MorningKissVoiceSettings settings = MorningKissVoiceSettings.of(
                payload.mode(),
                payload.selectedGroup(),
                payload.selectedClip(),
                payload.soundPackId()
        );
        List<MorningKissVoiceIndex.VoiceEntry> pool = switch (settings.mode()) {
            case RANDOM_ALL -> MorningKissVoiceIndex.getEntries(soundPackId);
            case RANDOM_GROUP -> MorningKissVoiceIndex.getEntriesForGroup(soundPackId, settings.selectedGroup());
            case SPECIFIC_CLIP -> {
                MorningKissVoiceIndex.VoiceEntry entry = MorningKissVoiceIndex.getEntry(soundPackId, settings.selectedClip());
                yield entry == null ? List.of() : List.of(entry);
            }
        };
        if (pool.isEmpty()) {
            pool = MorningKissVoiceIndex.getEntries(soundPackId);
        }
        if (pool.isEmpty()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return pool.get(minecraft.level == null ? 0 : minecraft.level.random.nextInt(pool.size()));
    }
}
