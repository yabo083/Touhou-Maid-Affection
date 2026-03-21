package com.github.touhoumaidaffection.client;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BondClientStateCache {
    private static final Map<UUID, MaidBondClientState> STATES = new ConcurrentHashMap<>();

    private BondClientStateCache() {
    }

    public static void update(UUID maidUuid, Set<String> unlockedAbilityIds, int queuedGiftCount, int maxQueuedGiftCount, int nextGiftReadySeconds) {
        STATES.put(maidUuid, new MaidBondClientState(
                new LinkedHashSet<>(unlockedAbilityIds),
                Math.max(0, queuedGiftCount),
                Math.max(1, maxQueuedGiftCount),
                Math.max(0, nextGiftReadySeconds)
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

    public static void clear() {
        STATES.clear();
    }

    private record MaidBondClientState(Set<String> unlockedAbilityIds, int queuedGiftCount, int maxQueuedGiftCount, int nextGiftReadySeconds) {
        private static final MaidBondClientState EMPTY = new MaidBondClientState(Set.of(), 0, 1, 0);
    }
}
