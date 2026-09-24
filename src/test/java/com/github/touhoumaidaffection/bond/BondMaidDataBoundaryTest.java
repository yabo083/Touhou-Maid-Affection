package com.github.touhoumaidaffection.bond;

import com.github.touhoumaidaffection.bond.lap.LapPillowMode;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code .maid} 迁移边界测试：画像随迁、运行态/调度键不随迁，空字符串不落盘/不导出。
 *
 * <p>运行态键是会话/世界相关的绝对时间（礼物计时、早安吻窗口标记、{@code LastSeen}），
 * 带到另一只女仆或另一个存档会污染新女仆的礼物计时与「今天是否已亲过」判定；
 * 待发礼物队列（{@link BondKeys#RANDOM_GIFT_QUEUE}）是耐久状态，必须随迁。
 */
class BondMaidDataBoundaryTest {

    private static final UUID MAID_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MAID_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // ---------------------------------------------------------------- 键集合

    @Test
    void runtimeKeysCoverSchedulingAndLastSeenButKeepGiftQueue() {
        assertEquals(
                Set.of(
                        BondKeys.RANDOM_GIFT_LAST_WALL_CLOCK,
                        BondKeys.RANDOM_GIFT_LAST_DELIVERY,
                        BondKeys.RANDOM_GIFT_LAST_INTERVAL_MINUTES,
                        BondKeys.MORNING_KISS_SCHEDULED_WINDOW,
                        BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK,
                        BondKeys.MORNING_KISS_LAST_AUTO_ATTEMPT_GAME_TIME,
                        BondKeys.MORNING_KISS_LAST_SUCCESS_WINDOW,
                        BondKeys.MORNING_KISS_LAST_FAILED_WINDOW,
                        BondKeys.LAST_SEEN_KEY
                ),
                BondKeys.RUNTIME_KEYS
        );
        assertFalse(BondKeys.RUNTIME_KEYS.contains(BondKeys.RANDOM_GIFT_QUEUE), "待发礼物队列必须随迁");
    }

    // ---------------------------------------------------------------- 导出

    @Test
    void exportStripsRuntimeKeysAndKeepsProfileKeys() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);
        populate(data, MAID_A);

        CompoundTag rawSubtree = root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString());
        for (String runtimeKey : BondKeys.RUNTIME_KEYS) {
            assertTrue(rawSubtree.contains(runtimeKey), "前置条件：存储里应有运行态键 " + runtimeKey);
        }

        CompoundTag exported = data.exportMaidData(MAID_A);

        for (String runtimeKey : BondKeys.RUNTIME_KEYS) {
            assertFalse(exported.contains(runtimeKey), "运行态键不应导出: " + runtimeKey);
        }
        assertFalse(exported.contains(BondKeys.LAST_SEEN_KEY), "LastSeen 是本地记账，不应导出");

        assertEquals(7, exported.getInt(BondKeys.BOND_LEVEL));
        assertTrue(exported.getBoolean(BondKeys.BOND_UNLOCKED));
        assertEquals("tlm:reimu", exported.getString(BondKeys.MAID_MODEL));
        assertEquals("灵梦", exported.getString(BondKeys.MAID_DISPLAY_NAME));
        assertEquals("pack_a", exported.getString(BondKeys.MAID_SOUND_PACK));
        assertEquals("ysm_model", exported.getString(BondKeys.MAID_YSM_MODEL_ID));
        assertEquals("ysm_tex", exported.getString(BondKeys.MAID_YSM_TEXTURE));
        assertEquals("YSM 名", exported.getString(BondKeys.MAID_YSM_DISPLAY_NAME));
        assertEquals("tlm:rescue", exported.getString(BondKeys.MAID_RESCUE_ACTION));
        assertEquals("provider_a", exported.getString(BondKeys.MAID_RESCUE_PROVIDER));
        assertEquals(3, exported.getInt(BondKeys.RANDOM_GIFT_QUEUE), "待发礼物队列必须随迁");
        assertEquals(LapPillowMode.MAID_SIT_PLAYER_LIE.serializedName(), exported.getString(BondKeys.LAP_PILLOW_MODE));
        assertTrue(exported.contains(BondKeys.BOND_ABILITIES));

        assertEquals(rawSubtree.copy(), root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString()),
                "导出不应修改自身");
    }

    @Test
    void exportStripsEmptyStringValues() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);
        populate(data, MAID_A);
        // 空串画像键：写入时即被剔除，但旧存档/旧文件可能带着，导出必须再剥一层。
        root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString())
                .putString(BondKeys.MAID_SOUND_PACK, "");
        root.getCompound(BondKeys.MAIDS).getCompound(MAID_A.toString())
                .putString(BondKeys.MAID_YSM_TEXTURE, "");

        CompoundTag exported = data.exportMaidData(MAID_A);

        assertFalse(exported.contains(BondKeys.MAID_SOUND_PACK), "空串值不应导出");
        assertFalse(exported.contains(BondKeys.MAID_YSM_TEXTURE), "空串值不应导出");
        assertEquals("ysm_model", exported.getString(BondKeys.MAID_YSM_MODEL_ID), "非空画像键仍应导出");
    }

    // ---------------------------------------------------------------- 导入

    @Test
    void importStripsRuntimeKeysFromLegacyExtras() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);

        CompoundTag legacyExtras = new CompoundTag();
        legacyExtras.putInt(BondKeys.BOND_LEVEL, 4);
        legacyExtras.putString(BondKeys.MAID_MODEL, "tlm:marisa");
        legacyExtras.putString(BondKeys.MAID_SOUND_PACK, "");
        legacyExtras.putLong(BondKeys.RANDOM_GIFT_LAST_WALL_CLOCK, 999L);
        legacyExtras.putLong(BondKeys.RANDOM_GIFT_LAST_DELIVERY, 888L);
        legacyExtras.putInt(BondKeys.RANDOM_GIFT_LAST_INTERVAL_MINUTES, 30);
        legacyExtras.putString(BondKeys.MORNING_KISS_SCHEDULED_WINDOW, "morning");
        legacyExtras.putLong(BondKeys.MORNING_KISS_SCHEDULED_ATTEMPT_TICK, 42L);
        legacyExtras.putLong(BondKeys.MORNING_KISS_LAST_AUTO_ATTEMPT_GAME_TIME, 43L);
        legacyExtras.putString(BondKeys.MORNING_KISS_LAST_SUCCESS_WINDOW, "morning");
        legacyExtras.putString(BondKeys.MORNING_KISS_LAST_FAILED_WINDOW, "night");
        legacyExtras.putLong(BondKeys.LAST_SEEN_KEY, 123L);
        legacyExtras.putInt(BondKeys.RANDOM_GIFT_QUEUE, 2);

        long before = System.currentTimeMillis();
        data.importMaidData(MAID_B, legacyExtras);
        long after = System.currentTimeMillis();

        CompoundTag imported = root.getCompound(BondKeys.MAIDS).getCompound(MAID_B.toString());
        for (String runtimeKey : BondKeys.RUNTIME_KEYS) {
            if (BondKeys.LAST_SEEN_KEY.equals(runtimeKey)) {
                continue; // 导入后由 importMaidData 主动刷新
            }
            assertFalse(imported.contains(runtimeKey), "旧文件里的运行态键不应落位: " + runtimeKey);
        }
        assertFalse(imported.contains(BondKeys.MAID_SOUND_PACK), "空串值不应落位");

        assertEquals(4, imported.getInt(BondKeys.BOND_LEVEL), "画像键应正确落位");
        assertEquals("tlm:marisa", imported.getString(BondKeys.MAID_MODEL));
        assertEquals(2, imported.getInt(BondKeys.RANDOM_GIFT_QUEUE), "待发礼物队列应随迁");
        assertEquals("", data.getMaidSoundPackId(MAID_B));

        long lastSeen = imported.getLong(BondKeys.LAST_SEEN_KEY);
        assertTrue(lastSeen >= before && lastSeen <= after, "LastSeen 应刷新为当前时间: " + lastSeen);

        assertEquals(0L, data.getLastGiftWallClockMs(MAID_B), "运行态读取应为缺省值");
        assertEquals("", data.getMorningKissScheduledWindowId(MAID_B), "运行态读取应为缺省值");
    }

    @Test
    void importStillReplacesWholeSubtree() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);
        data.setMaidModelId(MAID_B, "tlm:reimu");
        data.setBondLevel(MAID_B, 3);
        data.setMaidDisplayName(MAID_B, "灵梦");

        CompoundTag replacement = new CompoundTag();
        replacement.putInt(BondKeys.BOND_LEVEL, 9);
        data.importMaidData(MAID_B, replacement);

        CompoundTag imported = root.getCompound(BondKeys.MAIDS).getCompound(MAID_B.toString());
        assertFalse(imported.contains(BondKeys.MAID_MODEL), "源里没有的键必须被清掉（整体替换，非合并）");
        assertFalse(imported.contains(BondKeys.MAID_DISPLAY_NAME));
        assertEquals("", data.getMaidModelId(MAID_B));
        assertEquals("", data.getMaidDisplayName(MAID_B));
        assertEquals(9, data.getBondLevel(MAID_B));
    }

    @Test
    void exportImportRoundTripCarriesProfileFields() {
        CompoundTag root = new CompoundTag();
        BondData source = BondData.forTest(root);
        populate(source, MAID_A);
        CompoundTag exported = source.exportMaidData(MAID_A);

        CompoundTag targetRoot = new CompoundTag();
        BondData target = BondData.forTest(targetRoot);
        target.importMaidData(MAID_B, exported);

        assertEquals(source.getBondLevel(MAID_A), target.getBondLevel(MAID_B));
        assertEquals(source.isBondUnlocked(MAID_A), target.isBondUnlocked(MAID_B));
        assertEquals(source.getMaidModelId(MAID_A), target.getMaidModelId(MAID_B));
        assertEquals(source.getMaidDisplayName(MAID_A), target.getMaidDisplayName(MAID_B));
        assertEquals(source.getMaidSoundPackId(MAID_A), target.getMaidSoundPackId(MAID_B));
        assertEquals(source.getMaidYsmModelId(MAID_A), target.getMaidYsmModelId(MAID_B));
        assertEquals(source.getMaidYsmTexture(MAID_A), target.getMaidYsmTexture(MAID_B));
        assertEquals(source.getMaidYsmDisplayName(MAID_A), target.getMaidYsmDisplayName(MAID_B));
        assertEquals(source.getMaidRescueAction(MAID_A), target.getMaidRescueAction(MAID_B));
        assertEquals(source.getMaidRescueProviderId(MAID_A), target.getMaidRescueProviderId(MAID_B));
        assertEquals(source.getMaidLapPillowPose(MAID_A), target.getMaidLapPillowPose(MAID_B));
        assertEquals(source.getQueuedGiftCount(MAID_A), target.getQueuedGiftCount(MAID_B));
        assertEquals(source.getMorningKissVoiceSettings(MAID_A), target.getMorningKissVoiceSettings(MAID_B));
        assertEquals(source.getEmergencyRescueVoiceSettings(MAID_A), target.getEmergencyRescueVoiceSettings(MAID_B));
        assertEquals(source.getUnlockedAbilityIds(MAID_A), target.getUnlockedAbilityIds(MAID_B));

        // 运行态字段在新女仆上保持缺省（不带别处的会话时间戳）。
        assertEquals(0L, target.getLastGiftWallClockMs(MAID_B));
        assertEquals(0L, target.getLastGiftDeliveryGameTime(MAID_B));
        assertEquals(0, target.getLastGiftIntervalMinutes(MAID_B));
        assertEquals(0L, target.getMorningKissScheduledAttemptTick(MAID_B));
        assertEquals(0L, target.getMorningKissLastAutoAttemptGameTime(MAID_B));
        assertEquals("", target.getMorningKissLastSuccessfulWindowId(MAID_B));
        assertEquals("", target.getMorningKissLastFailedWindowId(MAID_B));
        assertEquals("", target.getMorningKissScheduledWindowId(MAID_B));
    }

    // ---------------------------------------------------------------- 空值不写入

    @Test
    void emptyStringsAreNotPersisted() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);

        data.setMaidSoundPackId(MAID_A, "pack_a");
        data.setMaidSoundPackId(MAID_A, "");
        assertAbsent(root, MAID_A, BondKeys.MAID_SOUND_PACK);
        assertEquals("", data.getMaidSoundPackId(MAID_A));

        data.setMaidYsmProfile(MAID_A, "ysm_model", "ysm_tex", "YSM 名");
        data.setMaidYsmProfile(MAID_A, null, "", null);
        assertAbsent(root, MAID_A, BondKeys.MAID_YSM_MODEL_ID);
        assertAbsent(root, MAID_A, BondKeys.MAID_YSM_TEXTURE);
        assertAbsent(root, MAID_A, BondKeys.MAID_YSM_DISPLAY_NAME);
        assertEquals("", data.getMaidYsmModelId(MAID_A));

        data.setMaidRescueAction(MAID_A, "tlm:rescue");
        data.setMaidRescueAction(MAID_A, "   ");
        assertAbsent(root, MAID_A, BondKeys.MAID_RESCUE_ACTION);
        assertEquals("", data.getMaidRescueAction(MAID_A));

        data.setMorningKissScheduledWindowId(MAID_A, "morning");
        data.setMorningKissScheduledWindowId(MAID_A, "");
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_SCHEDULED_WINDOW);
        assertEquals("", data.getMorningKissScheduledWindowId(MAID_A));

        data.setMorningKissLastSuccessfulWindowId(MAID_A, "morning");
        data.setMorningKissLastSuccessfulWindowId(MAID_A, "");
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_LAST_SUCCESS_WINDOW);
        assertEquals("", data.getMorningKissLastSuccessfulWindowId(MAID_A));

        data.setMorningKissLastFailedWindowId(MAID_A, "night");
        data.setMorningKissLastFailedWindowId(MAID_A, "");
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_LAST_FAILED_WINDOW);
        assertEquals("", data.getMorningKissLastFailedWindowId(MAID_A));

        data.setMorningKissScheduledWindowId(MAID_A, "morning");
        data.clearMorningKissSchedule(MAID_A);
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_SCHEDULED_WINDOW);
        assertEquals("", data.getMorningKissScheduledWindowId(MAID_A));

        data.setMaidLapPillowPose(MAID_A, LapPillowPoseSnapshot.maidSitPlayerLieDefault());
        assertAbsent(root, MAID_A, BondKeys.LAP_PILLOW_MAID_ACTION);
        assertAbsent(root, MAID_A, BondKeys.LAP_PILLOW_PLAYER_ACTION);
        assertEquals("", data.getMaidLapPillowPose(MAID_A).maidActionId());

        data.setMorningKissVoiceSettings(MAID_A, MorningKissVoiceSettings.DEFAULT);
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_VOICE_GROUP);
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_VOICE_CLIP);
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_VOICE_PACK);
        assertAbsent(root, MAID_A, BondKeys.MORNING_KISS_VOICE_POOL);
        assertEquals(MorningKissVoiceSettings.DEFAULT, data.getMorningKissVoiceSettings(MAID_A));

        data.setEmergencyRescueVoiceSettings(MAID_A, EmergencyRescueVoiceSettings.DEFAULT);
        assertAbsent(root, MAID_A, BondKeys.EMERGENCY_RESCUE_VOICE_TLM_GROUP);
        assertAbsent(root, MAID_A, BondKeys.EMERGENCY_RESCUE_VOICE_TLM_CLIP);
        assertAbsent(root, MAID_A, BondKeys.EMERGENCY_RESCUE_VOICE_FIXED_FILE);
        assertAbsent(root, MAID_A, BondKeys.EMERGENCY_RESCUE_VOICE_POOL);
        assertEquals(EmergencyRescueVoiceSettings.DEFAULT, data.getEmergencyRescueVoiceSettings(MAID_A));
    }

    @Test
    void emptyPlayerLevelSelectionsAreNotPersisted() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);

        data.setMorningKissSelectedWindowId("morning");
        data.setMorningKissSelectedMaidId(MAID_A.toString());
        data.clearMorningKissSelectedMaid();
        assertFalse(root.contains(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID));
        assertFalse(root.contains(BondKeys.MORNING_KISS_SELECTED_MAID_ID));
        assertEquals("", data.getMorningKissSelectedWindowId());
        assertEquals("", data.getMorningKissSelectedMaidId());

        data.setMorningKissSelectedWindowId("");
        data.setMorningKissSelectedMaidId(null);
        assertFalse(root.contains(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID));
        assertFalse(root.contains(BondKeys.MORNING_KISS_SELECTED_MAID_ID));
    }

    // ---------------------------------------------------------------- 辅助

    /** 一只含全部运行态键 + 画像键的女仆；{@code MAID_A} 的 YSM 纹理为空串（不落盘）。 */
    private static void populate(BondData data, UUID maidUuid) {
        data.setBondLevel(maidUuid, 7);
        data.setMaidModelId(maidUuid, "tlm:reimu");
        data.setMaidDisplayName(maidUuid, "灵梦");
        data.setMaidSoundPackId(maidUuid, "pack_a");
        data.setMaidYsmProfile(maidUuid, "ysm_model", "ysm_tex", "YSM 名");
        data.setMaidRescueAction(maidUuid, "tlm:rescue");
        data.setMaidRescueProviderId(maidUuid, "provider_a");
        data.setMaidLapPillowPose(maidUuid, LapPillowPoseSnapshot.maidSitPlayerLieDefault());
        data.setQueuedGiftCount(maidUuid, 3);
        data.setMorningKissVoiceSettings(maidUuid, MorningKissVoiceSettings.DEFAULT);
        data.setEmergencyRescueVoiceSettings(maidUuid, EmergencyRescueVoiceSettings.DEFAULT);
        data.unlockAbility(maidUuid, "lap_pillow");

        data.setLastGiftWallClockMs(maidUuid, 1_111L);
        data.setLastGiftDeliveryGameTime(maidUuid, 2_222L);
        data.setLastGiftIntervalMinutes(maidUuid, 45);
        data.setMorningKissScheduledWindowId(maidUuid, "morning");
        data.setMorningKissScheduledAttemptTick(maidUuid, 1_234L);
        data.setMorningKissLastAutoAttemptGameTime(maidUuid, 5_678L);
        data.setMorningKissLastSuccessfulWindowId(maidUuid, "morning");
        data.setMorningKissLastFailedWindowId(maidUuid, "night");
        data.setMaidLastSeen(maidUuid, 1_700_000_000_000L);
    }

    private static void assertAbsent(CompoundTag root, UUID maidUuid, String key) {
        assertFalse(maidSubtree(root, maidUuid).contains(key), "空值不应落盘: " + key);
    }

    private static CompoundTag maidSubtree(CompoundTag root, UUID maidUuid) {
        return root.getCompound(BondKeys.MAIDS).getCompound(maidUuid.toString());
    }
}