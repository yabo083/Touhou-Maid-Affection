package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;

public final class BondClientStateCache {
    private static final Map<UUID, MaidBondClientState> STATES = new ConcurrentHashMap<>();

    private BondClientStateCache() {
    }

    public static void update(UUID maidUuid, Set<String> unlockedAbilityIds, int queuedGiftCount, int maxQueuedGiftCount, int nextGiftReadySeconds,
                              MorningKissVoiceSettings morningKissVoiceSettings,
                              LapPillowPoseSnapshot lapPillowPose) {
        STATES.put(maidUuid, new MaidBondClientState(
                new LinkedHashSet<>(unlockedAbilityIds),
                Math.max(0, queuedGiftCount),
                Math.max(1, maxQueuedGiftCount),
                Math.max(0, nextGiftReadySeconds),
                morningKissVoiceSettings == null ? MorningKissVoiceSettings.DEFAULT : morningKissVoiceSettings,
                lapPillowPose == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : lapPillowPose.clamp()
        ));
    }

    public static boolean isAbilityUnlocked(UUID maidUuid, String abilityId) {
        return STATES.getOrDefault(maidUuid, MaidBondClientState.EMPTY).unlockedAbilityIds().contains(abilityId);
    }

    public static int getQueuedGiftCount(UUID maidUuid) {
        return STATES.getOrDefault(maidUuid, MaidBondClientState.EMPTY).queuedGiftCount();
    }

    public static int getMaxQueuedGiftCount(UUID maidUuid) {
        return STATES.getOrDefault(maidUuid, MaidBondClientState.EMPTY).maxQueuedGiftCount();
    }

    public static int getNextGiftReadySeconds(UUID maidUuid) {
        return STATES.getOrDefault(maidUuid, MaidBondClientState.EMPTY).nextGiftReadySeconds();
    }

    public static MorningKissVoiceSettings getMorningKissVoiceSettings(UUID maidUuid) {
        return STATES.getOrDefault(maidUuid, MaidBondClientState.EMPTY).morningKissVoiceSettings();
    }

    public static LapPillowPoseSnapshot getLapPillowPose(UUID maidUuid) {
        return STATES.getOrDefault(maidUuid, MaidBondClientState.EMPTY).lapPillowPose();
    }

    public static void updateMorningKissVoiceSettings(UUID maidUuid, MorningKissVoiceSettings settings) {
        STATES.compute(maidUuid, (ignored, current) -> {
            MaidBondClientState base = current == null ? MaidBondClientState.EMPTY : current;
            return new MaidBondClientState(base.unlockedAbilityIds(), base.queuedGiftCount(), base.maxQueuedGiftCount(), base.nextGiftReadySeconds(),
                    settings == null ? MorningKissVoiceSettings.DEFAULT : settings, base.lapPillowPose());
        });
    }

    public static void updateLapPillowPose(UUID maidUuid, LapPillowPoseSnapshot pose) {
        STATES.compute(maidUuid, (ignored, current) -> {
            MaidBondClientState base = current == null ? MaidBondClientState.EMPTY : current;
            return new MaidBondClientState(
                    base.unlockedAbilityIds(),
                    base.queuedGiftCount(),
                    base.maxQueuedGiftCount(),
                    base.nextGiftReadySeconds(),
                    base.morningKissVoiceSettings(),
                    pose == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : pose.clamp()
            );
        });
    }

    public static void clear() {
        STATES.clear();
    }

    private record MaidBondClientState(Set<String> unlockedAbilityIds, int queuedGiftCount, int maxQueuedGiftCount, int nextGiftReadySeconds,
                                       MorningKissVoiceSettings morningKissVoiceSettings,
                                       LapPillowPoseSnapshot lapPillowPose) {
        private static final MaidBondClientState EMPTY = new MaidBondClientState(Set.of(), 0, 1, 0, MorningKissVoiceSettings.DEFAULT, LapPillowPoseSnapshot.maidSitPlayerLieDefault());
    }
}
