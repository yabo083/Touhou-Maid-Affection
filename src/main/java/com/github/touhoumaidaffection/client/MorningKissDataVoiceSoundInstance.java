package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.OpusAudioStream;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

public final class MorningKissDataVoiceSoundInstance extends AbstractTickableSoundInstance {
    private final byte[] data;
    private final OggReader.Type oggType;
    private final String fileName;
    private final Entity trackedEntity;

    public MorningKissDataVoiceSoundInstance(SoundEvent soundEvent, byte[] data, OggReader.Type oggType,
                                             String fileName, Entity trackedEntity,
                                             double x, double y, double z,
                                             float volume, float pitch) {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.data = data;
        this.oggType = oggType;
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
        TouhouMaidAffection.LOGGER.info(
                "Morning kiss data-pack voice getStream invoked: file='{}', oggType={}, bytes={}, looping={}",
                fileName,
                oggType,
                data == null ? -1 : data.length,
                looping
        );
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (oggType == OggReader.Type.OPUS) {
                    return new OpusAudioStream(data);
                }
                if (oggType == OggReader.Type.VORBIS) {
                    return new JOrbisAudioStream(new ByteArrayInputStream(data));
                }
                TouhouMaidAffection.LOGGER.warn("Morning kiss data-pack voice skipped: unsupported OGG type {}", oggType);
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to stream morning kiss data-pack voice '{}'", fileName, ex);
            }
            return null;
        }, Util.backgroundExecutor());
    }
}
