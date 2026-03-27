package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class MorningKissVoicePlayback {
    private MorningKissVoicePlayback() {
    }

    public static void play(MorningKissVoicePlayPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || payload.soundPackId().isBlank()) {
            return;
        }

        MorningKissVoiceSettings settings = MorningKissVoiceSettings.of(
                payload.mode(),
                payload.selectedGroup(),
                payload.selectedClip(),
                payload.soundPackId()
        );
        MorningKissVoiceIndex.VoiceEntry entry = selectEntry(payload.soundPackId(), settings, minecraft);
        if (entry == null) {
            return;
        }

        SoundBuffer soundBuffer = MorningKissVoiceIndex.loadSoundBuffer(payload.soundPackId(), entry.clipKey());
        if (soundBuffer == null) {
            return;
        }

        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(entry.soundEventId());
        Entity entity = minecraft.level.getEntity(payload.maidEntityId());
        if (entity instanceof EntityMaid maid) {
            minecraft.getSoundManager().play(new MorningKissVoiceSoundInstance(soundEvent, soundBuffer, maid, maid.getX(), maid.getY(), maid.getZ(), 1.0F, 1.0F));
            return;
        }

        double x = minecraft.player.getX();
        double y = minecraft.player.getY();
        double z = minecraft.player.getZ();
        minecraft.getSoundManager().play(new MorningKissVoiceSoundInstance(soundEvent, soundBuffer, null, x, y, z, 1.0F, 1.0F));
    }

    private static MorningKissVoiceIndex.VoiceEntry selectEntry(String soundPackId, MorningKissVoiceSettings settings, Minecraft minecraft) {
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
        return pool.get(minecraft.level.random.nextInt(pool.size()));
    }
}
