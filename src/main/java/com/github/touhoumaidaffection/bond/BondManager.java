package com.github.touhoumaidaffection.bond;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;

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
        BondData.of(player).setMaidModelId(maid.getUUID(), maid.getModelId().toString());
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

    public static List<String> getUnlockedMaidModelIdsForAbility(ServerPlayer player, String abilityId) {
        return BondData.of(player).getUnlockedMaidModelIdsForAbility(abilityId);
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
        int intervalMinutes = Math.max(1, ModConfig.BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES.get());
        data.initializeRandomGiftState(maidUuid, nowMs, intervalMinutes);

        int maxQueued = Math.max(1, ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get());
        long intervalMs = intervalMinutes * 60_000L;

        int queued = Math.min(maxQueued, data.getQueuedGiftCount(maidUuid));
        long lastWallClock = data.getLastGiftWallClockMs(maidUuid);
        int lastIntervalMinutes = data.getLastGiftIntervalMinutes(maidUuid);
        if (lastIntervalMinutes > 0 && lastIntervalMinutes != intervalMinutes) {
            data.setQueuedGiftCount(maidUuid, queued);
            data.setLastGiftWallClockMs(maidUuid, nowMs);
            data.setLastGiftIntervalMinutes(maidUuid, intervalMinutes);
            return queued;
        }
        if (lastWallClock <= 0L) {
            data.setLastGiftWallClockMs(maidUuid, nowMs);
            data.setLastGiftIntervalMinutes(maidUuid, intervalMinutes);
            if (queued != data.getQueuedGiftCount(maidUuid)) {
                data.setQueuedGiftCount(maidUuid, queued);
            }
            return queued;
        }

        if (queued >= maxQueued) {
            data.setQueuedGiftCount(maidUuid, maxQueued);
            data.setLastGiftWallClockMs(maidUuid, nowMs);
            data.setLastGiftIntervalMinutes(maidUuid, intervalMinutes);
            return maxQueued;
        }

        long elapsed = Math.max(0L, nowMs - lastWallClock);
        int produced = (int) Math.min(Integer.MAX_VALUE, elapsed / intervalMs);
        if (produced <= 0) {
            if (queued != data.getQueuedGiftCount(maidUuid)) {
                data.setQueuedGiftCount(maidUuid, queued);
            }
            return queued;
        }

        int updatedQueue = Math.min(maxQueued, queued + produced);
        if (updatedQueue >= maxQueued) {
            data.setQueuedGiftCount(maidUuid, maxQueued);
            data.setLastGiftWallClockMs(maidUuid, nowMs);
            data.setLastGiftIntervalMinutes(maidUuid, intervalMinutes);
            return maxQueued;
        }

        data.setQueuedGiftCount(maidUuid, updatedQueue);
        data.setLastGiftWallClockMs(maidUuid, lastWallClock + produced * intervalMs);
        data.setLastGiftIntervalMinutes(maidUuid, intervalMinutes);
        return updatedQueue;
    }

    public static long getNextRandomGiftReadyAtMs(ServerPlayer player, UUID maidUuid, long nowMs) {
        BondData data = BondData.of(player);
        int intervalMinutes = Math.max(1, ModConfig.BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES.get());
        data.initializeRandomGiftState(maidUuid, nowMs, intervalMinutes);

        int maxQueued = Math.max(1, ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get());
        if (data.getQueuedGiftCount(maidUuid) >= maxQueued) {
            return 0L;
        }

        long lastWallClock = data.getLastGiftWallClockMs(maidUuid);
        if (lastWallClock <= 0L) {
            return nowMs + intervalMinutes * 60_000L;
        }
        return lastWallClock + intervalMinutes * 60_000L;
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
}
