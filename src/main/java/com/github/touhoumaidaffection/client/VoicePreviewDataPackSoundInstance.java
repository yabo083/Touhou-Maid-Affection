package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.Mp3AudioStream;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.OpusAudioStream;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

public final class VoicePreviewDataPackSoundInstance extends AbstractTickableSoundInstance {
    private static final SoundEvent STREAM_ANCHOR_SOUND_EVENT =
            SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("music.menu"));

    private final byte[] data;
    private final OggReader.Type oggType;
    private final boolean mp3;
    private final String fileName;
    private final Entity trackedEntity;

    public VoicePreviewDataPackSoundInstance(byte[] data, OggReader.Type oggType, boolean mp3,
                                             String fileName, Entity trackedEntity,
                                             double x, double y, double z,
                                             float volume, float pitch) {
        super(STREAM_ANCHOR_SOUND_EVENT, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.data = data;
        this.oggType = oggType;
        this.mp3 = mp3;
        this.fileName = fileName;
        this.trackedEntity = trackedEntity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.looping = false;
        this.delay = 0;
    }

    @Override
    public boolean canPlaySound() {
        return trackedEntity == null || !trackedEntity.isSilent();
    }

    @Override
    public void tick() {
        if (trackedEntity == null) {
            return;
        }
        if (trackedEntity.isRemoved()) {
            stop();
            return;
        }
        x = trackedEntity.getX();
        y = trackedEntity.getY();
        z = trackedEntity.getZ();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (mp3) {
                    return new Mp3AudioStream(data);
                }
                if (oggType == OggReader.Type.OPUS) {
                    return new OpusAudioStream(data);
                }
                if (oggType == OggReader.Type.VORBIS) {
                    return new JOrbisAudioStream(new ByteArrayInputStream(data));
                }
                TouhouMaidAffection.LOGGER.warn("Preview voice '{}' skipped: unsupported OGG type {}", fileName, oggType);
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to stream preview voice '{}'", fileName, ex);
            }
            return null;
        }, Util.backgroundExecutor());
    }
}
