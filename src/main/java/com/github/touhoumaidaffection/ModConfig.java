package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.ai.mimo.MimoProtocol;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Locale;

public class ModConfig {
    public static final ForgeConfigSpec SPEC;

    // Kiss cooldown (tiered by favorability level)
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_0;
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_1;
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_2;
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_3;

    // Favorability
    public static final ForgeConfigSpec.IntValue FAVORABILITY_POINTS;
    public static final ForgeConfigSpec.IntValue FAVORABILITY_COOLDOWN;

    // Bond ability costs
    public static final ForgeConfigSpec.IntValue BOND_COST_LAP_PILLOW;
    public static final ForgeConfigSpec.IntValue BOND_COST_EMERGENCY_HEAL;
    public static final ForgeConfigSpec.IntValue BOND_COST_MORNING_KISS;
    public static final ForgeConfigSpec.IntValue BOND_COST_YSM_ACTION;
    public static final ForgeConfigSpec.IntValue BOND_COST_RANDOM_GIFT;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_MAX_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue BOND_RANDOM_GIFT_ENABLED;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_MAX_QUEUED;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_DELIVERY_SEARCH_RANGE;
    public static final ForgeConfigSpec.DoubleValue BOND_RANDOM_GIFT_DELIVERY_REACH_DISTANCE;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_DELIVERY_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_PATHFIND_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.BooleanValue BOND_RANDOM_GIFT_SHOW_ACTION_BAR;
    public static final ForgeConfigSpec.BooleanValue BOND_RANDOM_GIFT_INCLUDE_MOD_ITEMS;
    public static final ForgeConfigSpec.IntValue BOND_RANDOM_GIFT_AUTO_MOD_SAMPLE_SIZE;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_ENABLED;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_REQUIRED_FAVORABILITY;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BOND_MORNING_KISS_ALLOWED_TIME_RANGES;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_MIN_KISS_COUNT;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_MAX_KISS_COUNT;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_KISS_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_APPLY_MAIDS_PRAYER;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_MAIDS_PRAYER_DURATION;
    public static final ForgeConfigSpec.ConfigValue<String> BOND_MORNING_KISS_MESSAGE_DISPLAY_MODE;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_DIALOGUE_CHAT_BUBBLE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AI_DIALOGUE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<String> BOND_MORNING_KISS_AI_DIALOGUE_PROMPT;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AI_DIALOGUE_PREGENERATE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AI_DIALOGUE_IMMEDIATE_FALLBACK_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_AI_DIALOGUE_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_AI_DIALOGUE_SCAN_DISTANCE;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AI_DIALOGUE_VERBOSE_LOG;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AUTO_ENABLED;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_AUTO_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BOND_MORNING_KISS_AUTO_WINDOW_ATTEMPT_SPREAD_PERCENT;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AUTO_SILENT_FAILURE;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AUTO_ALLOW_ALL_ELIGIBLE_MAIDS;
    public static final ForgeConfigSpec.BooleanValue BOND_MORNING_KISS_AUTO_SINGLE_ACTIVE_TASK_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue BOND_EMERGENCY_RESCUE_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue BOND_EMERGENCY_RESCUE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME;
    public static final ForgeConfigSpec.IntValue BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID;
    public static final ForgeConfigSpec.BooleanValue BOND_EMERGENCY_RESCUE_COMMON_FALLBACK_DEFAULT;
    public static final ForgeConfigSpec.IntValue BOND_EMERGENCY_RESCUE_SYNC_SCAN_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.BooleanValue BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG;
    public static final ForgeConfigSpec.DoubleValue BOND_EMERGENCY_RESCUE_VIEW_X_ROT_OFFSET;
    public static final ForgeConfigSpec.DoubleValue BOND_EMERGENCY_RESCUE_VIEW_Y_ROT_OFFSET;
    public static final ForgeConfigSpec.DoubleValue BOND_EMERGENCY_RESCUE_VIEW_Z_ROT_OFFSET;
    public static final ForgeConfigSpec.IntValue BOND_LAP_PILLOW_MAX_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue BOND_LAP_PILLOW_ETERNAL_UTOPIA_PARTICLES_ENABLED;

    // Buff
    public static final ForgeConfigSpec.BooleanValue BUFF_ENABLED;
    public static final ForgeConfigSpec.IntValue BUFF_KISS_THRESHOLD;
    public static final ForgeConfigSpec.IntValue BUFF_KISS_WINDOW;
    public static final ForgeConfigSpec.IntValue BUFF_DURATION;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_0;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_1;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_2;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_3;

