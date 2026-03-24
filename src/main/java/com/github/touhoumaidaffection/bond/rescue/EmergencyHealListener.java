package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondData;
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

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyHealListener {
    private EmergencyHealListener() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        ensureRescueChargesUpToDate(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        ensureRescueChargesUpToDate(player);
        float healthAfterHit = player.getHealth() - event.getAmount();
        float threshold = ModConfig.BOND_EMERGENCY_RESCUE_HEALTH_THRESHOLD.get();
        if (healthAfterHit > 0 && healthAfterHit > threshold) {
            return;
        }

        String consumedRescuerId = EmergencyRescueData.consumeOne(player);
        if (consumedRescuerId.isBlank()) {
            return;
        }

        event.setAmount(0);
        player.setHealth(Math.max(player.getMaxHealth() * 0.5f, threshold));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));

        BondData.MaidProfileSnapshot profile = resolveRescueProfile(player, consumedRescuerId);
        PacketDistributor.sendToPlayer(player, new MaidRescuePopPayload(
                consumedRescuerId,
                profile.modelId().isBlank() ? consumedRescuerId : profile.modelId(),
                profile.displayName(),
                profile.ysmModelId(),
                profile.ysmTexture(),
                profile.ysmDisplayName(),
                profile.rescueActionId()
        ));
    }


    private static BondData.MaidProfileSnapshot resolveRescueProfile(ServerPlayer player, String rescuerId) {
        BondData.MaidProfileSnapshot byProvider = BondManager.findMaidProfileByRescueProviderId(player, rescuerId);
        if (!isEmptyProfile(byProvider)) {
            return byProvider;
        }
        try {
            java.util.UUID maidUuid = java.util.UUID.fromString(rescuerId);
            return BondData.of(player).getMaidProfile(maidUuid);
        } catch (IllegalArgumentException ignored) {
            return BondManager.findMaidProfileByModelId(player, rescuerId);
        }
    }

    private static boolean isEmptyProfile(BondData.MaidProfileSnapshot profile) {
        return profile.modelId().isBlank()
                && profile.displayName().isBlank()
                && profile.ysmModelId().isBlank()
                && profile.ysmTexture().isBlank()
                && profile.ysmDisplayName().isBlank()
                && profile.rescueActionId().isBlank();
    }

    public static void ensureRescueChargesUpToDate(ServerPlayer player) {
        long currentDay = getCurrentRescueDay(player);
        long lastReplenishDay = EmergencyRescueData.getLastReplenishDay(player);

        // Migrate old saves that stored rescue refresh day using total game uptime.
        // Those values can be far larger than the world date derived from dayTime,
        // which would permanently block refreshes after switching to day-based logic.
        if (ModConfig.BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME.get() && lastReplenishDay > currentDay + 1L) {
            lastReplenishDay = currentDay > 0L ? currentDay - 1L : 0L;
            EmergencyRescueData.setLastReplenishDay(player, lastReplenishDay);
        }

        if (needsRescuerIdMigration(player)) {
            EmergencyRescueData.setAvailableRescuerIds(player, EmergencyRescueData.buildDailyRescuerList(player));
            EmergencyRescueData.setRegisteredRescuers(player, com.github.touhoumaidaffection.bond.BondManager.getUnlockedMaidIdsForAbility(player, "emergency_heal"));
        }

        if (currentDay <= lastReplenishDay) {
            return;
        }
        EmergencyRescueData.setAvailableRescuerIds(player, EmergencyRescueData.buildDailyRescuerList(player));
        EmergencyRescueData.setRegisteredRescuers(player, com.github.touhoumaidaffection.bond.BondManager.getUnlockedMaidIdsForAbility(player, "emergency_heal"));
        EmergencyRescueData.setLastReplenishDay(player, currentDay);
    }


    private static boolean needsRescuerIdMigration(ServerPlayer player) {
        for (String rescuerId : EmergencyRescueData.getAvailableRescuerIds(player)) {
            try {
                java.util.UUID.fromString(rescuerId);
            } catch (IllegalArgumentException ex) {
                return true;
            }
        }
        return false;
    }

    public static long getCurrentRescueDay(ServerPlayer player) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME.get()) {
            return Math.floorDiv(player.level().getDayTime(), 24000L);
        }
        return Math.floorDiv(player.level().getGameTime(), 24000L);
    }
}
