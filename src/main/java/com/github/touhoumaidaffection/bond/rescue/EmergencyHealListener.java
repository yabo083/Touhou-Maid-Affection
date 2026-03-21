package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyHealListener {
    private EmergencyHealListener() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        refreshIfNeeded(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        refreshIfNeeded(player);
        float healthAfterHit = player.getHealth() - event.getAmount();
        float threshold = ModConfig.BOND_EMERGENCY_RESCUE_HEALTH_THRESHOLD.get();
        if (healthAfterHit > 0 && healthAfterHit > threshold) {
            return;
        }

        String consumedModel = EmergencyRescueData.consumeOne(player);
        if (consumedModel.isBlank()) {
            return;
        }

        event.setAmount(0);
        player.setHealth(Math.max(player.getMaxHealth() * 0.5f, threshold));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        PacketDistributor.sendToPlayer(player, new MaidRescuePopPayload(consumedModel));
    }

    private static void refreshIfNeeded(ServerPlayer player) {
        long currentDay = player.level().getGameTime() / 24000L;
        if (currentDay <= EmergencyRescueData.getLastReplenishDay(player)) {
            return;
        }
        List<String> available = BondManager.getUnlockedMaidModelIdsForAbility(player, "emergency_heal");
        EmergencyRescueData.setAvailableMaidModels(player, available);
        EmergencyRescueData.setLastReplenishDay(player, currentDay);
    }
}
