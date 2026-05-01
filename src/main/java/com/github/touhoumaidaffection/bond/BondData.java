package com.github.touhoumaidaffection.bond;

import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.lap.LapPillowMode;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
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

    public void setMaidDisplayName(UUID maidUuid, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        root.putString("BondMaidDisplayName_" + maidUuid, displayName);
        save();
    }

    public String getMaidDisplayName(UUID maidUuid) {
        return root.getString("BondMaidDisplayName_" + maidUuid);
    }

    public void setMaidSoundPackId(UUID maidUuid, String soundPackId) {
        root.putString("BondMaidSoundPack_" + maidUuid, soundPackId == null ? "" : soundPackId);
        save();
    }

    public String getMaidSoundPackId(UUID maidUuid) {
        return root.getString("BondMaidSoundPack_" + maidUuid);
    }

    public void setMaidYsmProfile(UUID maidUuid, String ysmModelId, String ysmTexture, String ysmDisplayName) {
        root.putString("BondMaidYsmModelId_" + maidUuid, ysmModelId == null ? "" : ysmModelId);
        root.putString("BondMaidYsmTexture_" + maidUuid, ysmTexture == null ? "" : ysmTexture);
        root.putString("BondMaidYsmDisplayName_" + maidUuid, ysmDisplayName == null ? "" : ysmDisplayName);
        save();
    }

    public void setMaidRescueAction(UUID maidUuid, String actionId) {
        root.putString("BondMaidRescueAction_" + maidUuid, actionId == null ? "" : actionId);
        save();
    }

    public String getMaidRescueAction(UUID maidUuid) {
        return root.getString("BondMaidRescueAction_" + maidUuid);
    }

    public void setMaidRescueProviderId(UUID maidUuid, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return;
        }
        String key = "BondMaidRescueProvider_" + maidUuid;
        if (providerId.equals(root.getString(key))) {
            return;
        }
        root.putString(key, providerId);
        save();
    }

    public String getMaidRescueProviderId(UUID maidUuid) {
        return root.getString("BondMaidRescueProvider_" + maidUuid);
    }

    public LapPillowPoseSnapshot getMaidLapPillowPose(UUID maidUuid) {
        String prefix = "BondMaidLapPillow_";
        String mode = root.getString(prefix + "Mode_" + maidUuid);
        if (mode.isBlank()) {
            return LapPillowPoseSnapshot.maidSitPlayerLieDefault();
        }
        return new LapPillowPoseSnapshot(
                LapPillowMode.fromName(mode),
                root.getDouble(prefix + "MaidOffsetX_" + maidUuid),
                root.getDouble(prefix + "MaidOffsetY_" + maidUuid),
                root.getDouble(prefix + "MaidOffsetZ_" + maidUuid),
                readPlayerOffset(root, prefix + "PlayerOffsetX_" + maidUuid, prefix + "OffsetX_" + maidUuid),
                readPlayerOffset(root, prefix + "PlayerOffsetY_" + maidUuid, prefix + "OffsetY_" + maidUuid),
                readPlayerOffset(root, prefix + "PlayerOffsetZ_" + maidUuid, prefix + "OffsetZ_" + maidUuid),
                root.getString(prefix + "MaidAction_" + maidUuid),
                root.getString(prefix + "PlayerAction_" + maidUuid)
        ).clamp();
    }

    public void setMaidLapPillowPose(UUID maidUuid, LapPillowPoseSnapshot pose) {
        LapPillowPoseSnapshot safe = pose == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : pose.clamp();
        String prefix = "BondMaidLapPillow_";
        root.putString(prefix + "Mode_" + maidUuid, safe.mode().serializedName());
        root.putDouble(prefix + "MaidOffsetX_" + maidUuid, safe.maidOffsetX());
        root.putDouble(prefix + "MaidOffsetY_" + maidUuid, safe.maidOffsetY());
        root.putDouble(prefix + "MaidOffsetZ_" + maidUuid, safe.maidOffsetZ());
        root.putDouble(prefix + "PlayerOffsetX_" + maidUuid, safe.playerOffsetX());
        root.putDouble(prefix + "PlayerOffsetY_" + maidUuid, safe.playerOffsetY());
        root.putDouble(prefix + "PlayerOffsetZ_" + maidUuid, safe.playerOffsetZ());
        root.putString(prefix + "MaidAction_" + maidUuid, safe.maidActionId());
        root.putString(prefix + "PlayerAction_" + maidUuid, safe.playerActionId());
        save();
    }

    private double readPlayerOffset(CompoundTag tag, String currentKey, String legacyKey) {
        if (tag.contains(currentKey)) {
            return tag.getDouble(currentKey);
        }
        return tag.getDouble(legacyKey);
    }

    public String getMaidYsmModelId(UUID maidUuid) {
        return root.getString("BondMaidYsmModelId_" + maidUuid);
    }

    public String getMaidYsmTexture(UUID maidUuid) {
        return root.getString("BondMaidYsmTexture_" + maidUuid);
    }

    public String getMaidYsmDisplayName(UUID maidUuid) {
        return root.getString("BondMaidYsmDisplayName_" + maidUuid);
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

    public List<UUID> getUnlockedMaidIdsForAbility(String abilityId) {
        List<UUID> result = new ArrayList<>();
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
                result.add(maidUuid);
            }
        }
        return result;
    }

    public MaidProfileSnapshot findMaidProfileByModelId(String modelId) {
        UUID maidUuid = findMaidUuidByModelId(modelId);
        return maidUuid == null ? MaidProfileSnapshot.empty() : getMaidProfile(maidUuid);
    }

    public UUID findMaidUuidByModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        for (String key : root.getAllKeys()) {
            if (!key.startsWith("BondMaidModel_")) {
                continue;
            }
            String storedModelId = root.getString(key);
            if (!modelId.equals(storedModelId)) {
                continue;
            }
            String uuidPart = key.substring("BondMaidModel_".length());
            try {
                return UUID.fromString(uuidPart);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy keys.
            }
        }
        return null;
    }

    public MaidProfileSnapshot findMaidProfileByRescueProviderId(String providerId) {
        UUID maidUuid = findMaidUuidByRescueProviderId(providerId);
        return maidUuid == null ? MaidProfileSnapshot.empty() : getMaidProfile(maidUuid);
    }

    public UUID findMaidUuidByRescueProviderId(String providerId) {
        return findMaidUuidByRescueProviderId(providerId, "");
    }

    public UUID findMaidUuidByRescueProviderId(String providerId, String preferredAbilityId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        List<UUID> matches = new ArrayList<>();
        for (String key : root.getAllKeys()) {
            if (!key.startsWith("BondMaidRescueProvider_")) {
                continue;
            }
            if (!providerId.equals(root.getString(key))) {
                continue;
            }
            String uuidPart = key.substring("BondMaidRescueProvider_".length());
            try {
                matches.add(UUID.fromString(uuidPart));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy keys.
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort(java.util.Comparator.comparing(UUID::toString));
        if (preferredAbilityId != null && !preferredAbilityId.isBlank()) {
            for (UUID candidate : matches) {
                if (isAbilityUnlocked(candidate, preferredAbilityId)) {
                    return candidate;
                }
            }
        }
        for (UUID candidate : matches) {
            if (isBondUnlocked(candidate)) {
                return candidate;
            }
        }
        return matches.getFirst();
    }

    public int resetAbilityForAllMaids(String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return 0;
        }
        Set<UUID> maidIds = collectKnownMaidIds();
        if (maidIds.isEmpty()) {
            return 0;
        }

        int resetCount = 0;
        boolean dirty = false;
        for (UUID maidUuid : maidIds) {
            migrateAbilityDataIfNeeded(maidUuid);
            String abilitiesKey = getAbilitiesKey(maidUuid);
            CompoundTag abilities = root.getCompound(abilitiesKey);
            boolean wasUnlocked = abilities.getBoolean(abilityId);
            if (wasUnlocked) {
                resetCount++;
            }
            if (wasUnlocked || !abilities.contains(abilityId)) {
                abilities.putBoolean(abilityId, false);
                root.put(abilitiesKey, abilities);
                dirty = true;
            }
        }
        if (dirty) {
            save();
        }
        return resetCount;
    }

    private Set<UUID> collectKnownMaidIds() {
        LinkedHashSet<UUID> maidIds = new LinkedHashSet<>();
        for (String key : root.getAllKeys()) {
            addMaidIdFromKey(maidIds, key, "BondUnlocked_");
            addMaidIdFromKey(maidIds, key, "BondAbilities_");
        }
        return maidIds;
    }

    private void addMaidIdFromKey(Set<UUID> output, String key, String prefix) {
        if (key == null || !key.startsWith(prefix)) {
            return;
        }
        String uuidPart = key.substring(prefix.length());
        try {
            output.add(UUID.fromString(uuidPart));
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed legacy keys.
        }
    }

    public MaidProfileSnapshot getMaidProfile(UUID maidUuid) {
        return new MaidProfileSnapshot(
                getMaidModelId(maidUuid),
                getMaidDisplayName(maidUuid),
                getMaidSoundPackId(maidUuid),
                getMaidYsmModelId(maidUuid),
                getMaidYsmTexture(maidUuid),
                getMaidYsmDisplayName(maidUuid),
                getMaidRescueAction(maidUuid),
                getEmergencyRescueVoiceSettings(maidUuid)
        );
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

    public String getMorningKissLastSuccessfulWindowId(UUID maidUuid) {
        return root.getString("MorningKissLastSuccessWindow_" + maidUuid);
    }

    public void setMorningKissLastSuccessfulWindowId(UUID maidUuid, String windowId) {
        root.putString("MorningKissLastSuccessWindow_" + maidUuid, windowId == null ? "" : windowId);
        save();
    }

    public String getMorningKissLastFailedWindowId(UUID maidUuid) {
        return root.getString("MorningKissLastFailedWindow_" + maidUuid);
    }

    public void setMorningKissLastFailedWindowId(UUID maidUuid, String windowId) {
        root.putString("MorningKissLastFailedWindow_" + maidUuid, windowId == null ? "" : windowId);
        save();
    }

    public String getMorningKissScheduledWindowId(UUID maidUuid) {
        return root.getString("MorningKissScheduledWindow_" + maidUuid);
    }

    public void setMorningKissScheduledWindowId(UUID maidUuid, String windowId) {
        root.putString("MorningKissScheduledWindow_" + maidUuid, windowId == null ? "" : windowId);
        save();
    }

    public long getMorningKissScheduledAttemptTick(UUID maidUuid) {
        return root.getLong("MorningKissScheduledAttemptTick_" + maidUuid);
    }

    public void setMorningKissScheduledAttemptTick(UUID maidUuid, long tick) {
        root.putLong("MorningKissScheduledAttemptTick_" + maidUuid, Math.max(0L, tick));
        save();
    }

    public long getMorningKissLastAutoAttemptGameTime(UUID maidUuid) {
        return root.getLong("MorningKissLastAutoAttemptGameTime_" + maidUuid);
    }

    public void setMorningKissLastAutoAttemptGameTime(UUID maidUuid, long tick) {
        root.putLong("MorningKissLastAutoAttemptGameTime_" + maidUuid, Math.max(0L, tick));
        save();
    }

    public void clearMorningKissSchedule(UUID maidUuid) {
        root.putString("MorningKissScheduledWindow_" + maidUuid, "");
        root.putLong("MorningKissScheduledAttemptTick_" + maidUuid, 0L);
        save();
    }

    public String getMorningKissSelectedWindowId() {
        return root.getString("MorningKissSelectedWindowId");
    }

    public void setMorningKissSelectedWindowId(String windowId) {
        root.putString("MorningKissSelectedWindowId", windowId == null ? "" : windowId);
        save();
    }

    public String getMorningKissSelectedMaidId() {
        return root.getString("MorningKissSelectedMaidId");
    }

    public void setMorningKissSelectedMaidId(String maidId) {
        root.putString("MorningKissSelectedMaidId", maidId == null ? "" : maidId);
        save();
    }

    public void clearMorningKissSelectedMaid() {
        root.putString("MorningKissSelectedWindowId", "");
        root.putString("MorningKissSelectedMaidId", "");
        save();
    }

    public MorningKissVoiceSettings getMorningKissVoiceSettings(UUID maidUuid) {
        return MorningKissVoiceSettings.of(
                root.getString("MorningKissVoiceMode_" + maidUuid),
                root.getString("MorningKissVoiceGroup_" + maidUuid),
                root.getString("MorningKissVoiceClip_" + maidUuid),
                root.getString("MorningKissVoicePack_" + maidUuid),
                VoicePoolIds.decode(root.getString("MorningKissVoicePool_" + maidUuid))
        );
    }

    public void setMorningKissVoiceSettings(UUID maidUuid, MorningKissVoiceSettings settings) {
        MorningKissVoiceSettings safe = settings == null ? MorningKissVoiceSettings.DEFAULT : settings;
        root.putString("MorningKissVoiceMode_" + maidUuid, safe.mode().serializedName());
        root.putString("MorningKissVoiceGroup_" + maidUuid, safe.selectedGroup());
        root.putString("MorningKissVoiceClip_" + maidUuid, safe.selectedClip());
        root.putString("MorningKissVoicePack_" + maidUuid, safe.soundPackId());
        root.putString("MorningKissVoicePool_" + maidUuid, VoicePoolIds.encode(safe.selectedVoiceIds()));
        save();
    }

    public EmergencyRescueVoiceSettings getEmergencyRescueVoiceSettings(UUID maidUuid) {
        return EmergencyRescueVoiceSettings.of(
                root.getString("EmergencyRescueVoiceSourceMode_" + maidUuid),
                root.getString("EmergencyRescueVoiceTlmMode_" + maidUuid),
                root.getString("EmergencyRescueVoiceTlmGroup_" + maidUuid),
                root.getString("EmergencyRescueVoiceTlmClip_" + maidUuid),
                root.getString("EmergencyRescueVoiceCustomMode_" + maidUuid),
                root.getString("EmergencyRescueVoiceFixedFile_" + maidUuid),
                root.contains("EmergencyRescueVoiceCommonFallback_" + maidUuid)
                        ? root.getBoolean("EmergencyRescueVoiceCommonFallback_" + maidUuid)
                        : com.github.touhoumaidaffection.ModConfig.BOND_EMERGENCY_RESCUE_COMMON_FALLBACK_DEFAULT.get(),
                VoicePoolIds.decode(root.getString("EmergencyRescueVoicePool_" + maidUuid))
        );
    }

    public void setEmergencyRescueVoiceSettings(UUID maidUuid, EmergencyRescueVoiceSettings settings) {
        EmergencyRescueVoiceSettings safe = settings == null ? EmergencyRescueVoiceSettings.DEFAULT : settings;
        root.putString("EmergencyRescueVoiceSourceMode_" + maidUuid, safe.sourceMode().serializedName());
        root.putString("EmergencyRescueVoiceTlmMode_" + maidUuid, safe.tlmPlayMode().serializedName());
        root.putString("EmergencyRescueVoiceTlmGroup_" + maidUuid, safe.tlmSelectedGroup());
        root.putString("EmergencyRescueVoiceTlmClip_" + maidUuid, safe.tlmSelectedClip());
        root.putString("EmergencyRescueVoiceCustomMode_" + maidUuid, safe.customPlayMode().serializedName());
        root.putString("EmergencyRescueVoiceFixedFile_" + maidUuid, safe.fixedFile());
        root.putBoolean("EmergencyRescueVoiceCommonFallback_" + maidUuid, safe.useCommonFallback());
        root.putString("EmergencyRescueVoicePool_" + maidUuid, VoicePoolIds.encode(safe.selectedVoiceIds()));
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

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalPersistent = event.getOriginal().getPersistentData();
        if (!originalPersistent.contains(ROOT_KEY)) {
            return;
        }
        event.getEntity().getPersistentData().put(ROOT_KEY, originalPersistent.getCompound(ROOT_KEY).copy());
    }

    public record MaidProfileSnapshot(
            String modelId,
            String displayName,
            String soundPackId,
            String ysmModelId,
            String ysmTexture,
            String ysmDisplayName,
            String rescueActionId,
            EmergencyRescueVoiceSettings rescueVoiceSettings
    ) {
        private static MaidProfileSnapshot empty() {
            return new MaidProfileSnapshot("", "", "", "", "", "", "", EmergencyRescueVoiceSettings.DEFAULT);
        }
    }
}
