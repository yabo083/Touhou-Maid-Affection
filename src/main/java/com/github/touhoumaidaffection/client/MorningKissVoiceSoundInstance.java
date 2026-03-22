package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.api.client.sound.ICustomSoundBuffer;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;

public final class MorningKissVoiceSoundInstance extends AbstractTickableSoundInstance implements ICustomSoundBuffer {
    private final SoundBuffer soundBuffer;
    @Nullable
    private final EntityMaid maid;

    public MorningKissVoiceSoundInstance(SoundEvent soundEvent, SoundBuffer soundBuffer, @Nullable EntityMaid maid,
                                         double x, double y, double z, float volume, float pitch) {
        super(soundEvent, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.soundBuffer = soundBuffer;
        this.maid = maid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public boolean canPlaySound() {
        return maid == null || !maid.isSilent();
    }

    @Override
    public void tick() {
        if (maid == null) {
            return;
        }
        if (maid.isRemoved()) {
            stop();
            return;
        }
        x = maid.getX();
        y = maid.getY();
        z = maid.getZ();
    }

    @Nullable
    @Override
    public SoundBuffer getSoundBuffer() {
        return soundBuffer;
    }
}
