package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.api.client.sound.ICustomSoundBuffer;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;

public final class EmergencyRescueCustomSoundInstance extends AbstractTickableSoundInstance implements ICustomSoundBuffer {
    private final SoundBuffer soundBuffer;

    public EmergencyRescueCustomSoundInstance(SoundEvent soundEvent, SoundBuffer soundBuffer,
                                              double x, double y, double z, float volume, float pitch) {
        super(soundEvent, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.soundBuffer = soundBuffer;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        // Rescue sound should be a player-centric cue, not world-positional audio.
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    @Override
    public void tick() {
        // Static sound at trigger position.
    }

    @Nullable
    @Override
    public SoundBuffer getSoundBuffer() {
        return soundBuffer;
    }
}
