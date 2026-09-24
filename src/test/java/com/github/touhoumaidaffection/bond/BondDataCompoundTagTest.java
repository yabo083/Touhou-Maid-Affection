package com.github.touhoumaidaffection.bond;

import com.github.touhoumaidaffection.bond.lap.LapPillowMode;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基于真实 {@link CompoundTag} 的迁移适配层与 {@link BondData} 存储 API 测试。
 *
 * <p>{@link BondDataMigrationTest} 用内存 Map 验证迁移算法；这里验证 {@code BondData} 里把
 * {@code Sink} 适配到 NBT 的那一层（{@link BondData#migrateRoot(CompoundTag)}）以及存储读写路径
 * （{@link BondData#forTest(CompoundTag)}），保证值与值类型逐键一致、深拷贝、幂等。
 */
class BondDataCompoundTagTest {

    private static final UUID MAID_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MAID_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID MAID_C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final String UPPER_C = MAID_C.toString().toUpperCase(Locale.ROOT);
    private static final long NOW = 1_800_000_000_000L;

    // ---------------------------------------------------------------- (a) 迁移适配层

    @Test
    void migrateRootMovesEveryFlatKeyIntoMaidSubtrees() {
        CompoundTag root = legacyRoot();
        Map<String, Tag> originals = snapshotValues(root, legacyFlatKeys());

        BondDataMigration.Result result = BondData.migrateRoot(root);

        assertEquals(16, result.migrated());
        assertEquals(5, result.retained());

        CompoundTag maids = root.getCompound(BondKeys.MAIDS);
        assertEquals(3, maids.getAllKeys().size());

        for (Map.Entry<String, Tag> entry : originals.entrySet()) {
            BondKeys.ParsedFlatKey parsed = BondKeys.parseFlatKey(entry.getKey()).orElseThrow();
            CompoundTag maid = maids.getCompound(parsed.maidUuid().toString());
            Tag migrated = maid.get(parsed.baseName());
            assertEquals(entry.getValue().getId(), migrated.getId(), "值类型不一致: " + entry.getKey());
            assertEquals(entry.getValue(), migrated, "值不一致: " + entry.getKey());
            assertNull(root.get(entry.getKey()), "扁平键未删除: " + entry.getKey());
        }

        // 大小写混合的 uuid 写法归并到规范小写键下。
        assertTrue(maids.contains(MAID_C.toString()));
        assertFalse(maids.contains(UPPER_C));
        assertEquals(7, maids.getCompound(MAID_C.toString()).getInt(BondKeys.BOND_LEVEL));
        assertEquals("魔理沙", maids.getCompound(MAID_C.toString()).getString(BondKeys.MAID_DISPLAY_NAME));
    }

    @Test
    void migrateRootKeepsPlayerLevelAndUnparseableKeysUntouched() {
        CompoundTag root = legacyRoot();

        BondData.migrateRoot(root);

        assertEquals(Tag.TAG_STRING, root.getTagType(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID));
        assertEquals("morning", root.getString(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID));
        assertEquals(Tag.TAG_STRING, root.getTagType(BondKeys.MORNING_KISS_SELECTED_MAID_ID));
        assertEquals(MAID_A.toString(), root.getString(BondKeys.MORNING_KISS_SELECTED_MAID_ID));
        assertEquals(Tag.TAG_INT, root.getTagType(BondKeys.SCHEMA_VERSION_KEY));
        assertEquals(1, root.getInt(BondKeys.SCHEMA_VERSION_KEY));
        assertEquals(Tag.TAG_STRING, root.getTagType("SomeRandomKey"));
        assertEquals("keep-me", root.getString("SomeRandomKey"));
        assertEquals(Tag.TAG_INT, root.getTagType("BondLevel_notauuid"));
        assertEquals(42, root.getInt("BondLevel_notauuid"));
    }

    @Test
    void migrateRootDeepCopiesCompoundValues() {
        CompoundTag root = new CompoundTag();
        CompoundTag abilities = new CompoundTag();
        abilities.putBoolean("lap_pillow", true);
        abilities.putBoolean("morning_kiss", false);
        root.put(BondKeys.flatKey(BondKeys.BOND_ABILITIES, MAID_A), abilities);

        BondData.migrateRoot(root);

        CompoundTag migrated = root.getCompound(BondKeys.MAIDS)
                .getCompound(MAID_A.toString())
                .getCompound(BondKeys.BOND_ABILITIES);
        assertNotSame(abilities, migrated);
        assertEquals(abilities, migrated);

        migrated.putBoolean("lap_pillow", false);
        assertTrue(abilities.getBoolean("lap_pillow"), "迁移后的写入不应影响原对象");

        abilities.putBoolean("morning_kiss", true);
        assertFalse(migrated.getBoolean("morning_kiss"), "原对象的写入不应影响迁移后的值");
    }

    @Test
    void migrateRootIsIdempotent() {
        CompoundTag root = legacyRoot();

        BondDataMigration.Result first = BondData.migrateRoot(root);
        CompoundTag afterFirst = root.copy();
        BondDataMigration.Result second = BondData.migrateRoot(root);

        assertEquals(16, first.migrated());
        assertEquals(0, second.migrated());
        assertEquals(6, second.retained());
        assertEquals(afterFirst, root);
        assertEquals(5, root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString()).getInt(BondKeys.BOND_LEVEL));
    }

    // ---------------------------------------------------------------- (b) 存储读写路径

    @Test
    void forTestReadsMigratedNestedData() {
        CompoundTag root = legacyRoot();
        BondData.migrateRoot(root);
        BondData data = BondData.forTest(root);

        assertEquals(5, data.getBondLevel(MAID_A));
        assertEquals(7, data.getBondLevel(MAID_C));
        assertEquals("tlm:reimu", data.getMaidModelId(MAID_A));
        assertEquals("魔理沙", data.getMaidDisplayName(MAID_C));
        assertEquals(1_700_000_000_000L, data.getMaidLastSeen(MAID_B));
        assertEquals(0L, data.getMaidLastSeen(MAID_A), "缺失 LastSeen 返回 0");
        assertEquals(0, data.getBondLevel(UUID.randomUUID()), "未知女仆返回默认值");
    }

    @Test
    void forTestDoesNotMigrateOnConstruction() {
        CompoundTag root = legacyRoot();

        BondData data = BondData.forTest(root);

        assertEquals(0, data.getBondLevel(MAID_A), "forTest 不应触发迁移");
        assertTrue(root.contains(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A)));
        assertFalse(root.contains(BondKeys.MAIDS));
    }

    @Test
    void setBondLevelDerivesUnlockedFlag() {
        BondData data = BondData.forTest(new CompoundTag());

        data.setBondLevel(MAID_A, 2);
        assertEquals(2, data.getBondLevel(MAID_A));
        assertFalse(data.isBondUnlocked(MAID_A));

        data.setBondLevel(MAID_A, BondConfig.DEFAULT_UNLOCK_LEVEL);
        assertEquals(BondConfig.DEFAULT_UNLOCK_LEVEL, data.getBondLevel(MAID_A));
        assertTrue(data.isBondUnlocked(MAID_A));

        data.setBondLevel(MAID_A, -5);
        assertEquals(0, data.getBondLevel(MAID_A), "负数等级归零");
        assertFalse(data.isBondUnlocked(MAID_A));
    }

    @Test
    void modelAndDisplayNameRoundTrip() {
        BondData data = BondData.forTest(new CompoundTag());

        data.setMaidModelId(MAID_A, "tlm:marisa");
        data.setMaidDisplayName(MAID_A, "雾雨魔理沙");

        assertEquals("tlm:marisa", data.getMaidModelId(MAID_A));
        assertEquals("雾雨魔理沙", data.getMaidDisplayName(MAID_A));

        data.setMaidModelId(MAID_A, "  ");
        assertEquals("tlm:marisa", data.getMaidModelId(MAID_A), "空白模型名被忽略");
    }

    @Test
    void lapPillowReadsLegacyOffsetKeys() {
        CompoundTag root = legacyRoot();
        BondData.migrateRoot(root);
        BondData data = BondData.forTest(root);

        LapPillowPoseSnapshot pose = data.getMaidLapPillowPose(MAID_A);
        assertEquals(LapPillowMode.MAID_SIT_PLAYER_LIE, pose.mode());
        assertEquals(0.75D, pose.playerOffsetX(), 1.0E-9);
        assertEquals(0.25D, pose.playerOffsetY(), 1.0E-9);
        assertEquals(-0.5D, pose.playerOffsetZ(), 1.0E-9);
        assertEquals(0.0D, pose.maidOffsetX(), 1.0E-9);

        // 未记录姿态的女仆回退到默认姿态。
        assertEquals(LapPillowPoseSnapshot.maidSitPlayerLieDefault(), data.getMaidLapPillowPose(MAID_B));

        // 新键存在时优先于旧键。
        CompoundTag maidA = root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString());
        maidA.putDouble(BondKeys.LAP_PILLOW_PLAYER_OFFSET_X, -1.25D);
        assertEquals(-1.25D, BondData.forTest(root).getMaidLapPillowPose(MAID_A).playerOffsetX(), 1.0E-9);
    }

    @Test
    void lastSeenRoundTripAndClamp() {
        BondData data = BondData.forTest(new CompoundTag());

        assertEquals(0L, data.getMaidLastSeen(MAID_A));
        data.setMaidLastSeen(MAID_A, NOW);
        assertEquals(NOW, data.getMaidLastSeen(MAID_A));

        data.setMaidLastSeen(MAID_A, -1L);
        assertEquals(0L, data.getMaidLastSeen(MAID_A), "负时间戳归零");
    }

    @Test
    void abilityReadWriteUsesNestedAbilityCompound() {
        CompoundTag root = legacyRoot();
        BondData.migrateRoot(root);
        BondData data = BondData.forTest(root);

        assertTrue(data.isAbilityUnlocked(MAID_A, "lap_pillow"));
        assertFalse(data.isAbilityUnlocked(MAID_A, "morning_kiss"));
        List<String> unlocked = data.getUnlockedAbilityIds(MAID_A);
        assertTrue(unlocked.contains("lap_pillow"));
        assertFalse(unlocked.contains("morning_kiss"));

        data.unlockAbility(MAID_A, "random_gift");
        assertTrue(data.isAbilityUnlocked(MAID_A, "random_gift"));
        assertTrue(data.getUnlockedAbilityIds(MAID_A).contains("random_gift"));

        // 迁移后的女仆子树里能力数据仍是 compound。
        CompoundTag abilities = root.getCompound(BondKeys.MAIDS)
                .getCompound(MAID_A.toString())
                .getCompound(BondKeys.BOND_ABILITIES);
        assertEquals(Tag.TAG_COMPOUND, abilities.getId());
        assertTrue(abilities.getBoolean("random_gift"));
    }

    @Test
    void abilityMigrationMaterializesCompoundForFreshMaid() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);

        data.setBondLevel(MAID_A, BondConfig.DEFAULT_UNLOCK_LEVEL);

        CompoundTag tag = root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString());
        assertTrue(tag.contains(BondKeys.BOND_ABILITIES, Tag.TAG_COMPOUND), "解锁等级应物化能力 compound");
        assertEquals(2, tag.getInt(BondKeys.BOND_ABILITY_VERSION));
        assertFalse(data.isAbilityUnlocked(MAID_A, "lap_pillow"));
        assertTrue(data.getUnlockedAbilityIds(MAID_A).isEmpty());
    }

    // ---------------------------------------------------------------- (c) .maid 对外格式

    @Test
    void exportMaidDataKeepsLegacyShapeAndDoesNotMutateSelf() {
        CompoundTag root = legacyRoot();
        BondData.migrateRoot(root);
        BondData data = BondData.forTest(root);
        CompoundTag before = root.copy();

        CompoundTag exported = data.exportMaidData(MAID_A);

        assertEquals(
                Set.of(
                        BondKeys.BOND_LEVEL,
                        BondKeys.BOND_UNLOCKED,
                        BondKeys.BOND_ABILITIES,
                        BondKeys.BOND_ABILITY_VERSION,
                        BondKeys.MAID_MODEL,
                        BondKeys.LAP_PILLOW_MODE,
                        BondKeys.LAP_PILLOW_LEGACY_OFFSET_X,
                        BondKeys.LAP_PILLOW_LEGACY_OFFSET_Y,
                        BondKeys.LAP_PILLOW_LEGACY_OFFSET_Z
                ),
                exported.getAllKeys(),
                ".maid 形状必须是 base 名 -> 值的 compound"
        );
        assertEquals(Tag.TAG_INT, exported.getTagType(BondKeys.BOND_LEVEL));
        assertEquals(5, exported.getInt(BondKeys.BOND_LEVEL));
        assertEquals(Tag.TAG_BYTE, exported.getTagType(BondKeys.BOND_UNLOCKED));
        assertEquals(Tag.TAG_STRING, exported.getTagType(BondKeys.MAID_MODEL));
        assertEquals("tlm:reimu", exported.getString(BondKeys.MAID_MODEL));
        assertEquals(Tag.TAG_DOUBLE, exported.getTagType(BondKeys.LAP_PILLOW_LEGACY_OFFSET_X));
        assertEquals(0.75D, exported.getDouble(BondKeys.LAP_PILLOW_LEGACY_OFFSET_X), 1.0E-9);
        assertTrue(exported.getCompound(BondKeys.BOND_ABILITIES).getBoolean("lap_pillow"));
        assertEquals(Tag.TAG_COMPOUND, exported.getTagType(BondKeys.BOND_ABILITIES));

        assertEquals(before, root, "导出不应修改自身");
        assertEquals(exported, data.exportMaidData(MAID_A), "重复导出结果一致");

        // 导出的是副本。
        exported.putInt(BondKeys.BOND_LEVEL, 99);
        assertEquals(5, data.getBondLevel(MAID_A));
    }

    @Test
    void exportMaidDataIsEmptyForUnknownMaid() {
        BondData data = BondData.forTest(new CompoundTag());

        assertTrue(data.exportMaidData(UUID.randomUUID()).isEmpty());
        assertTrue(data.exportMaidData(null).isEmpty());
    }

    @Test
    void importMaidDataReplacesSubtreeAndRefreshesLastSeen() {
        CompoundTag root = legacyRoot();
        BondData.migrateRoot(root);
        BondData data = BondData.forTest(root);
        CompoundTag exported = data.exportMaidData(MAID_A);
        UUID newMaid = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        long before = System.currentTimeMillis();

        data.importMaidData(newMaid, exported);

        long after = System.currentTimeMillis();
        CompoundTag imported = root.getCompound(BondKeys.MAIDS).getCompound(newMaid.toString());
        for (String key : exported.getAllKeys()) {
            assertEquals(exported.getTagType(key), imported.getTagType(key), "导入键类型不一致: " + key);
            assertEquals(exported.get(key), imported.get(key), "导入键值不一致: " + key);
        }
        long lastSeen = imported.getLong(BondKeys.LAST_SEEN_KEY);
        assertTrue(lastSeen >= before && lastSeen <= after, "LastSeen 应刷新为当前时间: " + lastSeen);

        // 导入的是副本：之后修改传入 tag 不影响已存数据。
        exported.putInt(BondKeys.BOND_LEVEL, 99);
        assertEquals(5, data.getBondLevel(newMaid));

        // 空 tag 导入被忽略。
        UUID ignored = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        data.importMaidData(ignored, new CompoundTag());
        assertFalse(root.getCompound(BondKeys.MAIDS).contains(ignored.toString()), "空导入不应产生空子树");
        assertEquals(4, root.getCompound(BondKeys.MAIDS).getAllKeys().size());
    }

    // ---------------------------------------------------------------- (d) prune

    @Test
    void pruneStaleMaidsRemovesStaleAndMissingLastSeen() {
        CompoundTag root = new CompoundTag();
        CompoundTag maids = new CompoundTag();
        maids.put(MAID_A.toString(), subtreeWithLastSeen(NOW));
        maids.put(MAID_B.toString(), subtreeWithLastSeen(NOW - 200L * BondRetention.MILLIS_PER_DAY));
        maids.put(MAID_C.toString(), new CompoundTag());
        root.put(BondKeys.MAIDS, maids);

        BondData.PruneResult result = BondData.forTest(root).pruneStaleMaids(NOW, 90);

        assertEquals(2, result.removed());
        assertEquals(1, result.retained());
        CompoundTag remaining = root.getCompound(BondKeys.MAIDS);
        assertEquals(Set.of(MAID_A.toString()), remaining.getAllKeys());
        assertEquals(NOW, remaining.getCompound(MAID_A.toString()).getLong(BondKeys.LAST_SEEN_KEY));
    }

    @Test
    void pruneStaleMaidsWithNonPositiveRetentionOnlyCounts() {
        CompoundTag root = new CompoundTag();
        CompoundTag maids = new CompoundTag();
        maids.put(MAID_A.toString(), subtreeWithLastSeen(NOW));
        maids.put(MAID_B.toString(), subtreeWithLastSeen(NOW - 200L * BondRetention.MILLIS_PER_DAY));
        maids.put(MAID_C.toString(), new CompoundTag());
        root.put(BondKeys.MAIDS, maids);
        CompoundTag before = root.copy();

        BondData.PruneResult result = BondData.forTest(root).pruneStaleMaids(NOW, 0);

        assertEquals(0, result.removed());
        assertEquals(3, result.retained());
        assertEquals(before, root, "retentionDays <= 0 时不得删除任何数据");
    }

    // ---------------------------------------------------------------- 辅助

    private static CompoundTag subtreeWithLastSeen(long lastSeen) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(BondKeys.LAST_SEEN_KEY, lastSeen);
        return tag;
    }

    private static Map<String, Tag> snapshotValues(CompoundTag root, List<String> keys) {
        Map<String, Tag> values = new LinkedHashMap<>();
        for (String key : keys) {
            values.put(key, root.get(key));
        }
        return values;
    }

    private static List<String> legacyFlatKeys() {
        return List.of(
                BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A),
                BondKeys.flatKey(BondKeys.MAID_MODEL, MAID_A),
                BondKeys.flatKey(BondKeys.BOND_UNLOCKED, MAID_A),
                BondKeys.flatKey(BondKeys.BOND_ABILITIES, MAID_A),
                BondKeys.flatKey(BondKeys.BOND_ABILITY_VERSION, MAID_A),
                BondKeys.flatKey(BondKeys.LAP_PILLOW_MODE, MAID_A),
                BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_X, MAID_A),
                BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_Y, MAID_A),
                BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_Z, MAID_A),
                BondKeys.flatKey(BondKeys.LAST_SEEN_KEY, MAID_B),
                BondKeys.flatKey(BondKeys.MORNING_KISS_SCHEDULED_WINDOW, MAID_B),
                BondKeys.flatKey(BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK, MAID_B),
                BondKeys.flatKey(BondKeys.EMERGENCY_RESCUE_VOICE_COMMON_FALLBACK, MAID_B),
                BondKeys.flatKey(BondKeys.EMERGENCY_RESCUE_VOICE_FIXED_FILE, MAID_B),
                BondKeys.BOND_LEVEL + "_" + UPPER_C,
                BondKeys.MAID_DISPLAY_NAME + "_" + UPPER_C
        );
    }

    /** 逼真的旧扁平根：3 个女仆（含大写 uuid 写法）× 多类键 + 玩家粒度键 + 无法解析的键。 */
    private static CompoundTag legacyRoot() {
        CompoundTag root = new CompoundTag();

        root.putInt(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);
        root.putString(BondKeys.flatKey(BondKeys.MAID_MODEL, MAID_A), "tlm:reimu");
        root.putBoolean(BondKeys.flatKey(BondKeys.BOND_UNLOCKED, MAID_A), true);
        CompoundTag abilities = new CompoundTag();
        abilities.putBoolean("lap_pillow", true);
        abilities.putBoolean("morning_kiss", false);
        root.put(BondKeys.flatKey(BondKeys.BOND_ABILITIES, MAID_A), abilities);
        root.putInt(BondKeys.flatKey(BondKeys.BOND_ABILITY_VERSION, MAID_A), 2);
        root.putString(BondKeys.flatKey(BondKeys.LAP_PILLOW_MODE, MAID_A), "maid_sit_player_lie");
        root.putDouble(BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_X, MAID_A), 0.75D);
        root.putDouble(BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_Y, MAID_A), 0.25D);
        root.putDouble(BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_Z, MAID_A), -0.5D);

        root.putLong(BondKeys.flatKey(BondKeys.LAST_SEEN_KEY, MAID_B), 1_700_000_000_000L);
        root.putString(BondKeys.flatKey(BondKeys.MORNING_KISS_SCHEDULED_WINDOW, MAID_B), "morning");
        root.putLong(BondKeys.flatKey(BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK, MAID_B), 1234L);
        root.putBoolean(BondKeys.flatKey(BondKeys.EMERGENCY_RESCUE_VOICE_COMMON_FALLBACK, MAID_B), true);
        root.putString(BondKeys.flatKey(BondKeys.EMERGENCY_RESCUE_VOICE_FIXED_FILE, MAID_B), "tlm:rescue.ogg");

        root.putInt(BondKeys.BOND_LEVEL + "_" + UPPER_C, 7);
        root.putString(BondKeys.MAID_DISPLAY_NAME + "_" + UPPER_C, "魔理沙");

        root.putString(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID, "morning");
        root.putString(BondKeys.MORNING_KISS_SELECTED_MAID_ID, MAID_A.toString());
        root.putInt(BondKeys.SCHEMA_VERSION_KEY, 1);

        root.putString("SomeRandomKey", "keep-me");
        root.putInt("BondLevel_notauuid", 42);

        return root;
    }
}