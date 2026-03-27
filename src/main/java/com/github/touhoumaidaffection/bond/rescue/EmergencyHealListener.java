package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyHealListener {
    private EmergencyHealListener() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        ensureRescueChargesUpToDate(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (!ModConfig.BOND_EMERGENCY_RESCUE_ENABLED.get() || !EmergencyRescueData.isRescueEnabled(player)) {
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

        ResolvedRescueProfile resolvedProfile = resolveRescueProfile(player, consumedRescuerId);
        BondData.MaidProfileSnapshot profile = resolvedProfile.profile();
        EmergencyRescueVoiceSettings rescueVoiceSettings = profile.rescueVoiceSettings() == null
                ? EmergencyRescueVoiceSettings.DEFAULT
                : profile.rescueVoiceSettings();
        debugRescueResolve(consumedRescuerId, resolvedProfile, rescueVoiceSettings);
        EmergencyRescueSoundProfileData.EmergencyRescueSoundProfile soundProfile = EmergencyRescueSoundProfileData.getActiveProfile();
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MaidRescuePopPayload(
                resolvedProfile.maidUuidForPayload(),
                profile.modelId().isBlank() ? resolvedProfile.maidUuidForPayload() : profile.modelId(),
                profile.displayName(),
                profile.soundPackId(),
                profile.ysmModelId(),
                profile.ysmTexture(),
                profile.ysmDisplayName(),
                profile.rescueActionId(),
                rescueVoiceSettings.sourceMode().serializedName(),
                rescueVoiceSettings.tlmPlayMode().serializedName(),
                rescueVoiceSettings.tlmSelectedGroup(),
                rescueVoiceSettings.tlmSelectedClip(),
                rescueVoiceSettings.customPlayMode().serializedName(),
                rescueVoiceSettings.fixedFile(),
                rescueVoiceSettings.useCommonFallback(),
                soundProfile.soundEventId(),
                soundProfile.allowClientOverride(),
                soundProfile.maxClientSoundDurationSeconds(),
                soundProfile.requiredClientSoundFormat()
        ));
    }


    private static ResolvedRescueProfile resolveRescueProfile(ServerPlayer player, String rescuerId) {
        BondData data = BondData.of(player);

        UUID resolvedUuid = EmergencyRescueData.resolveRescuerToMaidUuid(player, rescuerId);
        if (resolvedUuid != null) {
            return new ResolvedRescueProfile(resolvedUuid.toString(), data.getMaidProfile(resolvedUuid));
        }

        String lookupId = EmergencyRescueData.toLegacyLookupId(rescuerId);
        UUID byProviderUuid = data.findMaidUuidByRescueProviderId(lookupId, "emergency_heal");
        if (byProviderUuid != null) {
            return new ResolvedRescueProfile(byProviderUuid.toString(), data.getMaidProfile(byProviderUuid));
        }

        UUID parsedUuid = tryParseUuid(lookupId);
        if (parsedUuid != null) {
            BondData.MaidProfileSnapshot byUuid = data.getMaidProfile(parsedUuid);
            if (!isEmptyProfile(byUuid)) {
                return new ResolvedRescueProfile(parsedUuid.toString(), byUuid);
            }
        }

        UUID byModelUuid = data.findMaidUuidByModelId(lookupId);
        if (byModelUuid != null) {
            return new ResolvedRescueProfile(byModelUuid.toString(), data.getMaidProfile(byModelUuid));
        }

        BondData.MaidProfileSnapshot byModel = BondManager.findMaidProfileByModelId(player, lookupId);
        String payloadMaidUuid = parsedUuid == null ? lookupId : parsedUuid.toString();
        return new ResolvedRescueProfile(payloadMaidUuid, byModel);
    }

    private static UUID tryParseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isEmptyProfile(BondData.MaidProfileSnapshot profile) {
        return profile.modelId().isBlank()
                && profile.displayName().isBlank()
                && profile.ysmModelId().isBlank()
                && profile.ysmTexture().isBlank()
                && profile.ysmDisplayName().isBlank()
                && profile.rescueActionId().isBlank()
                && profile.soundPackId().isBlank();
    }

    public static void ensureRescueChargesUpToDate(ServerPlayer player) {
        long currentDay = getCurrentRescueDay(player);
        long lastReplenishDay = EmergencyRescueData.getLastReplenishDay(player);
        int chargesBefore = EmergencyRescueData.getChargeCount(player);
        int maxBefore = EmergencyRescueData.getMaxChargeCount(player);

        // Migrate old saves that stored rescue refresh day using total game uptime.
        // Those values can be far larger than the world date derived from dayTime,
        // which would permanently block refreshes after switching to day-based logic.
        if (ModConfig.BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME.get() && lastReplenishDay > currentDay + 1L) {
            lastReplenishDay = currentDay > 0L ? currentDay - 1L : 0L;
            EmergencyRescueData.setLastReplenishDay(player, lastReplenishDay);
        }

        EmergencyRescueData.normalizeRescuerState(player);

        if (currentDay <= lastReplenishDay) {
            debugRefreshState(player, currentDay, lastReplenishDay, chargesBefore, maxBefore, false);
            return;
        }
        EmergencyRescueData.setAvailableRescuerIds(player, EmergencyRescueData.buildDailyRescuerList(player));
        EmergencyRescueData.setRegisteredRescuers(player, com.github.touhoumaidaffection.bond.BondManager.getUnlockedMaidIdsForAbility(player, "emergency_heal"));
        EmergencyRescueData.setLastReplenishDay(player, currentDay);
        debugRefreshState(
                player,
                currentDay,
                lastReplenishDay,
                chargesBefore,
                maxBefore,
                true
        );
    }

    public static long getCurrentRescueDay(ServerPlayer player) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME.get()) {
            return Math.floorDiv(player.level().getDayTime(), 24000L);
        }
        return Math.floorDiv(player.level().getGameTime(), 24000L);
    }

    private static void debugRescueResolve(
            String consumedRescuerId,
            ResolvedRescueProfile resolvedProfile,
            EmergencyRescueVoiceSettings rescueVoiceSettings
    ) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG == null || !ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG.get()) {
            return;
        }
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue resolve: consumedId='{}' -> maidUuid='{}', sourceMode='{}', modelId='{}', displayName='{}'",
                consumedRescuerId,
                resolvedProfile.maidUuidForPayload(),
                rescueVoiceSettings.sourceMode().serializedName(),
                resolvedProfile.profile().modelId(),
                resolvedProfile.profile().displayName()
        );
    }

    private record ResolvedRescueProfile(String maidUuidForPayload, BondData.MaidProfileSnapshot profile) {
    }

    private static void debugRefreshState(
            ServerPlayer player,
            long currentDay,
            long lastReplenishDayBefore,
            int chargesBefore,
            int maxBefore,
            boolean refreshed
    ) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG == null || !ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG.get()) {
            return;
        }
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue refresh check: player={} refreshed={} currentDay={} lastDayBefore={} chargesBefore={}/{} chargesAfter={}/{}",
                player.getGameProfile().getName(),
                refreshed,
                currentDay,
                lastReplenishDayBefore,
                chargesBefore,
                maxBefore,
                EmergencyRescueData.getChargeCount(player),
                EmergencyRescueData.getMaxChargeCount(player)
        );
    }
}
