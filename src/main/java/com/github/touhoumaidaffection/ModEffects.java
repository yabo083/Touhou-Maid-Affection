package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.effect.MaidsPrayerEffect;
import com.github.touhoumaidaffection.effect.GoldenDreamEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, TouhouMaidAffection.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> MAIDS_PRAYER =
            MOB_EFFECTS.register("maids_prayer", MaidsPrayerEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> GOLDEN_DREAM =
            MOB_EFFECTS.register("golden_dream", GoldenDreamEffect::new);
}