    // Particles
    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT_MIN;
    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT_EXTRA;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_OFFSET_Y;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SPREAD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_FORWARD_OFFSET;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_AVOID_VIEW_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_SHAPE_MODE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_UPWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_RADIAL_SPEED;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SWIRL_SPEED;
    public static final ForgeConfigSpec.IntValue PARTICLE_PHASE_BURSTS;
    public static final ForgeConfigSpec.IntValue PARTICLE_PHASE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_PHASE_RAMP;
    public static final ForgeConfigSpec.IntValue PARTICLE_CLUSTER_COPIES;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_CLUSTER_JITTER;
    public static final ForgeConfigSpec.BooleanValue PARTICLE_ACCENT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_ACCENT_TYPE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_ACCENT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue PARTICLE_FAVORABILITY_COLOR_ACCENT;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_ACCENT_COLOR_MODE;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_R;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_G;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_B;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_A;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_R;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_G;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_B;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_A;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_R;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_G;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_B;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_A;

    // FOV zoom
    public static final ForgeConfigSpec.BooleanValue FOV_ZOOM_ENABLED;
    public static final ForgeConfigSpec.IntValue FOV_ZOOM_IN_TICKS;
    public static final ForgeConfigSpec.IntValue FOV_HOLD_TICKS;
    public static final ForgeConfigSpec.IntValue FOV_ZOOM_OUT_TICKS;
    public static final ForgeConfigSpec.DoubleValue FOV_ZOOM_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue FOV_CARRIED_SIDE_OFFSET;
    public static final ForgeConfigSpec.DoubleValue FOV_CARRIED_FORWARD_OFFSET;
    public static final ForgeConfigSpec.DoubleValue FOV_CARRIED_VERTICAL_OFFSET;

