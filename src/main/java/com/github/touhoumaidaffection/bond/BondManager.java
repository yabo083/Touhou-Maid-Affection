package com.github.touhoumaidaffection.bond;

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

    public static boolean isBondUnlocked(ServerPlayer player, UUID maidUuid) {
        return BondData.of(player).isBondUnlocked(maidUuid);
    }

    public static boolean canUseAbility(ServerPlayer player, UUID maidUuid, String abilityId) {
        BondData data = BondData.of(player);
        return data.isBondUnlocked(maidUuid) && data.getAbilityState(maidUuid, abilityId);
    }
}