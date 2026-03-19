package com.github.touhoumaidaffection.bond;

import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class BondData {
    private static final String ROOT_KEY = "touhou_maid_affection.bond";

    private final CompoundTag persistent;
    private final CompoundTag root;

    private BondData(CompoundTag persistent, CompoundTag root) {
        this.persistent = persistent;
        this.root = root;
    }

    public static BondData of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new BondData(persistent, persistent.getCompound(ROOT_KEY));
    }

    public int getBondLevel(UUID maidUuid) {
        return root.getInt("BondLevel_" + maidUuid);
    }

    public void setBondLevel(UUID maidUuid, int level) {
        int normalized = Math.max(0, level);
        root.putInt("BondLevel_" + maidUuid, normalized);
        boolean unlocked = normalized >= BondConfig.DEFAULT_UNLOCK_LEVEL;
        root.putBoolean("BondUnlocked_" + maidUuid, unlocked);
        if (unlocked) {
            initializeAbilitiesIfNeeded(maidUuid);
        }
        save();
    }

    public boolean isBondUnlocked(UUID maidUuid) {
        return root.getBoolean("BondUnlocked_" + maidUuid);
    }

    public boolean getAbilityState(UUID maidUuid, String abilityId) {
        CompoundTag abilities = root.getCompound("BondAbilities_" + maidUuid);
        return abilities.getBoolean(abilityId);
    }

    public void setAbilityState(UUID maidUuid, String abilityId, boolean enabled) {
        CompoundTag abilities = root.getCompound("BondAbilities_" + maidUuid);
        abilities.putBoolean(abilityId, enabled);
        root.put("BondAbilities_" + maidUuid, abilities);
        save();
    }

    private void initializeAbilitiesIfNeeded(UUID maidUuid) {
        String abilitiesKey = "BondAbilities_" + maidUuid;
        if (root.contains(abilitiesKey)) {
            return;
        }
        CompoundTag abilities = new CompoundTag();
        BondAbilityManager.registerDefaults();
        BondAbilityManager.getAllAbilities().forEach(ability -> abilities.putBoolean(ability.getId(), true));
        root.put(abilitiesKey, abilities);
    }

    private void save() {
        persistent.put(ROOT_KEY, root);
    }
}
