package com.github.touhoumaidaffection.bond;

import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.lap.LapPillowMode;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 主人玩家的羁绊数据。
 *
 * <p>数据挂在玩家 persistentData 的 {@link BondKeys#ROOT} 下，女仆粒度数据嵌套在
 * {@code maids.<女仆UUID>.<base>} 子 compound 中，玩家粒度数据（如早安吻选择）留在根上。
 * 旧存档的扁平键（{@code <base>_<女仆UUID>}）由 {@link BondDataMigration} 在首次读取时
 * 一次性迁移（见 {@link BondKeys#SCHEMA_VERSION_KEY}）。
 */
@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public class BondData {
    private static final int CURRENT_ABILITY_DATA_VERSION = 2;

    private final CompoundTag persistent;
    private final CompoundTag root;

    private BondData(CompoundTag persistent, CompoundTag root) {
        this.persistent = persistent;
        this.root = root;
    }

    public static BondData of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(BondKeys.ROOT)) {
            persistent.put(BondKeys.ROOT, new CompoundTag());
        }
        BondData data = new BondData(persistent, persistent.getCompound(BondKeys.ROOT));
        data.migrateIfNeeded();
        return data;
    }

    /**
     * 首次读取时把旧扁平布局迁移到嵌套布局；已迁移（{@code SchemaVersion >= CURRENT_SCHEMA}）直接跳过。
     */
    private void migrateIfNeeded() {
        if (root.getInt(BondKeys.SCHEMA_VERSION_KEY) >= BondKeys.CURRENT_SCHEMA) {
            return;
        }
        BondDataMigration.migrate(new CompoundSink());
        root.putInt(BondKeys.SCHEMA_VERSION_KEY, BondKeys.CURRENT_SCHEMA);
        save();
    }

    /** 把 {@link BondDataMigration.Sink} 适配到根 compound。 */
    private final class CompoundSink implements BondDataMigration.Sink<Tag> {
        @Override
        public Set<String> rootKeys() {
            return root.getAllKeys();
        }

        @Override
        public Tag value(String key) {
            return root.get(key);
        }

        @Override
        public void writeMaidValue(UUID maidUuid, String baseName, Tag value) {
            maidTag(maidUuid, true).put(baseName, value.copy());
        }

        @Override
        public void removeRootKey(String key) {
            root.remove(key);
        }
    }

    /** 取 {@code maids} 子 compound；{@code create} 为 false 且缺失时返回游离的空 compound。 */
    private CompoundTag maidsRoot(boolean create) {
        if (root.contains(BondKeys.MAIDS, Tag.TAG_COMPOUND)) {
            return root.getCompound(BondKeys.MAIDS);
        }
        if (!create) {
            return new CompoundTag();
        }
        CompoundTag maids = new CompoundTag();
        root.put(BondKeys.MAIDS, maids);
        return maids;
    }

    /** 取某女仆的子树；{@code create} 为 false 且缺失时返回游离的空 compound。 */
    private CompoundTag maidTag(UUID maidUuid, boolean create) {
        if (maidUuid == null) {
            return new CompoundTag();
        }
        CompoundTag maids = maidsRoot(create);
        String key = maidUuid.toString();
        if (maids.contains(key, Tag.TAG_COMPOUND)) {
            return maids.getCompound(key);
        }
        if (!create) {
            return new CompoundTag();
        }
        CompoundTag tag = new CompoundTag();
        maids.put(key, tag);
        return tag;
    }

    /** 把 {@code maids} 下的键解析为女仆 UUID，非法键返回 {@code null}。 */
    private static UUID parseMaidKey(String key) {
        if (key == null) {
            return null;
        }
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public int getBondLevel(UUID maidUuid) {
        return maidTag(maidUuid, false).getInt(BondKeys.BOND_LEVEL);
    }

    public void setBondLevel(UUID maidUuid, int level) {
        int normalized = Math.max(0, level);
        CompoundTag tag = maidTag(maidUuid, true);
        tag.putInt(BondKeys.BOND_LEVEL, normalized);
        boolean unlocked = normalized >= BondConfig.DEFAULT_UNLOCK_LEVEL;
        tag.putBoolean(BondKeys.BOND_UNLOCKED, unlocked);
        if (unlocked) {
            migrateAbilityDataIfNeeded(maidUuid);
        }
        save();
    }

    public boolean isBondUnlocked(UUID maidUuid) {
        return maidTag(maidUuid, false).getBoolean(BondKeys.BOND_UNLOCKED);
    }

    public boolean isAbilityUnlocked(UUID maidUuid, String abilityId) {
        migrateAbilityDataIfNeeded(maidUuid);
        CompoundTag abilities = maidTag(maidUuid, false).getCompound(BondKeys.BOND_ABILITIES);
        return abilities.getBoolean(abilityId);
    }

    public void unlockAbility(UUID maidUuid, String abilityId) {
        migrateAbilityDataIfNeeded(maidUuid);
        CompoundTag tag = maidTag(maidUuid, true);
        CompoundTag abilities = tag.getCompound(BondKeys.BOND_ABILITIES);
        abilities.putBoolean(abilityId, true);
        tag.put(BondKeys.BOND_ABILITIES, abilities);
        save();
    }

    public List<String> getUnlockedAbilityIds(UUID maidUuid) {
        migrateAbilityDataIfNeeded(maidUuid);
        CompoundTag abilities = maidTag(maidUuid, false).getCompound(BondKeys.BOND_ABILITIES);
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
        maidTag(maidUuid, true).putString(BondKeys.MAID_MODEL, modelId);
        save();
    }

    public void setMaidDisplayName(UUID maidUuid, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        maidTag(maidUuid, true).putString(BondKeys.MAID_DISPLAY_NAME, displayName);
        save();
    }

    public String getMaidDisplayName(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_DISPLAY_NAME);
    }

    public void setMaidSoundPackId(UUID maidUuid, String soundPackId) {
        maidTag(maidUuid, true).putString(BondKeys.MAID_SOUND_PACK, soundPackId == null ? "" : soundPackId);
        save();
    }

    public String getMaidSoundPackId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_SOUND_PACK);
    }

    public void setMaidYsmProfile(UUID maidUuid, String ysmModelId, String ysmTexture, String ysmDisplayName) {
        CompoundTag tag = maidTag(maidUuid, true);
        tag.putString(BondKeys.MAID_YSM_MODEL_ID, ysmModelId == null ? "" : ysmModelId);
        tag.putString(BondKeys.MAID_YSM_TEXTURE, ysmTexture == null ? "" : ysmTexture);
        tag.putString(BondKeys.MAID_YSM_DISPLAY_NAME, ysmDisplayName == null ? "" : ysmDisplayName);
        save();
    }

    public void setMaidRescueAction(UUID maidUuid, String actionId) {
        maidTag(maidUuid, true).putString(BondKeys.MAID_RESCUE_ACTION, BondDataLimits.normalize(actionId));
        save();
    }

    public String getMaidRescueAction(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_RESCUE_ACTION);
    }

    public void setMaidRescueProviderId(UUID maidUuid, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return;
        }
        CompoundTag tag = maidTag(maidUuid, true);
        if (providerId.equals(tag.getString(BondKeys.MAID_RESCUE_PROVIDER))) {
            return;
        }
        tag.putString(BondKeys.MAID_RESCUE_PROVIDER, providerId);
        save();
    }

    public String getMaidRescueProviderId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_RESCUE_PROVIDER);
    }

    public LapPillowPoseSnapshot getMaidLapPillowPose(UUID maidUuid) {
        CompoundTag tag = maidTag(maidUuid, false);
        String mode = tag.getString(BondKeys.LAP_PILLOW_MODE);
        if (mode.isBlank()) {
            return LapPillowPoseSnapshot.maidSitPlayerLieDefault();
        }
        return new LapPillowPoseSnapshot(
                LapPillowMode.fromName(mode),
                tag.getDouble(BondKeys.LAP_PILLOW_MAID_OFFSET_X),
                tag.getDouble(BondKeys.LAP_PILLOW_MAID_OFFSET_Y),
                tag.getDouble(BondKeys.LAP_PILLOW_MAID_OFFSET_Z),
                readPlayerOffset(tag, BondKeys.LAP_PILLOW_PLAYER_OFFSET_X, BondKeys.LAP_PILLOW_LEGACY_OFFSET_X),
                readPlayerOffset(tag, BondKeys.LAP_PILLOW_PLAYER_OFFSET_Y, BondKeys.LAP_PILLOW_LEGACY_OFFSET_Y),
                readPlayerOffset(tag, BondKeys.LAP_PILLOW_PLAYER_OFFSET_Z, BondKeys.LAP_PILLOW_LEGACY_OFFSET_Z),
                tag.getString(BondKeys.LAP_PILLOW_MAID_ACTION),
                tag.getString(BondKeys.LAP_PILLOW_PLAYER_ACTION)
        ).clamp();
    }

    public void setMaidLapPillowPose(UUID maidUuid, LapPillowPoseSnapshot pose) {
        LapPillowPoseSnapshot safe = pose == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : pose.clamp();
        CompoundTag tag = maidTag(maidUuid, true);
        tag.putString(BondKeys.LAP_PILLOW_MODE, safe.mode().serializedName());
        tag.putDouble(BondKeys.LAP_PILLOW_MAID_OFFSET_X, safe.maidOffsetX());
        tag.putDouble(BondKeys.LAP_PILLOW_MAID_OFFSET_Y, safe.maidOffsetY());
        tag.putDouble(BondKeys.LAP_PILLOW_MAID_OFFSET_Z, safe.maidOffsetZ());
        tag.putDouble(BondKeys.LAP_PILLOW_PLAYER_OFFSET_X, safe.playerOffsetX());
        tag.putDouble(BondKeys.LAP_PILLOW_PLAYER_OFFSET_Y, safe.playerOffsetY());
        tag.putDouble(BondKeys.LAP_PILLOW_PLAYER_OFFSET_Z, safe.playerOffsetZ());
        tag.putString(BondKeys.LAP_PILLOW_MAID_ACTION, safe.maidActionId());
        tag.putString(BondKeys.LAP_PILLOW_PLAYER_ACTION, safe.playerActionId());
        save();
    }

    private double readPlayerOffset(CompoundTag tag, String currentKey, String legacyKey) {
        if (tag.contains(currentKey)) {
            return tag.getDouble(currentKey);
        }
        return tag.getDouble(legacyKey);
    }

    public String getMaidYsmModelId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_YSM_MODEL_ID);
    }

    public String getMaidYsmTexture(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_YSM_TEXTURE);
    }

    public String getMaidYsmDisplayName(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_YSM_DISPLAY_NAME);
    }

    public String getMaidModelId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MAID_MODEL);
    }

    public List<UUID> getUnlockedMaidIdsForAbility(String abilityId) {
        List<UUID> result = new ArrayList<>();
        CompoundTag maids = maidsRoot(false);
        for (String key : new ArrayList<>(maids.getAllKeys())) {
            UUID maidUuid = parseMaidKey(key);
            if (maidUuid == null || !maids.getCompound(key).getBoolean(BondKeys.BOND_UNLOCKED)) {
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
        CompoundTag maids = maidsRoot(false);
        for (String key : maids.getAllKeys()) {
            if (!modelId.equals(maids.getCompound(key).getString(BondKeys.MAID_MODEL))) {
                continue;
            }
            UUID maidUuid = parseMaidKey(key);
            if (maidUuid != null) {
                return maidUuid;
            }
        }
        return null;
    }

    public UUID findMaidUuidByRescueProviderId(String providerId) {
        return findMaidUuidByRescueProviderId(providerId, "");
    }

    public UUID findMaidUuidByRescueProviderId(String providerId, String preferredAbilityId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        CompoundTag maids = maidsRoot(false);
        List<UUID> matches = new ArrayList<>();
        for (String key : maids.getAllKeys()) {
            if (!providerId.equals(maids.getCompound(key).getString(BondKeys.MAID_RESCUE_PROVIDER))) {
                continue;
            }
            UUID maidUuid = parseMaidKey(key);
            if (maidUuid != null) {
                matches.add(maidUuid);
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
        return matches.get(0);
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
            CompoundTag tag = maidTag(maidUuid, true);
            CompoundTag abilities = tag.getCompound(BondKeys.BOND_ABILITIES);
            boolean wasUnlocked = abilities.getBoolean(abilityId);
            if (wasUnlocked) {
                resetCount++;
            }
            if (wasUnlocked || !abilities.contains(abilityId)) {
                abilities.putBoolean(abilityId, false);
                tag.put(BondKeys.BOND_ABILITIES, abilities);
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
        CompoundTag maids = maidsRoot(false);
        for (String key : maids.getAllKeys()) {
            CompoundTag tag = maids.getCompound(key);
            if (!tag.contains(BondKeys.BOND_UNLOCKED) && !tag.contains(BondKeys.BOND_ABILITIES)) {
                continue;
            }
            UUID maidUuid = parseMaidKey(key);
            if (maidUuid != null) {
                maidIds.add(maidUuid);
            }
        }
        return maidIds;
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

    /** 该女仆最后一次与主人同步档案的时间（epoch millis）；缺失时返回 0。 */
    public long getMaidLastSeen(UUID maidUuid) {
        return maidTag(maidUuid, false).getLong(BondKeys.LAST_SEEN_KEY);
    }

    /** 刷新该女仆的最后在线时间。 */
    public void setMaidLastSeen(UUID maidUuid, long epochMs) {
        if (maidUuid == null) {
            return;
        }
        long normalized = Math.max(0L, epochMs);
        CompoundTag tag = maidTag(maidUuid, true);
        if (tag.getLong(BondKeys.LAST_SEEN_KEY) == normalized) {
            return;
        }
        tag.putLong(BondKeys.LAST_SEEN_KEY, normalized);
        save();
    }

    /**
     * 清理过旧的女仆子树：{@code LastSeen} 早于 {@code now - retentionDays} 天，
     * 或缺失 {@code LastSeen} 的子树（视为历史遗留）。
     *
     * <p>{@code retentionDays <= 0} 时不删除任何数据。不会因女仆死亡/卸载/换主人而自动清理，
     * 只由该显式入口触发。
     *
     * @return 删除数量与保留数量
     */
    public PruneResult pruneStaleMaids(long nowEpochMs, long retentionDays) {
        CompoundTag maids = maidsRoot(false);
        List<String> keys = new ArrayList<>(maids.getAllKeys());
        int removed = 0;
        for (String key : keys) {
            long lastSeen = maids.getCompound(key).getLong(BondKeys.LAST_SEEN_KEY);
            if (!BondRetention.isStale(lastSeen, nowEpochMs, retentionDays)) {
                continue;
            }
            maids.remove(key);
            removed++;
        }
        if (removed > 0) {
            save();
        }
        return new PruneResult(removed, keys.size() - removed);
    }

    public int getQueuedGiftCount(UUID maidUuid) {
        return Math.max(0, maidTag(maidUuid, false).getInt(BondKeys.RANDOM_GIFT_QUEUE));
    }

    public void setQueuedGiftCount(UUID maidUuid, int count) {
        maidTag(maidUuid, true).putInt(BondKeys.RANDOM_GIFT_QUEUE, Math.max(0, count));
        save();
    }

    public long getLastGiftWallClockMs(UUID maidUuid) {
        return maidTag(maidUuid, false).getLong(BondKeys.RANDOM_GIFT_LAST_WALL_CLOCK);
    }

    public void setLastGiftWallClockMs(UUID maidUuid, long timestampMs) {
        maidTag(maidUuid, true).putLong(BondKeys.RANDOM_GIFT_LAST_WALL_CLOCK, Math.max(0L, timestampMs));
        save();
    }

    public long getLastGiftDeliveryGameTime(UUID maidUuid) {
        return maidTag(maidUuid, false).getLong(BondKeys.RANDOM_GIFT_LAST_DELIVERY);
    }

    public void setLastGiftDeliveryGameTime(UUID maidUuid, long gameTime) {
        maidTag(maidUuid, true).putLong(BondKeys.RANDOM_GIFT_LAST_DELIVERY, Math.max(0L, gameTime));
        save();
    }

    public int getLastGiftIntervalMinutes(UUID maidUuid) {
        return Math.max(0, maidTag(maidUuid, false).getInt(BondKeys.RANDOM_GIFT_LAST_INTERVAL_MINUTES));
    }

    public void setLastGiftIntervalMinutes(UUID maidUuid, int intervalMinutes) {
        maidTag(maidUuid, true).putInt(BondKeys.RANDOM_GIFT_LAST_INTERVAL_MINUTES, Math.max(0, intervalMinutes));
        save();
    }

    public void initializeRandomGiftState(UUID maidUuid, long nowMs) {
        initializeRandomGiftState(maidUuid, nowMs, 0);
    }

    public void initializeRandomGiftState(UUID maidUuid, long nowMs, int intervalMinutes) {
        CompoundTag tag = maidTag(maidUuid, true);
        if (!tag.contains(BondKeys.RANDOM_GIFT_QUEUE)) {
            tag.putInt(BondKeys.RANDOM_GIFT_QUEUE, 0);
        }
        if (!tag.contains(BondKeys.RANDOM_GIFT_LAST_WALL_CLOCK)) {
            tag.putLong(BondKeys.RANDOM_GIFT_LAST_WALL_CLOCK, Math.max(0L, nowMs));
        }
        if (!tag.contains(BondKeys.RANDOM_GIFT_LAST_DELIVERY)) {
            tag.putLong(BondKeys.RANDOM_GIFT_LAST_DELIVERY, 0L);
        }
        if (!tag.contains(BondKeys.RANDOM_GIFT_LAST_INTERVAL_MINUTES)) {
            tag.putInt(BondKeys.RANDOM_GIFT_LAST_INTERVAL_MINUTES, Math.max(0, intervalMinutes));
        }
        save();
    }

    public String getMorningKissLastSuccessfulWindowId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MORNING_KISS_LAST_SUCCESS_WINDOW);
    }

    public void setMorningKissLastSuccessfulWindowId(UUID maidUuid, String windowId) {
        maidTag(maidUuid, true).putString(BondKeys.MORNING_KISS_LAST_SUCCESS_WINDOW, windowId == null ? "" : windowId);
        save();
    }

    public String getMorningKissLastFailedWindowId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MORNING_KISS_LAST_FAILED_WINDOW);
    }

    public void setMorningKissLastFailedWindowId(UUID maidUuid, String windowId) {
        maidTag(maidUuid, true).putString(BondKeys.MORNING_KISS_LAST_FAILED_WINDOW, windowId == null ? "" : windowId);
        save();
    }

    public String getMorningKissScheduledWindowId(UUID maidUuid) {
        return maidTag(maidUuid, false).getString(BondKeys.MORNING_KISS_SCHEDULED_WINDOW);
    }

    public void setMorningKissScheduledWindowId(UUID maidUuid, String windowId) {
        maidTag(maidUuid, true).putString(BondKeys.MORNING_KISS_SCHEDULED_WINDOW, windowId == null ? "" : windowId);
        save();
    }

    public long getMorningKissScheduledAttemptTick(UUID maidUuid) {
        return maidTag(maidUuid, false).getLong(BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK);
    }

    public void setMorningKissScheduledAttemptTick(UUID maidUuid, long tick) {
        maidTag(maidUuid, true).putLong(BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK, Math.max(0L, tick));
        save();
    }

    public long getMorningKissLastAutoAttemptGameTime(UUID maidUuid) {
        return maidTag(maidUuid, false).getLong(BondKeys.MORNING_KISS_LAST_AUTO_ATTEMPT_GAME_TIME);
    }

    public void setMorningKissLastAutoAttemptGameTime(UUID maidUuid, long tick) {
        maidTag(maidUuid, true).putLong(BondKeys.MORNING_KISS_LAST_AUTO_ATTEMPT_GAME_TIME, Math.max(0L, tick));
        save();
    }

    public void clearMorningKissSchedule(UUID maidUuid) {
        CompoundTag tag = maidTag(maidUuid, true);
        tag.putString(BondKeys.MORNING_KISS_SCHEDULED_WINDOW, "");
        tag.putLong(BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK, 0L);
        save();
    }

    public String getMorningKissSelectedWindowId() {
        return root.getString(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID);
    }

    public void setMorningKissSelectedWindowId(String windowId) {
        root.putString(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID, windowId == null ? "" : windowId);
        save();
    }

    public String getMorningKissSelectedMaidId() {
        return root.getString(BondKeys.MORNING_KISS_SELECTED_MAID_ID);
    }

    public void setMorningKissSelectedMaidId(String maidId) {
        root.putString(BondKeys.MORNING_KISS_SELECTED_MAID_ID, maidId == null ? "" : maidId);
        save();
    }

    public void clearMorningKissSelectedMaid() {
        root.putString(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID, "");
        root.putString(BondKeys.MORNING_KISS_SELECTED_MAID_ID, "");
        save();
    }

    public MorningKissVoiceSettings getMorningKissVoiceSettings(UUID maidUuid) {
        CompoundTag tag = maidTag(maidUuid, false);
        return MorningKissVoiceSettings.of(
                tag.getString(BondKeys.MORNING_KISS_VOICE_MODE),
                tag.getString(BondKeys.MORNING_KISS_VOICE_GROUP),
                tag.getString(BondKeys.MORNING_KISS_VOICE_CLIP),
                tag.getString(BondKeys.MORNING_KISS_VOICE_PACK),
                VoicePoolIds.decode(tag.getString(BondKeys.MORNING_KISS_VOICE_POOL))
        );
    }

    public void setMorningKissVoiceSettings(UUID maidUuid, MorningKissVoiceSettings settings) {
        MorningKissVoiceSettings safe = settings == null ? MorningKissVoiceSettings.DEFAULT : settings;
        if (!VoicePoolIds.isPersistableSelection(safe.selectedVoiceIds())) {
            return;
        }
        CompoundTag tag = maidTag(maidUuid, true);
        tag.putString(BondKeys.MORNING_KISS_VOICE_MODE, safe.mode().serializedName());
        tag.putString(BondKeys.MORNING_KISS_VOICE_GROUP, safe.selectedGroup());
        tag.putString(BondKeys.MORNING_KISS_VOICE_CLIP, safe.selectedClip());
        tag.putString(BondKeys.MORNING_KISS_VOICE_PACK, safe.soundPackId());
        tag.putString(BondKeys.MORNING_KISS_VOICE_POOL, VoicePoolIds.encode(safe.selectedVoiceIds()));
        save();
    }

    public EmergencyRescueVoiceSettings getEmergencyRescueVoiceSettings(UUID maidUuid) {
        CompoundTag tag = maidTag(maidUuid, false);
        return EmergencyRescueVoiceSettings.of(
                tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_SOURCE_MODE),
                tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_TLM_MODE),
                tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_TLM_GROUP),
                tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_TLM_CLIP),
                tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_CUSTOM_MODE),
                tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_FIXED_FILE),
                tag.contains(BondKeys.EMERGENCY_RESCUE_VOICE_COMMON_FALLBACK)
                        ? tag.getBoolean(BondKeys.EMERGENCY_RESCUE_VOICE_COMMON_FALLBACK)
                        : com.github.touhoumaidaffection.ModConfig.BOND_EMERGENCY_RESCUE_COMMON_FALLBACK_DEFAULT.get(),
                VoicePoolIds.decode(tag.getString(BondKeys.EMERGENCY_RESCUE_VOICE_POOL))
        );
    }

    public void setEmergencyRescueVoiceSettings(UUID maidUuid, EmergencyRescueVoiceSettings settings) {
        EmergencyRescueVoiceSettings safe = settings == null ? EmergencyRescueVoiceSettings.DEFAULT : settings;
        if (!VoicePoolIds.isPersistableSelection(safe.selectedVoiceIds())) {
            return;
        }
        CompoundTag tag = maidTag(maidUuid, true);
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_SOURCE_MODE, safe.sourceMode().serializedName());
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_TLM_MODE, safe.tlmPlayMode().serializedName());
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_TLM_GROUP, safe.tlmSelectedGroup());
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_TLM_CLIP, safe.tlmSelectedClip());
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_CUSTOM_MODE, safe.customPlayMode().serializedName());
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_FIXED_FILE, safe.fixedFile());
        tag.putBoolean(BondKeys.EMERGENCY_RESCUE_VOICE_COMMON_FALLBACK, safe.useCommonFallback());
        tag.putString(BondKeys.EMERGENCY_RESCUE_VOICE_POOL, VoicePoolIds.encode(safe.selectedVoiceIds()));
        save();
    }

    /**
     * 导出该女仆的羁绊数据：返回 {@code maids.<女仆UUID>} 子树的副本（base 名 → 值的 compound）。
     * 不修改本对象，因此 {@code .maid} 附加数据的对外格式与旧版一致。
     *
     * @return 该女仆无任何数据时返回空 tag，调用方自行判断是否导出
     */
    public CompoundTag exportMaidData(UUID maidUuid) {
        if (maidUuid == null) {
            return new CompoundTag();
        }
        return maidTag(maidUuid, false).copy();
    }

    /**
     * 导入羁绊数据：整体替换 {@code maids.<女仆UUID>} 子树（{@code base 名 → 值}），
     * 并刷新 {@code LastSeen}。空白数据直接跳过。
     */
    public void importMaidData(UUID maidUuid, CompoundTag data) {
        if (maidUuid == null || data == null || data.isEmpty()) {
            return;
        }
        maidsRoot(true).put(maidUuid.toString(), data.copy());
        maidTag(maidUuid, true).putLong(BondKeys.LAST_SEEN_KEY, System.currentTimeMillis());
        save();
    }
    private void migrateAbilityDataIfNeeded(UUID maidUuid) {
        if (maidUuid == null) {
            return;
        }
        CompoundTag tag = maidTag(maidUuid, false);
        if (tag.getInt(BondKeys.BOND_ABILITY_VERSION) >= CURRENT_ABILITY_DATA_VERSION
                && tag.contains(BondKeys.BOND_ABILITIES)) {
            return;
        }
        CompoundTag abilities = new CompoundTag();
        BondAbilityManager.registerDefaults();
        BondAbilityManager.getAllAbilities().forEach(ability -> abilities.putBoolean(ability.getId(), false));
        CompoundTag target = maidTag(maidUuid, true);
        target.put(BondKeys.BOND_ABILITIES, abilities);
        target.putInt(BondKeys.BOND_ABILITY_VERSION, CURRENT_ABILITY_DATA_VERSION);
        save();
    }

    private void save() {
        persistent.put(BondKeys.ROOT, root);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalPersistent = event.getOriginal().getPersistentData();
        if (!originalPersistent.contains(BondKeys.ROOT)) {
            return;
        }
        event.getEntity().getPersistentData().put(BondKeys.ROOT, originalPersistent.getCompound(BondKeys.ROOT).copy());
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

    /**
     * prune 结果。
     *
     * @param removed  被删除的女仆子树数量
     * @param retained 保留下来的女仆子树数量
     */
    public record PruneResult(int removed, int retained) {
    }
}