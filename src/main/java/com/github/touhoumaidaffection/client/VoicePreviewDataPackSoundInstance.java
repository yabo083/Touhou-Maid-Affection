package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.CompletableFuture;

public final class VoicePreviewDataPackSoundInstance extends TrackedEntityVoiceSoundInstance {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("music.menu"));

    private final byte[] data;
    private final OggReader.Type oggType;
    private final boolean mp3;
    private final String fileName;

    public VoicePreviewDataPackSoundInstance(byte[] data, OggReader.Type oggType, boolean mp3,
                                             String fileName, Entity trackedEntity,
                                             double x, double y, double z,
                                             float volume, float pitch) {
        super(STREAM_ANCHOR_SOUND_EVENT, SoundSource.PLAYERS, trackedEntity, x, y, z, volume, pitch);
        this.data = data;
        this.oggType = oggType;
        this.mp3 = mp3;
        this.fileName = fileName;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return InMemoryVoiceStream.open(data, oggType, mp3, "preview data-pack voice " + fileName);
    }
}
