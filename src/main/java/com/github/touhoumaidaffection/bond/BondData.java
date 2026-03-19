package com.github.touhoumaidaffection.bond;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class BondData {
    private static final String ROOT_KEY = "touhou_maid_affection.bond";

    private final CompoundTag root;

    public BondData(CompoundTag root) {
        this.root = root;
    }

    public static BondData of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new BondData(persistent.getCompound(ROOT_KEY));
    }

    public int getBondLevel(UUID maidUuid) {
        return root.getInt("BondLevel_" + maidUuid);
    }

    public void setBondLevel(UUID maidUuid, int level) {
        root.putInt("BondLevel_" + maidUuid, level);
        root.putBoolean("BondUnlocked_" + maidUuid, level >= 3);
    }

    public boolean isBondUnlocked(UUID maidUuid) {
        return root.getBoolean("BondUnlocked_" + maidUuid);
    }

    public boolean getAbilityState(UUID maidUuid, String abilityId) {
        CompoundTag abilities = root.getCompound("BondAbilities_" + maidUuid);
        return abilities.getBoolean(abilityId);
    }
}