    // TMA MiMo adapter for Touhou Little Maid AI services
    public static final ForgeConfigSpec.BooleanValue TMA_MIMO_ADAPTER_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> TMA_MIMO_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> TMA_MIMO_CHAT_URL;
    public static final ForgeConfigSpec.ConfigValue<String> TMA_MIMO_TTS_URL;
    public static final ForgeConfigSpec.IntValue TMA_MIMO_MAX_COMPLETION_TOKENS;
    public static final ForgeConfigSpec.ConfigValue<String> TMA_MIMO_TTS_VOICE_PROMPT;
    public static final ForgeConfigSpec.ConfigValue<String> TMA_MIMO_TTS_AUDIO_FORMAT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Kiss cooldown settings (in ticks, 20 ticks = 1 second)",
                        "Cooldown decreases as the maid's favorability level increases")
               .push("cooldown");

        COOLDOWN_LEVEL_0 = builder
                .comment("Cooldown at favorability level 0 (default: 100 = 5 seconds)")
                .defineInRange("level0", 100, 0, 6000);

        COOLDOWN_LEVEL_1 = builder
                .comment("Cooldown at favorability level 1 (default: 60 = 3 seconds)")
                .defineInRange("level1", 60, 0, 6000);

        COOLDOWN_LEVEL_2 = builder
                .comment("Cooldown at favorability level 2 (default: 20 = 1 second)")
                .defineInRange("level2", 20, 0, 6000);

        COOLDOWN_LEVEL_3 = builder
                .comment("Cooldown at favorability level 3 / max (default: 0 = no cooldown)")
                .defineInRange("level3", 0, 0, 6000);

        builder.pop();

        builder.comment("Favorability gain settings")
               .push("favorability");

        FAVORABILITY_POINTS = builder
                .comment("Favorability points gained per kiss (default: 3)")
                .defineInRange("points", 3, 1, 100);

        FAVORABILITY_COOLDOWN = builder
                .comment("Favorability gain cooldown in ticks (default: 600 = 30 seconds)",
                         "This is separate from the interaction cooldown - prevents favorability farming")
                .defineInRange("cooldownTicks", 600, 0, 72000);

        builder.pop();

        builder.comment("Bond ability P point costs",
                        "Defaults are tuned for Touhou Little Maid's normal P point acquisition pace")
               .push("bondCosts");

        BOND_COST_LAP_PILLOW = builder
                .comment("P point cost for Lap Pillow")
                .defineInRange("lapPillow", 8, 0, 9999);

        BOND_COST_EMERGENCY_HEAL = builder
                .comment("P point cost for Emergency Heal")
                .defineInRange("emergencyHeal", 3, 0, 9999);

        BOND_COST_MORNING_KISS = builder
                .comment("P point cost for Morning Kiss")
                .defineInRange("morningKiss", 12, 0, 9999);

        BOND_COST_YSM_ACTION = builder
                .comment("P point cost for YSM Action")
                .defineInRange("ysmAction", 16, 0, 9999);

        BOND_COST_RANDOM_GIFT = builder
                .comment("P point cost for Random Gift")
                .defineInRange("randomGift", 6, 0, 9999);

        BOND_RANDOM_GIFT_MAX_DISTANCE = builder
                .comment("Maximum distance to gift items after unlock")
                .defineInRange("randomGiftMaxDistance", 6, 1, 64);

        builder.comment("Automatic random gift behaviour after unlock")
                .push("randomGiftBehavior");

        BOND_RANDOM_GIFT_ENABLED = builder
                .comment("Enable the automatic random gift behavior after unlock")
                .define("enabled", true);

        BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES = builder
                .comment("Real-time minutes required to prepare one gift")
                .defineInRange("intervalRealMinutes", 20, 1, 1440);

        BOND_RANDOM_GIFT_MAX_QUEUED = builder
                .comment("Maximum number of queued gifts per maid")
                .defineInRange("maxQueuedGifts", 7, 1, 64);

        BOND_RANDOM_GIFT_DELIVERY_SEARCH_RANGE = builder
                .comment("Range around the player to search for nearby gift maids")
                .defineInRange("deliverySearchRange", 24, 4, 128);

        BOND_RANDOM_GIFT_DELIVERY_REACH_DISTANCE = builder
                .comment("Distance at which the maid can throw the gift")
                .defineInRange("deliveryReachDistance", 2.25, 0.5, 16.0);

        BOND_RANDOM_GIFT_DELIVERY_COOLDOWN_TICKS = builder
                .comment("Minimum ticks between consecutive automatic gift throws from the same maid")
                .defineInRange("deliveryCooldownTicks", 40, 0, 24000);

        BOND_RANDOM_GIFT_PATHFIND_TIMEOUT_TICKS = builder
                .comment("Timeout for a single automatic gift delivery pathfinding task")
                .defineInRange("pathfindTimeoutTicks", 200, 20, 24000);

        BOND_RANDOM_GIFT_SHOW_ACTION_BAR = builder
                .comment("Show action bar text when a maid throws a gift")
                .define("showActionBar", true);

        BOND_RANDOM_GIFT_INCLUDE_MOD_ITEMS = builder
                .comment("Automatically sample a batch of non-vanilla mod items into the gift pool")
                .define("includeModItems", true);

        BOND_RANDOM_GIFT_AUTO_MOD_SAMPLE_SIZE = builder
                .comment("How many non-vanilla mod items are auto-sampled into the gift pool")
                .defineInRange("autoModSampleSize", 96, 0, 2048);

        builder.pop();

        builder.comment("Morning Kiss behavior after unlock")
                .push("morningKissBehavior");

        BOND_MORNING_KISS_ENABLED = builder
                .comment("Enable Morning Kiss after the ability is unlocked")
                .define("enabled", true);

        BOND_MORNING_KISS_REQUIRED_FAVORABILITY = builder
                .comment("Required maid favorability level to use Morning Kiss")
                .defineInRange("requiredFavorabilityLevel", 3, 0, 3);

        BOND_MORNING_KISS_MAX_DISTANCE = builder
                .comment("Maximum distance to call a maid for Morning Kiss")
                .defineInRange("maxDistance", 16, 1, 128);

        BOND_MORNING_KISS_TIMEOUT_TICKS = builder
                .comment("Morning Kiss timeout in ticks")
                .defineInRange("timeoutTicks", 200, 20, 2400);

        BOND_MORNING_KISS_ALLOWED_TIME_RANGES = builder
                .comment("Allowed in-world time ranges for Morning Kiss, using 24-hour time",
                        "Examples: 06:00-08:00, 18:00-20:00",
                        "Optional dialogue bucket prefix is supported: morning@06:00-08:00, evening@18:00-20:00",
                        "Legacy tick ranges like 0-2000 are still accepted for compatibility")
                .defineListAllowEmpty(
                        java.util.List.of("allowedTimeRanges"),
                        java.util.List.of("06:00-08:00", "18:00-20:00"),
                        value -> value instanceof String string && !string.isBlank()
                );

        BOND_MORNING_KISS_MIN_KISS_COUNT = builder
                .comment("Minimum number of kisses performed in one Morning Kiss sequence")
                .defineInRange("minKissCount", 1, 1, 3);

        BOND_MORNING_KISS_MAX_KISS_COUNT = builder
                .comment("Maximum number of kisses performed in one Morning Kiss sequence")
                .defineInRange("maxKissCount", 3, 1, 3);

        BOND_MORNING_KISS_KISS_INTERVAL_TICKS = builder
                .comment("Ticks between consecutive kisses in one Morning Kiss sequence")
                .defineInRange("kissIntervalTicks", 16, 1, 200);

        BOND_MORNING_KISS_APPLY_MAIDS_PRAYER = builder
                .comment("Apply Maid's Prayer during Morning Kiss")
                .define("applyMaidsPrayer", true);

        BOND_MORNING_KISS_MAIDS_PRAYER_DURATION = builder
                .comment("Maid's Prayer duration applied by Morning Kiss in ticks")
                .defineInRange("maidsPrayerDurationTicks", 600, 20, 72000);

        BOND_MORNING_KISS_MESSAGE_DISPLAY_MODE = builder
                .comment("Where Morning Kiss prompts and dialogue are shown",
                        "Allowed values: action_bar, chat")
                .define("messageDisplayMode", "action_bar");

        BOND_MORNING_KISS_DIALOGUE_CHAT_BUBBLE_ENABLED = builder
                .comment("Show Morning Kiss dialogue above the maid with Touhou Little Maid chat bubbles when possible",
                        "If disabled, dialogue uses messageDisplayMode instead")
                .define("dialogueChatBubbleEnabled", true);

        BOND_MORNING_KISS_AI_DIALOGUE_ENABLED = builder
                .comment("Enable Morning Kiss AI-generated dialogue integration",
                        "Requires Touhou Little Maid LLM to be enabled and configured on the maid")
                .define("aiDialogueEnabled", false);

        BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE = builder
                .comment("Language passed to Touhou Little Maid AI chat for immediate Morning Kiss dialogue fallback")
                .define("aiDialogueLanguage", "zh_cn");

        BOND_MORNING_KISS_AI_DIALOGUE_PROMPT = builder
                .comment("Prompt template for Morning Kiss AI-generated dialogue",
                        "Placeholders: {maid}, {player}, {pool}, {time}")
                .define("aiDialoguePrompt", "你正在扮演 Minecraft 中名为 {maid} 的女仆。现在是 {pool} 时段，你刚给玩家 {player} 送上早安吻或晚安吻。请只输出自然、温柔、适合显示在游戏内的中文台词，不要解释，不要添加引号。");

        BOND_MORNING_KISS_AI_DIALOGUE_PREGENERATE_ENABLED = builder
                .comment("Pregenerate AI Morning Kiss dialogue outside active Morning Kiss time windows to reduce trigger latency")
                .define("aiDialoguePregenerateEnabled", true);

        BOND_MORNING_KISS_AI_DIALOGUE_IMMEDIATE_FALLBACK_ENABLED = builder
                .comment("If no pregenerated line is cached, request one live through Touhou Little Maid chat during Morning Kiss",
                        "This can add visible latency, so it is disabled by default")
                .define("aiDialogueImmediateFallbackEnabled", false);

        BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED = builder
                .comment("Generate remote TTS OGG voice for pregenerated Morning Kiss dialogue when the maid has TTS configured",
                        "System/local TTS and non-OGG responses are ignored and fall back to text-only cached dialogue")
                .define("aiDialogueTtsEnabled", true);

        BOND_MORNING_KISS_AI_DIALOGUE_SCAN_INTERVAL_TICKS = builder
                .comment("Ticks between AI dialogue pregeneration scans")
                .defineInRange("aiDialogueScanIntervalTicks", 1200, 20, 72000);

        BOND_MORNING_KISS_AI_DIALOGUE_SCAN_DISTANCE = builder
                .comment("Range around each player to find owned maids for AI dialogue pregeneration")
                .defineInRange("aiDialogueScanDistance", 16, 1, 128);

        BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL = builder
                .comment("Target number of pregenerated Morning Kiss lines kept per maid and time pool")
                .defineInRange("aiDialogueCacheTargetPerPool", 4, 1, 8);

        BOND_MORNING_KISS_AI_DIALOGUE_VERBOSE_LOG = builder
                .comment("Enable verbose debug logs for Morning Kiss AI dialogue pregeneration")
                .define("aiDialogueVerboseLog", false);

        BOND_MORNING_KISS_AUTO_ENABLED = builder
                .comment("Allow maids to proactively trigger Morning Kiss during allowed time windows")
                .define("autoEnabled", true);

        BOND_MORNING_KISS_AUTO_SCAN_INTERVAL_TICKS = builder
                .comment("How often the server scans nearby loaded maids for proactive Morning Kiss")
                .defineInRange("autoScanIntervalTicks", 40, 5, 1200);

        BOND_MORNING_KISS_AUTO_WINDOW_ATTEMPT_SPREAD_PERCENT = builder
                .comment("How much of the current time window can be used for random proactive attempt scheduling")
                .defineInRange("autoWindowAttemptSpreadPercent", 70, 10, 100);

        BOND_MORNING_KISS_AUTO_SILENT_FAILURE = builder
                .comment("When true, proactive Morning Kiss failures stay silent instead of notifying the player")
                .define("autoSilentFailure", true);

        BOND_MORNING_KISS_AUTO_ALLOW_ALL_ELIGIBLE_MAIDS = builder
                .comment("When true, all eligible nearby maids may proactively trigger Morning Kiss in the same time window one after another",
                        "When false, only one maid is selected for the whole time window")
                .define("autoAllowAllEligibleMaids", true);

        BOND_MORNING_KISS_AUTO_SINGLE_ACTIVE_TASK_PER_PLAYER = builder
                .comment("Allow only one active Morning Kiss task per player at a time")
                .define("autoSingleActiveTaskPerPlayer", true);

        builder.pop();

        builder.comment("Emergency rescue behaviour after unlock")
                .push("emergencyRescueBehavior");

        BOND_EMERGENCY_RESCUE_ENABLED = builder
                .comment("Master switch for emergency rescue interception")
                .define("enabled", true);

        BOND_EMERGENCY_RESCUE_HEALTH_THRESHOLD = builder
                .comment("Emergency rescue trigger threshold in health points")
                .defineInRange("healthThreshold", 4, 1, 20);

        BOND_EMERGENCY_RESCUE_REFRESH_BY_DAYTIME = builder
                .comment("Refresh rescue charges by Minecraft date/daytime progression instead of total game uptime",
                        "When enabled, sleeping to the next day or /time add 24000 can refresh charges")
                .define("refreshByDayTime", true);

        BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID = builder
                .comment("How many rescue charges each unlocked maid contributes on daily refresh")
                .defineInRange("chargesPerMaid", 1, 1, 16);

        BOND_EMERGENCY_RESCUE_COMMON_FALLBACK_DEFAULT = builder
                .comment("Default value of common voice fallback for rescue custom voice profiles")
                .define("commonFallbackDefault", true);

        BOND_EMERGENCY_RESCUE_SYNC_SCAN_INTERVAL_SECONDS = builder
                .comment("Seconds between server rescue predefined sound folder scans")
                .defineInRange("syncScanIntervalSeconds", 30, 5, 3600);

        BOND_EMERGENCY_RESCUE_SYNC_VERBOSE_LOG = builder
                .comment("Verbose logs for rescue sound sync and playback fallback")
                .define("syncVerboseLog", true);

        BOND_EMERGENCY_RESCUE_VIEW_X_ROT_OFFSET = builder
                .comment("Additional X-axis rotation offset for the rescue overlay maid shown in front of the player",
                        "Negative tilts backward, positive tilts forward",
                        "Range: -180 to 180 degrees")
                .defineInRange("overlayViewPitchOffset", 0.0, -180.0, 180.0);

        BOND_EMERGENCY_RESCUE_VIEW_Y_ROT_OFFSET = builder
                .comment("Additional Y-axis rotation offset for the rescue overlay maid shown in front of the player",
                        "Useful when you want the maid to face the screen more directly",
                        "Range: -180 to 180 degrees")
                .defineInRange("overlayViewYawOffset", 180.0, -180.0, 180.0);

        BOND_EMERGENCY_RESCUE_VIEW_Z_ROT_OFFSET = builder
                .comment("Additional Z-axis rotation offset for the rescue overlay maid shown in front of the player",
                        "Negative rolls left, positive rolls right",
                        "Range: -180 to 180 degrees")
                .defineInRange("overlayViewRollOffset", 0.0, -180.0, 180.0);

        builder.pop();

        BOND_LAP_PILLOW_MAX_DISTANCE = builder
                .comment("Maximum distance to start lap pillow")
                .defineInRange("lapPillowMaxDistance", 3, 1, 16);

        BOND_LAP_PILLOW_ETERNAL_UTOPIA_PARTICLES_ENABLED = builder
                .comment("Show particles for Eternal Utopia during lap pillow (default: true)")
                .define("lapPillowEternalUtopiaParticles", true);

        builder.pop();

        builder.comment("Maid's Prayer buff settings",
                        "Triggered by kissing multiple times in a short window")
               .push("buff");

        BUFF_ENABLED = builder
                .comment("Enable the Maid's Prayer buff (default: true)")
                .define("enabled", true);

        BUFF_KISS_THRESHOLD = builder
                .comment("Number of kisses needed to trigger the buff (default: 3)")
                .defineInRange("kissThreshold", 3, 1, 20);

        BUFF_KISS_WINDOW = builder
                .comment("Time window in ticks to reach the kiss threshold (default: 200 = 10 seconds)")
                .defineInRange("kissWindowTicks", 200, 20, 6000);

        BUFF_DURATION = builder
                .comment("Buff duration in ticks (default: 600 = 30 seconds)")
                .defineInRange("durationTicks", 600, 20, 72000);

        builder.comment("Regeneration amplifier per favorability level (0 = I, 1 = II, etc.)",
                        "Higher levels can exceed vanilla limits — the power of love knows no bounds")
               .push("amplifier");

        BUFF_AMPLIFIER_LEVEL_0 = builder
                .comment("Amplifier at favorability level 0 (default: 0 = Regen I)")
                .defineInRange("level0", 0, 0, 255);

        BUFF_AMPLIFIER_LEVEL_1 = builder
                .comment("Amplifier at favorability level 1 (default: 1 = Regen II)")
                .defineInRange("level1", 1, 0, 255);

        BUFF_AMPLIFIER_LEVEL_2 = builder
                .comment("Amplifier at favorability level 2 (default: 2 = Regen III, beyond vanilla)")
                .defineInRange("level2", 2, 0, 255);

        BUFF_AMPLIFIER_LEVEL_3 = builder
                .comment("Amplifier at favorability level 3 (default: 4 = Regen V, far beyond vanilla)")
                .defineInRange("level3", 4, 0, 255);

        builder.pop();
        builder.pop();

        builder.comment("Particle settings")
               .push("particles");

        PARTICLE_COUNT_MIN = builder
                .comment("Minimum number of heart particles per kiss (default: 3)")
                .defineInRange("minCount", 3, 0, 50);

        PARTICLE_COUNT_EXTRA = builder
                .comment("Extra random particles (total = min + random(0..extra)) (default: 4)")
                .defineInRange("extraRandom", 4, 0, 50);

        PARTICLE_OFFSET_Y = builder
                .comment("Vertical offset applied to particle anchor to avoid covering faces (default: 0.12)")
                .defineInRange("offsetY", 0.12, -1.0, 2.5);

        PARTICLE_SPREAD_RADIUS = builder
                .comment("Horizontal spread radius of the particle field (default: 0.18)")
                .defineInRange("spreadRadius", 0.18, 0.0, 2.0);

        PARTICLE_FORWARD_OFFSET = builder
                .comment("Forward offset of the particle anchor along player->maid direction (default: 0.45)")
                .defineInRange("forwardOffset", 0.45, -1.0, 1.0);

        PARTICLE_AVOID_VIEW_STRENGTH = builder
                .comment("How strongly particles avoid the center line of sight (default: 0.0)")
                .defineInRange("avoidViewStrength", 0.0, 0.0, 2.0);

        PARTICLE_SHAPE_MODE = builder
                .comment("Particle spatial mode: RING | HALO | SPIRAL (default: SPIRAL)")
                .define("shapeMode", ParticleShapeMode.SPIRAL.name());

        PARTICLE_UPWARD_SPEED = builder
                .comment("Base upward speed of particles (default: 0.08)")
                .defineInRange("upwardSpeed", 0.08, -0.2, 1.0);

        PARTICLE_RADIAL_SPEED = builder
                .comment("Base radial expansion speed from anchor (default: 0.0)")
                .defineInRange("radialSpeed", 0.0, -0.3, 0.3);

        PARTICLE_SWIRL_SPEED = builder
                .comment("Tangential swirl speed component, useful for spiral feeling (default: 0.0)")
                .defineInRange("swirlSpeed", 0.0, -2.0, 2.0);

        PARTICLE_PHASE_BURSTS = builder
                .comment("How many timed burst phases a kiss emits (default: 5)")
                .defineInRange("phaseBursts", 5, 1, 12);

        PARTICLE_PHASE_INTERVAL_TICKS = builder
                .comment("Interval between burst phases in ticks (default: 2)")
                .defineInRange("phaseIntervalTicks", 2, 1, 20);

        PARTICLE_PHASE_RAMP = builder
                .comment("Burst count ramp mode: EASE_IN | UNIFORM (default: EASE_IN)")
                .define("phaseRamp", ParticlePhaseRamp.EASE_IN.name());

        PARTICLE_CLUSTER_COPIES = builder
                .comment("Extra micro-copies per logical heart to simulate larger size (default: 1)")
                .defineInRange("clusterCopies", 1, 0, 8);

        PARTICLE_CLUSTER_JITTER = builder
                .comment("Jitter radius used for heart cluster size simulation (default: 0.03)")
                .defineInRange("clusterJitter", 0.03, 0.0, 0.3);

        PARTICLE_ACCENT_ENABLED = builder
                .comment("Enable subtle accent particles (DUST/GLOW) to enrich atmosphere (default: true)")
                .define("accentEnabled", true);

        PARTICLE_ACCENT_TYPE = builder
                .comment("Accent particle type: NONE | DUST | GLOW (default: GLOW)")
                .define("accentType", ParticleAccentType.GLOW.name());

        PARTICLE_ACCENT_CHANCE = builder
                .comment("Chance per logical particle to spawn an accent particle (default: 0.7)")
                .defineInRange("accentChance", 0.7, 0.0, 1.0);

        PARTICLE_FAVORABILITY_COLOR_ACCENT = builder
                .comment("When true, accent color can shift with maid favorability (default: true)")
                .define("favorabilityColorAccent", true);

        PARTICLE_ACCENT_COLOR_MODE = builder
                .comment("Accent color mode for DUST accent particles: FAVORABILITY | SOLID | GRADIENT (default: FAVORABILITY)")
                .define("accentColorMode", ParticleAccentColorMode.FAVORABILITY.name());

        builder.comment("Solid accent color RGBA (0..255)").push("accentSolidRgba");
        PARTICLE_ACCENT_SOLID_R = builder.defineInRange("r", 255, 0, 255);
        PARTICLE_ACCENT_SOLID_G = builder.defineInRange("g", 166, 0, 255);
        PARTICLE_ACCENT_SOLID_B = builder.defineInRange("b", 204, 0, 255);
        PARTICLE_ACCENT_SOLID_A = builder.defineInRange("a", 220, 0, 255);
        builder.pop();

        builder.comment("Gradient accent color start RGBA (0..255)").push("accentGradientStartRgba");
        PARTICLE_ACCENT_GRADIENT_START_R = builder.defineInRange("r", 255, 0, 255);
        PARTICLE_ACCENT_GRADIENT_START_G = builder.defineInRange("g", 204, 0, 255);
        PARTICLE_ACCENT_GRADIENT_START_B = builder.defineInRange("b", 224, 0, 255);
        PARTICLE_ACCENT_GRADIENT_START_A = builder.defineInRange("a", 200, 0, 255);
        builder.pop();

        builder.comment("Gradient accent color end RGBA (0..255)").push("accentGradientEndRgba");
        PARTICLE_ACCENT_GRADIENT_END_R = builder.defineInRange("r", 255, 0, 255);
        PARTICLE_ACCENT_GRADIENT_END_G = builder.defineInRange("g", 118, 0, 255);
        PARTICLE_ACCENT_GRADIENT_END_B = builder.defineInRange("b", 170, 0, 255);
        PARTICLE_ACCENT_GRADIENT_END_A = builder.defineInRange("a", 230, 0, 255);
        builder.pop();

        builder.pop();

        builder.comment("FOV zoom effect on kiss",
                        "Creates a smooth 'lean-in' feeling by narrowing the FOV")
               .push("fov");

        FOV_ZOOM_ENABLED = builder
                .comment("Enable FOV zoom on kiss (default: true)")
                .define("enabled", true);

        FOV_ZOOM_IN_TICKS = builder
                .comment("Zoom-in duration in ticks (default: 4 = 0.2s)")
                .defineInRange("zoomInTicks", 4, 1, 40);

        FOV_HOLD_TICKS = builder
                .comment("Hold at max zoom duration in ticks (default: 3 = 0.15s)")
                .defineInRange("holdTicks", 3, 0, 40);

        FOV_ZOOM_OUT_TICKS = builder
                .comment("Zoom-out duration in ticks (default: 6 = 0.3s)")
                .defineInRange("zoomOutTicks", 6, 1, 60);

        FOV_ZOOM_STRENGTH = builder
                .comment("Zoom strength (0.0 = no zoom, 1.0 = full zoom to 0 FOV) (default: 0.85)")
                .defineInRange("strength", 0.85, 0.0, 0.95);

        FOV_CARRIED_SIDE_OFFSET = builder
                .comment("Princess-carry camera target side offset relative to player look direction",
                        "Negative = left, positive = right (default: 0.48)")
                .defineInRange("carriedSideOffset", 0.48, -1.5, 1.5);

        FOV_CARRIED_FORWARD_OFFSET = builder
                .comment("Princess-carry camera target forward offset (default: 0.16)")
                .defineInRange("carriedForwardOffset", 0.16, -1.0, 1.0);

        FOV_CARRIED_VERTICAL_OFFSET = builder
                .comment("Princess-carry camera target vertical offset from player eye (default: -0.10)")
                .defineInRange("carriedVerticalOffset", -0.10, -1.0, 1.0);

        builder.pop();

        builder.comment("TMA MiMo adapter defaults for Touhou Little Maid AI chat/TTS",
                        "These values seed the TLM site editor defaults; per-site edits are still stored by TLM under config/touhou_little_maid/sites.",
                        "Do not publish your API key. Leave it blank here if you prefer entering it in the TLM AI settings GUI.")
                .push("tmaMimoAdapter");

        TMA_MIMO_ADAPTER_ENABLED = builder
                .comment("Register TMA MiMo chat and TTS providers into Touhou Little Maid AI settings")
                .define("enabled", true);

        TMA_MIMO_API_KEY = builder
                .comment("Default Xiaomi MiMo API key used when creating the TLM MiMo sites",
                        "Blank by default; the TLM site editor can store it separately")
                .define("apiKey", "");

        TMA_MIMO_CHAT_URL = builder
                .comment("Default MiMo OpenAI-compatible chat completions endpoint")
                .define("chatUrl", "https://api.xiaomimimo.com/v1/chat/completions");

        TMA_MIMO_TTS_URL = builder
                .comment("Default MiMo TTS endpoint; MiMo V2.5 TTS uses the chat completions API")
                .define("ttsUrl", "https://api.xiaomimimo.com/v1/chat/completions");

        TMA_MIMO_MAX_COMPLETION_TOKENS = builder
                .comment("Default max_completion_tokens for MiMo chat requests sent through the TMA adapter")
                .defineInRange("maxCompletionTokens", 1024, 1, 8192);

        TMA_MIMO_TTS_VOICE_PROMPT = builder
                .comment("Default voice design prompt for mimo-v2.5-tts-voicedesign")
                .define("ttsVoicePrompt", "温柔、清澈、亲近的年轻女性声音，语速自然，适合 Minecraft 女仆角色。");

        TMA_MIMO_TTS_AUDIO_FORMAT = builder
                .comment("Default requested MiMo TTS audio format",
                        "MiMo V2.5 TTS supports mp3, wav, pcm, and pcm16; TLM's AI TTS playback path supports mp3 most reliably")
                .define("ttsAudioFormat", MimoProtocol.DEFAULT_TTS_AUDIO_FORMAT);

        builder.pop();

        SPEC = builder.build();
    }

    public static ParticleShapeMode getParticleShapeMode() {
        return parseEnum(PARTICLE_SHAPE_MODE.get(), ParticleShapeMode.HALO);
    }

    public static ParticlePhaseRamp getParticlePhaseRamp() {
        return parseEnum(PARTICLE_PHASE_RAMP.get(), ParticlePhaseRamp.EASE_IN);
    }

    public static ParticleAccentType getParticleAccentType() {
        return parseEnum(PARTICLE_ACCENT_TYPE.get(), ParticleAccentType.NONE);
    }

    public static ParticleAccentColorMode getParticleAccentColorMode() {
        return parseEnum(PARTICLE_ACCENT_COLOR_MODE.get(), ParticleAccentColorMode.FAVORABILITY);
    }

    private static <E extends Enum<E>> E parseEnum(String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<E> enumClass = (Class<E>) fallback.getDeclaringClass();
            return Enum.valueOf(enumClass, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public enum ParticleShapeMode {
        RING,
        HALO,
        SPIRAL
    }

    public enum ParticlePhaseRamp {
        EASE_IN,
        UNIFORM
    }

    public enum ParticleAccentType {
        NONE,
        DUST,
        GLOW
    }

    public enum ParticleAccentColorMode {
        FAVORABILITY,
        SOLID,
        GRADIENT
    }
}
