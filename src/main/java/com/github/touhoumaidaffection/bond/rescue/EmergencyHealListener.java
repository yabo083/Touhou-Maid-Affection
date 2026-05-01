package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class EmergencyHealListener {
    private EmergencyHealListener() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (!EmergencyRescueService.fastGate(player)) {
            return;
        }
        if (!EmergencyRescueService.shouldAttemptByDamage(player, event.getAmount())) {
            return;
        }
        if (EmergencyRescueService.tryConsumeAndRescue(player, "living_hurt")) {
            event.setAmount(0.0F);
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
