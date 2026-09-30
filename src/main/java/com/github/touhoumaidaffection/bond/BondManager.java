package com.github.touhoumaidaffection.bond;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.bond.rescue.MaidRescueContributorId;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class BondManager {
    private BondManager() {
    }

    public static int getBondLevel(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getBondLevel(maidUuid);
    }

    public static void setBondLevel(ServerPlayer player, UUID maidUuid, int level) {
        BondData.of(player).setBondLevel(maidUuid, level);
    }

    public static void syncMaidProfile(ServerPlayer player, EntityMaid maid) {
        BondData data = BondData.of(player);
        data.setMaidModelId(maid.getUUID(), maid.getModelId().toString());
        data.setMaidDisplayName(maid.getUUID(), MaidDisplayNameResolver.resolvePlainDisplayName(maid));
        data.setMaidSoundPackId(maid.getUUID(), maid.getSoundPackId());
        String rescueProviderId = MaidRescueContributorId.ensure(maid);
        if (!rescueProviderId.isBlank()) {
            data.setMaidRescueProviderId(maid.getUUID(), rescueProviderId);
        }
        if (maid.isYsmModel()) {
            data.setMaidYsmProfile(
                    maid.getUUID(),
                    maid.getYsmModelId(),
                    maid.getYsmModelTexture(),
                    maid.getYsmModelName().getString()
            );
        } else {
            data.setMaidYsmProfile(maid.getUUID(), "", "", "");
        }
        data.setMaidLastSeen(maid.getUUID(), System.currentTimeMillis());
    }

    public static boolean isBondUnlocked(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).isBondUnlocked(maidUuid);
    }

    public static boolean isAbilityUnlocked(ServerPlayer player, UUID maidUuid, String abilityId) {
        BondData data = BondData.of(player);
        return data.isBondUnlocked(maidUuid) && data.isAbilityUnlocked(maidUuid, abilityId);
    }

    public static void unlockAbility(ServerPlayer player, UUID maidUuid, String abilityId) {
        BondData.of(player).unlockAbility(maidUuid, abilityId);
    }

    public static List<String> getUnlockedAbilityIds(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getUnlockedAbilityIds(maidUuid);
    }

    public static List<UUID> getUnlockedMaidIdsForAbility(ServerPlayer player, String abilityId) {
        return BondData.of(player).getUnlockedMaidIdsForAbility(abilityId);
    }

    public static String getMaidModelId(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMaidModelId(maidUuid);
    }

    public static BondData.MaidProfileSnapshot findMaidProfileByModelId(ServerPlayer player, String modelId) {
        return BondData.of(player).findMaidProfileByModelId(modelId);
    }

    public static String getMaidRescueAction(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMaidRescueAction(maidUuid);
    }

    public static void setMaidRescueAction(ServerPlayer player, UUID maidUuid, String actionId) {
        BondData.of(player).setMaidRescueAction(maidUuid, actionId);
    }

    public static String getMaidRescueProviderId(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMaidRescueProviderId(maidUuid);
    }

    public static LapPillowPoseSnapshot getMaidLapPillowPose(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMaidLapPillowPose(maidUuid);
    }

    public static void setMaidLapPillowPose(ServerPlayer player, UUID maidUuid, LapPillowPoseSnapshot pose) {
        BondData.of(player).setMaidLapPillowPose(maidUuid, pose);
    }

    public static int getQueuedGiftCount(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getQueuedGiftCount(maidUuid);
    }

    public static void setQueuedGiftCount(ServerPlayer player, UUID maidUuid, int count) {
        BondData.of(player).setQueuedGiftCount(maidUuid, count);
    }

    public static long getLastGiftWallClockMs(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getLastGiftWallClockMs(maidUuid);
    }

    public static void setLastGiftWallClockMs(ServerPlayer player, UUID maidUuid, long timestampMs) {
        BondData.of(player).setLastGiftWallClockMs(maidUuid, timestampMs);
    }

    public static long getLastGiftDeliveryGameTime(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getLastGiftDeliveryGameTime(maidUuid);
    }

    public static void setLastGiftDeliveryGameTime(ServerPlayer player, UUID maidUuid, long gameTime) {
        BondData.of(player).setLastGiftDeliveryGameTime(maidUuid, gameTime);
    }

    public static void initializeRandomGiftState(ServerPlayer player, UUID maidUuid, long nowMs) {
        BondData.of(player).initializeRandomGiftState(maidUuid, nowMs, Math.max(1, ModConfig.BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES.get()));
    }

    public static int reconcileRandomGiftQueue(ServerPlayer player, UUID maidUuid, long nowMs) {
        BondData data = BondData.of(player);
        int interval = Math.max(1, ModConfig.BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES.get());
        RandomGiftClock.State state = RandomGiftClock.reconcile(data.getQueuedGiftCount(maidUuid),
                data.getLastGiftWallClockMs(maidUuid), data.getLastGiftIntervalMinutes(maidUuid),
                interval, ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get(), nowMs);
        if (state.queued() != data.getQueuedGiftCount(maidUuid)) data.setQueuedGiftCount(maidUuid, state.queued());
        if (state.lastWallClockMs() != data.getLastGiftWallClockMs(maidUuid)) data.setLastGiftWallClockMs(maidUuid, state.lastWallClockMs());
        if (interval != data.getLastGiftIntervalMinutes(maidUuid)) data.setLastGiftIntervalMinutes(maidUuid, interval);
        return state.queued();
    }

    public static long getNextRandomGiftReadyAtMs(ServerPlayer player, UUID maidUuid, long nowMs) {
        BondData data = BondData.of(player);
        return RandomGiftClock.reconcile(data.getQueuedGiftCount(maidUuid),
                data.getLastGiftWallClockMs(maidUuid), data.getLastGiftIntervalMinutes(maidUuid),
                ModConfig.BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES.get(), ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get(), nowMs).nextReadyAtMs();
    }

    public static String getMorningKissLastSuccessfulWindowId(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMorningKissLastSuccessfulWindowId(maidUuid);
    }

    public static void setMorningKissLastSuccessfulWindowId(ServerPlayer player, UUID maidUuid, String windowId) {
        BondData.of(player).setMorningKissLastSuccessfulWindowId(maidUuid, windowId);
    }

    public static String getMorningKissLastFailedWindowId(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMorningKissLastFailedWindowId(maidUuid);
    }

    public static void setMorningKissLastFailedWindowId(ServerPlayer player, UUID maidUuid, String windowId) {
        BondData.of(player).setMorningKissLastFailedWindowId(maidUuid, windowId);
    }

    public static String getMorningKissScheduledWindowId(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMorningKissScheduledWindowId(maidUuid);
    }

    public static void setMorningKissScheduledWindowId(ServerPlayer player, UUID maidUuid, String windowId) {
        BondData.of(player).setMorningKissScheduledWindowId(maidUuid, windowId);
    }

    public static long getMorningKissScheduledAttemptTick(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMorningKissScheduledAttemptTick(maidUuid);
    }

    public static void setMorningKissScheduledAttemptTick(ServerPlayer player, UUID maidUuid, long tick) {
        BondData.of(player).setMorningKissScheduledAttemptTick(maidUuid, tick);
    }

    public static long getMorningKissLastAutoAttemptGameTime(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMorningKissLastAutoAttemptGameTime(maidUuid);
    }

    public static void setMorningKissLastAutoAttemptGameTime(ServerPlayer player, UUID maidUuid, long tick) {
        BondData.of(player).setMorningKissLastAutoAttemptGameTime(maidUuid, tick);
    }

    public static void clearMorningKissSchedule(ServerPlayer player, UUID maidUuid) {
        BondData.of(player).clearMorningKissSchedule(maidUuid);
    }

    public static String getMorningKissSelectedWindowId(ServerPlayer player) {
        return BondData.of(player).getMorningKissSelectedWindowId();
    }

    public static void setMorningKissSelectedWindowId(ServerPlayer player, String windowId) {
        BondData.of(player).setMorningKissSelectedWindowId(windowId);
    }

    public static String getMorningKissSelectedMaidId(ServerPlayer player) {
        return BondData.of(player).getMorningKissSelectedMaidId();
    }

    public static void setMorningKissSelectedMaidId(ServerPlayer player, String maidId) {
        BondData.of(player).setMorningKissSelectedMaidId(maidId);
    }

    public static void clearMorningKissSelectedMaid(ServerPlayer player) {
        BondData.of(player).clearMorningKissSelectedMaid();
    }

    public static MorningKissVoiceSettings getMorningKissVoiceSettings(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getMorningKissVoiceSettings(maidUuid);
    }

    public static void setMorningKissVoiceSettings(ServerPlayer player, UUID maidUuid, MorningKissVoiceSettings settings) {
        BondData.of(player).setMorningKissVoiceSettings(maidUuid, settings);
    }

    public static EmergencyRescueVoiceSettings getEmergencyRescueVoiceSettings(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).getEmergencyRescueVoiceSettings(maidUuid);
    }

    public static void setEmergencyRescueVoiceSettings(ServerPlayer player, UUID maidUuid, EmergencyRescueVoiceSettings settings) {
        BondData.of(player).setEmergencyRescueVoiceSettings(maidUuid, settings);
    }
}
