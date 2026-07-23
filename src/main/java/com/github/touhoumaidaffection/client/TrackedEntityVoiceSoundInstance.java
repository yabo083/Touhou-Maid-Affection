package com.github.touhoumaidaffection.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

abstract class TrackedEntityVoiceSoundInstance extends AbstractTickableSoundInstance {
    @Nullable
    private final Entity trackedEntity;

    protected TrackedEntityVoiceSoundInstance(
            SoundEvent soundEvent,
            SoundSource source,
            @Nullable Entity trackedEntity,
            double x,
            double y,
            double z,
            float volume,
            float pitch
    ) {
        super(soundEvent, source, SoundInstance.createUnseededRandom());
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
}
