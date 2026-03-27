package com.github.touhoumaidaffection;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, TouhouMaidAffection.MOD_ID);

    public static final RegistryObject<SoundEvent> KISS =
            SOUNDS.register("kiss", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(TouhouMaidAffection.MOD_ID, "touhou_maid_affection.kiss")
            ));
}
