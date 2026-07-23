package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class MorningKissVoiceSoundInstance extends TrackedEntityVoiceSoundInstance {
    private final byte[] data;
    private final String fileName;

    public MorningKissVoiceSoundInstance(SoundEvent soundEvent, byte[] data, String fileName, @Nullable EntityMaid maid,
                                         double x, double y, double z, float volume, float pitch) {
        super(soundEvent, SoundSource.NEUTRAL, maid, x, y, z, volume, pitch);
        this.data = data;
        this.fileName = fileName;
    }

    @Nullable
    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return InMemoryVoiceStream.openOgg(data, "morning kiss TLM voice " + fileName);
    }
}
