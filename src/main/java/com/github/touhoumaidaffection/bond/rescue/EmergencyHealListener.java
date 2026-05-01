package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class EmergencyHealListener {
    private EmergencyHealListener() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (!EmergencyRescueService.fastGate(player)) {
            return;
        }
        if (!EmergencyRescueService.shouldAttemptByDamage(player, event.getNewDamage())) {
            return;
        }
        if (EmergencyRescueService.tryConsumeAndRescue(player, "damage_pre")) {
            event.setNewDamage(0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (!EmergencyRescueService.fastGate(player)) {
            return;
        }
        if (EmergencyRescueService.tryConsumeAndRescue(player, "death_fallback")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Logout happens during integrated-server teardown; keep this handler side-effect free
        // so shutdown does not try to re-enter the rescue service path.
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
    }

    public static void ensureRescueChargesUpToDate(ServerPlayer player) {
        EmergencyRescueService.refreshChargesIfNeeded(player);
    }

    public static long getCurrentRescueDay(ServerPlayer player) {
        return EmergencyRescueService.getCurrentRescueDay(player);
    }
}

