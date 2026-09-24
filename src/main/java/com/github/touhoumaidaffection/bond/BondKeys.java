package com.github.touhoumaidaffection.bond;

import java.util.Optional;
import java.util.UUID;

/**
 * 羁绊数据持久化键的集中定义。
 *
 * <p>存储布局（{@code SchemaVersion >= }{@value #CURRENT_SCHEMA}）：
 * <pre>
 * touhou_maid_affection.bond          (玩家 persistentData 下的根 compound)
 * ├── SchemaVersion                   (int，存储结构版本)
 * ├── maids                           (compound)
 * │   └── &lt;女仆 UUID&gt;                 (compound)
 * │       ├── BondLevel               (int)
 * │       ├── BondUnlocked            (boolean)
 * │       ├── BondAbilities           (compound)
 * │       ├── LastSeen                (long，epoch millis)
 * │       └── …其余女仆粒度 base 键
 * ├── MorningKissSelectedWindowId     (玩家粒度键，无 UUID 后缀)
 * └── MorningKissSelectedMaidId       (玩家粒度键，无 UUID 后缀)
 * </pre>
 *
 * <p>历史布局（{@code SchemaVersion} 缺失或 {@code < 2}）把所有女仆粒度数据扁平地存在根上，
 * 键形如 {@code <base>_<女仆UUID>}；{@link BondDataMigration} 负责一次性迁移到嵌套布局。
 *
 * <p>纯逻辑实现，不依赖任何 Minecraft 类型，便于单元测试。
 */
public final class BondKeys {

    /** 挂在玩家 persistentData 上的根键（沿用历史字面量，不改）。 */
    public static final String ROOT = "touhou_maid_affection.bond";
    /** 根下承载所有女仆粒度数据的子 compound。 */
    public static final String MAIDS = "maids";
    /** 根上的存储结构版本键。 */
    public static final String SCHEMA_VERSION_KEY = "SchemaVersion";
    /** 女仆子树中的最后在线时间（epoch millis），供 prune 判定。 */
    public static final String LAST_SEEN_KEY = "LastSeen";
    /** 当前存储结构版本：1 = 扁平键，2 = 按女仆嵌套。 */
    public static final int CURRENT_SCHEMA = 2;

    // ---- 女仆粒度 base 名（嵌套布局下即 maids.<uuid> 的子键，扁平布局下是 <base>_<uuid> 的前缀）----
    public static final String BOND_LEVEL = "BondLevel";
    public static final String BOND_UNLOCKED = "BondUnlocked";
    public static final String BOND_ABILITIES = "BondAbilities";
    public static final String BOND_ABILITY_VERSION = "BondAbilityVersion";

    public static final String MAID_MODEL = "BondMaidModel";
    public static final String MAID_DISPLAY_NAME = "BondMaidDisplayName";
    public static final String MAID_SOUND_PACK = "BondMaidSoundPack";
    public static final String MAID_YSM_MODEL_ID = "BondMaidYsmModelId";
    public static final String MAID_YSM_TEXTURE = "BondMaidYsmTexture";
    public static final String MAID_YSM_DISPLAY_NAME = "BondMaidYsmDisplayName";
    public static final String MAID_RESCUE_ACTION = "BondMaidRescueAction";
    public static final String MAID_RESCUE_PROVIDER = "BondMaidRescueProvider";

    public static final String LAP_PILLOW_MODE = "BondMaidLapPillow_Mode";
    public static final String LAP_PILLOW_MAID_OFFSET_X = "BondMaidLapPillow_MaidOffsetX";
    public static final String LAP_PILLOW_MAID_OFFSET_Y = "BondMaidLapPillow_MaidOffsetY";
    public static final String LAP_PILLOW_MAID_OFFSET_Z = "BondMaidLapPillow_MaidOffsetZ";
    public static final String LAP_PILLOW_PLAYER_OFFSET_X = "BondMaidLapPillow_PlayerOffsetX";
    public static final String LAP_PILLOW_PLAYER_OFFSET_Y = "BondMaidLapPillow_PlayerOffsetY";
    public static final String LAP_PILLOW_PLAYER_OFFSET_Z = "BondMaidLapPillow_PlayerOffsetZ";
    /** 旧版玩家偏移键，读取时作为 {@link #LAP_PILLOW_PLAYER_OFFSET_X} 的回退。 */
    public static final String LAP_PILLOW_LEGACY_OFFSET_X = "BondMaidLapPillow_OffsetX";
    public static final String LAP_PILLOW_LEGACY_OFFSET_Y = "BondMaidLapPillow_OffsetY";
    public static final String LAP_PILLOW_LEGACY_OFFSET_Z = "BondMaidLapPillow_OffsetZ";
    public static final String LAP_PILLOW_MAID_ACTION = "BondMaidLapPillow_MaidAction";
    public static final String LAP_PILLOW_PLAYER_ACTION = "BondMaidLapPillow_PlayerAction";

