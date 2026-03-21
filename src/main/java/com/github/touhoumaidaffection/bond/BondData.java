package com.github.touhoumaidaffection.bond;

import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BondData {
    private static final String ROOT_KEY = "touhou_maid_affection.bond";
    private static final int CURRENT_ABILITY_DATA_VERSION = 2;

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
            migrateAbilityDataIfNeeded(maidUuid);
        }
        save();
    }

    public boolean isBondUnlocked(UUID maidUuid) {
        return root.getBoolean("BondUnlocked_" + maidUuid);
    }

    public boolean isAbilityUnlocked(UUID maidUuid, String abilityId) {
        migrateAbilityDataIfNeeded(maidUuid);
        CompoundTag abilities = root.getCompound(getAbilitiesKey(maidUuid));
        return abilities.getBoolean(abilityId);
    }

    public void unlockAbility(UUID maidUuid, String abilityId) {
        migrateAbilityDataIfNeeded(maidUuid);
        CompoundTag abilities = root.getCompound(getAbilitiesKey(maidUuid));
        abilities.putBoolean(abilityId, true);
        root.put(getAbilitiesKey(maidUuid), abilities);
        save();
    }

    public List<String> getUnlockedAbilityIds(UUID maidUuid) {
        migrateAbilityDataIfNeeded(maidUuid);
        CompoundTag abilities = root.getCompound(getAbilitiesKey(maidUuid));
        List<String> result = new ArrayList<>();
        BondAbilityManager.registerDefaults();
        BondAbilityManager.getAllAbilities().forEach(ability -> {
            if (abilities.getBoolean(ability.getId())) {
                result.add(ability.getId());
            }
        });
        return result;
    }

    public void setMaidModelId(UUID maidUuid, String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return;
        }
        root.putString("BondMaidModel_" + maidUuid, modelId);
        save();
    }

    public String getMaidModelId(UUID maidUuid) {
        return root.getString("BondMaidModel_" + maidUuid);
    }

    public List<String> getUnlockedMaidModelIdsForAbility(String abilityId) {
        List<String> result = new ArrayList<>();
        for (String key : root.getAllKeys()) {
            if (!key.startsWith("BondUnlocked_") || !root.getBoolean(key)) {
                continue;
            }
            String uuidPart = key.substring("BondUnlocked_".length());
            UUID maidUuid;
            try {
                maidUuid = UUID.fromString(uuidPart);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (isAbilityUnlocked(maidUuid, abilityId)) {
                String modelId = getMaidModelId(maidUuid);
                result.add(modelId.isBlank() ? maidUuid.toString() : modelId);
            }
        }
        return result;
    }

    public int getQueuedGiftCount(UUID maidUuid) {
        return Math.max(0, root.getInt("RandomGiftQueue_" + maidUuid));
    }

    public void setQueuedGiftCount(UUID maidUuid, int count) {
        root.putInt("RandomGiftQueue_" + maidUuid, Math.max(0, count));
        save();
    }

    public long getLastGiftWallClockMs(UUID maidUuid) {
        return root.getLong("RandomGiftLastWallClock_" + maidUuid);
    }

    public void setLastGiftWallClockMs(UUID maidUuid, long timestampMs) {
        root.putLong("RandomGiftLastWallClock_" + maidUuid, Math.max(0L, timestampMs));
        save();
    }

    public long getLastGiftDeliveryGameTime(UUID maidUuid) {
        return root.getLong("RandomGiftLastDelivery_" + maidUuid);
    }

    public void setLastGiftDeliveryGameTime(UUID maidUuid, long gameTime) {
        root.putLong("RandomGiftLastDelivery_" + maidUuid, Math.max(0L, gameTime));
        save();
    }

    public int getLastGiftIntervalMinutes(UUID maidUuid) {
        return Math.max(0, root.getInt("RandomGiftLastIntervalMinutes_" + maidUuid));
    }

    public void setLastGiftIntervalMinutes(UUID maidUuid, int intervalMinutes) {
        root.putInt("RandomGiftLastIntervalMinutes_" + maidUuid, Math.max(0, intervalMinutes));
        save();
    }

    public void initializeRandomGiftState(UUID maidUuid, long nowMs) {
        initializeRandomGiftState(maidUuid, nowMs, 0);
    }

    public void initializeRandomGiftState(UUID maidUuid, long nowMs, int intervalMinutes) {
        String queueKey = "RandomGiftQueue_" + maidUuid;
        String wallClockKey = "RandomGiftLastWallClock_" + maidUuid;
        String deliveryKey = "RandomGiftLastDelivery_" + maidUuid;
        String intervalKey = "RandomGiftLastIntervalMinutes_" + maidUuid;
        if (!root.contains(queueKey)) {
            root.putInt(queueKey, 0);
        }
        if (!root.contains(wallClockKey)) {
            root.putLong(wallClockKey, Math.max(0L, nowMs));
        }
        if (!root.contains(deliveryKey)) {
            root.putLong(deliveryKey, 0L);
        }
        if (!root.contains(intervalKey)) {
            root.putInt(intervalKey, Math.max(0, intervalMinutes));
        }
        save();
    }

    private void migrateAbilityDataIfNeeded(UUID maidUuid) {
        String abilitiesKey = getAbilitiesKey(maidUuid);
        String versionKey = getAbilityVersionKey(maidUuid);
        if (root.getInt(versionKey) >= CURRENT_ABILITY_DATA_VERSION && root.contains(abilitiesKey)) {
            return;
        }
        CompoundTag abilities = new CompoundTag();
        BondAbilityManager.registerDefaults();
        BondAbilityManager.getAllAbilities().forEach(ability -> abilities.putBoolean(ability.getId(), false));
        root.put(abilitiesKey, abilities);
        root.putInt(versionKey, CURRENT_ABILITY_DATA_VERSION);
        save();
    }

    private String getAbilitiesKey(UUID maidUuid) {
        return "BondAbilities_" + maidUuid;
    }

    private String getAbilityVersionKey(UUID maidUuid) {
        return "BondAbilityVersion_" + maidUuid;
    }

    private void save() {
        persistent.put(ROOT_KEY, root);
    }
}
