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
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EmergencyRescueService {
    private static final String EMERGENCY_HEAL_ABILITY_ID = "emergency_heal";
    private static final Map<UUID, Long> LAST_RESCUE_SUCCESS_TICK = new HashMap<>();

    private EmergencyRescueService() {
    }

    public static boolean fastGate(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return false;
        }
        return ModConfig.BOND_EMERGENCY_RESCUE_ENABLED.get() && EmergencyRescueData.isRescueEnabled(player);
    }

    public static boolean shouldAttemptByDamage(ServerPlayer player, float finalDamage) {
        if (player == null || finalDamage <= 0.0F) {
            return false;
        }
        float healthAfterHit = player.getHealth() - finalDamage;
        float threshold = ModConfig.BOND_EMERGENCY_RESCUE_HEALTH_THRESHOLD.get();
        return healthAfterHit <= 0.0F || healthAfterHit <= threshold;
    }

    public static void refreshChargesIfNeeded(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long currentDay = getCurrentRescueDay(player);
        long lastReplenishDay = EmergencyRescueData.getLastReplenishDay(player);

        // Migrate old saves that stored rescue refresh day using total game uptime.
        // Those values can be far larger than the world date derived from dayTime,
        // which would permanently block refreshes after switching to day-based logic.
        if (ModConfig.BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME.get() && lastReplenishDay > currentDay + 1L) {
            lastReplenishDay = currentDay > 0L ? currentDay - 1L : 0L;
            EmergencyRescueData.setLastReplenishDay(player, lastReplenishDay);
        }

        EmergencyRescueData.normalizeRescuerState(player);

        if (currentDay <= lastReplenishDay) {
            return;
        }
        EmergencyRescueData.setAvailableRescuerIds(player, EmergencyRescueData.buildDailyRescuerList(player));
        EmergencyRescueData.setRegisteredRescuers(player, BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID));
        EmergencyRescueData.setLastReplenishDay(player, currentDay);
    }

    public static long getCurrentRescueDay(ServerPlayer player) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME.get()) {
            return Math.floorDiv(player.level().getDayTime(), 24000L);
        }
        return Math.floorDiv(player.level().getGameTime(), 24000L);
    }

    public static boolean tryConsumeAndRescue(ServerPlayer player, String trigger) {
        if (!fastGate(player)) {
            return false;
        }
        if (alreadyRescuedThisTick(player)) {
            return false;
        }

        refreshChargesIfNeeded(player);
        String consumedRescuerId = EmergencyRescueData.consumeOne(player);
        if (consumedRescuerId.isBlank()) {
            return false;
        }

        float threshold = ModConfig.BOND_EMERGENCY_RESCUE_HEALTH_THRESHOLD.get();
        player.setHealth(Math.max(player.getMaxHealth() * 0.5F, threshold));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        markRescueSuccess(player);

        sendRescuePayload(player, consumedRescuerId, trigger == null ? "unknown" : trigger);
        return true;
    }

    public static void clearRuntimeState(ServerPlayer player) {
        if (player == null) {
            return;
        }
        LAST_RESCUE_SUCCESS_TICK.remove(player.getUUID());
    }

    private static boolean alreadyRescuedThisTick(ServerPlayer player) {
        long currentTick = player.level().getGameTime();
        Long lastTick = LAST_RESCUE_SUCCESS_TICK.get(player.getUUID());
        return lastTick != null && lastTick == currentTick;
    }

    private static void markRescueSuccess(ServerPlayer player) {
        LAST_RESCUE_SUCCESS_TICK.put(player.getUUID(), player.level().getGameTime());
    }

    private static void sendRescuePayload(ServerPlayer player, String consumedRescuerId, String trigger) {
        ResolvedRescueProfile resolvedProfile = resolveRescueProfile(player, consumedRescuerId);
        BondData.MaidProfileSnapshot profile = resolvedProfile.profile();
        EmergencyRescueVoiceSettings rescueVoiceSettings = profile.rescueVoiceSettings() == null
                ? EmergencyRescueVoiceSettings.DEFAULT
                : profile.rescueVoiceSettings();
        debugRescueResolve(consumedRescuerId, resolvedProfile, rescueVoiceSettings, trigger);

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
        UUID byProviderUuid = data.findMaidUuidByRescueProviderId(lookupId, EMERGENCY_HEAL_ABILITY_ID);
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

    private static void debugRescueResolve(
            String consumedRescuerId,
            ResolvedRescueProfile resolvedProfile,
            EmergencyRescueVoiceSettings rescueVoiceSettings,
            String trigger
    ) {
        if (ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG == null || !ModConfig.BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG.get()) {
            return;
        }
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue [{}]: consumedId='{}' -> maidUuid='{}', sourceMode='{}', modelId='{}', displayName='{}'",
                trigger,
                consumedRescuerId,
                resolvedProfile.maidUuidForPayload(),
                rescueVoiceSettings.sourceMode().serializedName(),
                resolvedProfile.profile().modelId(),
                resolvedProfile.profile().displayName()
        );
    }

    private record ResolvedRescueProfile(String maidUuidForPayload, BondData.MaidProfileSnapshot profile) {
    }
}