    public static final String RANDOM_GIFT_QUEUE = "RandomGiftQueue";
    public static final String RANDOM_GIFT_LAST_WALL_CLOCK = "RandomGiftLastWallClock";
    public static final String RANDOM_GIFT_LAST_DELIVERY = "RandomGiftLastDelivery";
    public static final String RANDOM_GIFT_LAST_INTERVAL_MINUTES = "RandomGiftLastIntervalMinutes";

    public static final String MORNING_KISS_LAST_SUCCESS_WINDOW = "MorningKissLastSuccessWindow";
    public static final String MORNING_KISS_LAST_FAILED_WINDOW = "MorningKissLastFailedWindow";
    public static final String MORNING_KISS_SCHEDULED_WINDOW = "MorningKissScheduledWindow";
    public static final String MORNING_KISS_SCHEDULED_ATTEMPT_TICK = "MorningKissScheduledAttemptTick";
    public static final String MORNING_KISS_LAST_AUTO_ATTEMPT_GAME_TIME = "MorningKissLastAutoAttemptGameTime";
    public static final String MORNING_KISS_VOICE_MODE = "MorningKissVoiceMode";
    public static final String MORNING_KISS_VOICE_GROUP = "MorningKissVoiceGroup";
    public static final String MORNING_KISS_VOICE_CLIP = "MorningKissVoiceClip";
    public static final String MORNING_KISS_VOICE_PACK = "MorningKissVoicePack";
    public static final String MORNING_KISS_VOICE_POOL = "MorningKissVoicePool";

    public static final String EMERGENCY_RESCUE_VOICE_SOURCE_MODE = "EmergencyRescueVoiceSourceMode";
    public static final String EMERGENCY_RESCUE_VOICE_TLM_MODE = "EmergencyRescueVoiceTlmMode";
    public static final String EMERGENCY_RESCUE_VOICE_TLM_GROUP = "EmergencyRescueVoiceTlmGroup";
    public static final String EMERGENCY_RESCUE_VOICE_TLM_CLIP = "EmergencyRescueVoiceTlmClip";
    public static final String EMERGENCY_RESCUE_VOICE_CUSTOM_MODE = "EmergencyRescueVoiceCustomMode";
    public static final String EMERGENCY_RESCUE_VOICE_FIXED_FILE = "EmergencyRescueVoiceFixedFile";
    public static final String EMERGENCY_RESCUE_VOICE_COMMON_FALLBACK = "EmergencyRescueVoiceCommonFallback";
    public static final String EMERGENCY_RESCUE_VOICE_POOL = "EmergencyRescueVoicePool";

    // ---- 玩家粒度键（始终留在根上，无 UUID 后缀）----
    public static final String MORNING_KISS_SELECTED_WINDOW_ID = "MorningKissSelectedWindowId";
    public static final String MORNING_KISS_SELECTED_MAID_ID = "MorningKissSelectedMaidId";

    private BondKeys() {
    }

    /**
     * 构造历史扁平键 {@code <base>_<maidUuid>}。base 为 {@code null}/空白或 uuid 为 {@code null} 时返回 {@code null}。
     */
    public static String flatKey(String baseName, UUID maidUuid) {
        if (baseName == null || baseName.isBlank() || maidUuid == null) {
            return null;
        }
        return baseName + "_" + maidUuid;
    }

    /**
     * 若 {@code key} 恰为 {@code <base>_<maidUuid>}，返回 base；否则返回 {@link Optional#empty()}。
     *
     * <p>UUID 后缀按不区分大小写匹配，以容忍历史上可能写入的大写形式。
     */
    public static Optional<String> baseOfFlatKey(String key, UUID maidUuid) {
        if (key == null || maidUuid == null) {
            return Optional.empty();
        }
        String suffix = "_" + maidUuid;
        if (key.length() <= suffix.length()) {
            return Optional.empty();
        }
        if (!key.regionMatches(true, key.length() - suffix.length(), suffix, 0, suffix.length())) {
            return Optional.empty();
        }
        return Optional.of(key.substring(0, key.length() - suffix.length()));
    }

    /**
     * 解析历史扁平键 {@code <base>_<maidUuid>} 为 (女仆 UUID, base)。
     *
     * <p>分隔符取最后一个下划线（UUID 自身不含下划线），因此 base 内允许出现下划线，
     * 例如 {@code BondMaidLapPillow_Mode_<uuid>} → base {@code BondMaidLapPillow_Mode}。
     * 不含下划线、下划线位于首/尾、或后缀不是合法 UUID 时返回 {@link Optional#empty()}，
     * 调用方应把这些键原样保留。
     */
    public static Optional<ParsedFlatKey> parseFlatKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        int split = key.lastIndexOf('_');
        if (split <= 0 || split >= key.length() - 1) {
            return Optional.empty();
        }
        UUID maidUuid;
        try {
            maidUuid = UUID.fromString(key.substring(split + 1));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        return Optional.of(new ParsedFlatKey(maidUuid, key.substring(0, split)));
    }

    /** 历史扁平键解析结果：女仆 UUID 与去掉后缀的 base 名。 */
    public record ParsedFlatKey(UUID maidUuid, String baseName) {
    }
}