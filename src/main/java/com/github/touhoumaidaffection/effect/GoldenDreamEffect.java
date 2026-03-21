package com.github.touhoumaidaffection.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class GoldenDreamEffect extends MobEffect {
    public GoldenDreamEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE7C75F);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) {
                toRemove.add(effect.getEffect());
            }
        }
        toRemove.forEach(entity::removeEffect);
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0F + amplifier * 0.5F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
