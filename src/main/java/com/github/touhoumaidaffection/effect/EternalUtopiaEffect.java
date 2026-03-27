package com.github.touhoumaidaffection.effect;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class EternalUtopiaEffect extends MobEffect {
    public EternalUtopiaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAEEFA3);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        resetTimeSinceRest(entity);

        if (entity.tickCount % 20 == 0) {
            clearHarmfulEffects(entity);
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0F);
            }
            if (entity instanceof Player player) {
                int beforeFood = player.getFoodData().getFoodLevel();
                float beforeSat = player.getFoodData().getSaturationLevel();
                player.getFoodData().eat(1, 2.0F);
                float boostedSat = Math.min(player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel() + 1.0F);
                player.getFoodData().setSaturation(boostedSat);
                if (entity instanceof ServerPlayer serverPlayer) {
                    TouhouMaidAffection.LOGGER.info(
                            "[EternalUtopia] Tick apply: player={} hp={}/{} food={}=>{} sat={}=>{}",
                            serverPlayer.getScoreboardName(),
                            format(serverPlayer.getHealth()),
                            format(serverPlayer.getMaxHealth()),
                            beforeFood,
                            serverPlayer.getFoodData().getFoodLevel(),
                            format(beforeSat),
                            format(serverPlayer.getFoodData().getSaturationLevel())
                    );
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    private static void clearHarmfulEffects(LivingEntity entity) {
        List<MobEffect> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                toRemove.add(effect.getEffect());
            }
        }
        for (MobEffect effect : toRemove) {
            entity.removeEffect(effect);
        }
    }

    private static void resetTimeSinceRest(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
