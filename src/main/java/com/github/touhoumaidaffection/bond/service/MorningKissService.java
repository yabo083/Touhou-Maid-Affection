package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.bond.VoicePoolSelection;
import com.github.touhoumaidaffection.bond.service.MorningKissScheduleRules.DialoguePool;
import com.github.touhoumaidaffection.handler.KissMaidHandler;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import com.github.touhoumaidaffection.network.MorningKissDataVoicePlayPayload;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.github.touhoumaidaffection.ysm.YSMMaidAnimation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class MorningKissService {
    private static final Map<DialoguePool, String[]> DIALOGUE_KEYS = Map.of(
            DialoguePool.MORNING, new String[]{
                    "bond.morning_kiss.dialogue.morning.1",
                    "bond.morning_kiss.dialogue.morning.2",
                    "bond.morning_kiss.dialogue.morning.3",
                    "bond.morning_kiss.dialogue.morning.4",
                    "bond.morning_kiss.dialogue.morning.5",
                    "bond.morning_kiss.dialogue.morning.6"
            },
            DialoguePool.EVENING, new String[]{
                    "bond.morning_kiss.dialogue.evening.1",
                    "bond.morning_kiss.dialogue.evening.2",
                    "bond.morning_kiss.dialogue.evening.3",
                    "bond.morning_kiss.dialogue.evening.4",
                    "bond.morning_kiss.dialogue.evening.5",
                    "bond.morning_kiss.dialogue.evening.6"
            },
            DialoguePool.GENERAL, new String[]{
                    "bond.morning_kiss.dialogue.general.1",
                    "bond.morning_kiss.dialogue.general.2",
                    "bond.morning_kiss.dialogue.general.3",
                    "bond.morning_kiss.dialogue.general.4"
            }
    );

    private static final Map<UUID, PendingMorningKiss> TASKS = new HashMap<>();
    private static final Map<String, Integer> VOICE_SEQUENCE_INDEX = new HashMap<>();
    private static final double KISS_REACH_DISTANCE_SQR = 3.24D;
    private static final double LAX_KISS_REACH_DISTANCE_SQR = 5.76D;
    private static final double MAX_VERTICAL_DELTA = 2.5D;

    private MorningKissService() {
    }

    public static boolean canStart(Player player, EntityMaid maid) {
        return getFailureKey(player, maid) == null;
    }

    public static String getFailureKey(Player player, EntityMaid maid) {
        if (!ModConfig.BOND_MORNING_KISS_ENABLED.get()) {
            return "bond.morning_kiss.failed_disabled";
        }
        if (player == null || maid == null || !maid.isAlive()) {
            return "bond.morning_kiss.failed_invalid";
        }
        if (maid.getFavorabilityManager().getLevel() < ModConfig.BOND_MORNING_KISS_REQUIRED_FAVORABILITY.get()) {
            return "bond.morning_kiss.failed_favorability";
        }
        if (player instanceof ServerPlayer serverPlayer
                && LapPillowState.isActive(serverPlayer)
                && !LapPillowState.isSessionMaid(serverPlayer, maid.getUUID())) {
            return "bond.morning_kiss.failed_invalid";
        }
        double maxDistance = ModConfig.BOND_MORNING_KISS_MAX_DISTANCE.get();
        if (player.distanceToSqr(maid) > maxDistance * maxDistance) {
            return "bond.morning_kiss.failed_distance";
        }
        if (!isAllowedTime(player.level())) {
            return "bond.morning_kiss.failed_time";
        }
        return null;
    }

    public static boolean start(Player player, EntityMaid maid) {
        return start(player, maid, TriggerSource.MANUAL, "", true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        tickAutoScheduling(server);
        tickActiveTasks(server);
    }

    public static String getAllowedTimeRangesText() {
        List<MorningKissScheduleRules.TimeRange> ranges = resolveAllowedTimeRanges();
        if (ranges.isEmpty()) {
            return "--";
        }
        List<String> labels = new ArrayList<>();
        for (MorningKissScheduleRules.TimeRange range : ranges) {
            labels.add(range.toDisplayString());
        }
        return String.join(", ", labels);
    }

    public static String getKissCountRangeText() {
        int min = getSafeMinKissCount();
        int max = getSafeMaxKissCount();
        return min == max ? String.valueOf(min) : min + "-" + max;
    }

    public static void cancelForPlayerExcept(ServerPlayer player, UUID allowedMaidUuid) {
        if (player == null) {
            return;
        }
        PendingMorningKiss task = TASKS.get(player.getUUID());
        if (task == null) {
            return;
        }
        if (allowedMaidUuid == null || !allowedMaidUuid.equals(task.maidUuid())) {
            TASKS.remove(player.getUUID());
        }
    }

    private static void tickAutoScheduling(MinecraftServer server) {
        if (!ModConfig.BOND_MORNING_KISS_ENABLED.get() || !ModConfig.BOND_MORNING_KISS_AUTO_ENABLED.get()) {
            return;
        }

        int scanInterval = ModConfig.BOND_MORNING_KISS_AUTO_SCAN_INTERVAL_TICKS.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().isClientSide()) {
                continue;
            }
            long gameTime = player.serverLevel().getGameTime();
            if (scanInterval > 1 && gameTime % scanInterval != 0L) {
                continue;
            }
            ActiveTimeWindow activeWindow = getActiveTimeWindow(player.serverLevel());
            if (activeWindow == null) {
                continue;
            }
            if (ModConfig.BOND_MORNING_KISS_AUTO_SINGLE_ACTIVE_TASK_PER_PLAYER.get() && TASKS.containsKey(player.getUUID())) {
                continue;
            }
            scheduleOrStartAutoTasks(player, activeWindow);
        }
    }

    private static void scheduleOrStartAutoTasks(ServerPlayer player, ActiveTimeWindow activeWindow) {
        double maxDistance = ModConfig.BOND_MORNING_KISS_MAX_DISTANCE.get();
        List<EntityMaid> nearbyMaids = player.serverLevel().getEntitiesOfClass(
                EntityMaid.class,
                player.getBoundingBox().inflate(maxDistance),
                maid -> maid.isAlive()
                        && maid.isOwnedBy(player)
                        && BondManager.isAbilityUnlocked(player, maid.getUUID(), "morning_kiss")
                        && maid.getFavorabilityManager().getLevel() >= ModConfig.BOND_MORNING_KISS_REQUIRED_FAVORABILITY.get()
        );
        nearbyMaids.sort(Comparator.comparingDouble(maid -> maid.distanceToSqr(player)));
        if (LapPillowState.isActive(player)) {
            UUID activeMaidUuid = LapPillowState.getMaidUuid(player);
            if (activeMaidUuid != null) {
                nearbyMaids.removeIf(maid -> !activeMaidUuid.equals(maid.getUUID()));
            }
        }
        if (nearbyMaids.isEmpty()) {
            return;
        }

        if (!ModConfig.BOND_MORNING_KISS_AUTO_ALLOW_ALL_ELIGIBLE_MAIDS.get()) {
            nearbyMaids = selectSingleAutoMaid(player, activeWindow, nearbyMaids);
            if (nearbyMaids.isEmpty()) {
                return;
            }
        }

        for (EntityMaid maid : nearbyMaids) {
            UUID maidUuid = maid.getUUID();
            if (activeWindow.windowId().equals(BondManager.getMorningKissLastSuccessfulWindowId(player, maidUuid))) {
                continue;
            }
            if (activeWindow.windowId().equals(BondManager.getMorningKissLastFailedWindowId(player, maidUuid))) {
                continue;
            }

            ensureScheduledAttempt(player, maid, activeWindow);
            long scheduledTick = BondManager.getMorningKissScheduledAttemptTick(player, maidUuid);
            if (scheduledTick <= 0L || player.serverLevel().getGameTime() < scheduledTick) {
                continue;
            }
            if (ModConfig.BOND_MORNING_KISS_AUTO_SINGLE_ACTIVE_TASK_PER_PLAYER.get() && TASKS.containsKey(player.getUUID())) {
                return;
            }

            BondManager.setMorningKissLastAutoAttemptGameTime(player, maidUuid, player.serverLevel().getGameTime());
            if (start(player, maid, TriggerSource.AUTO_WINDOW, activeWindow.windowId(), false)) {
                showMorningKissMessage(player, startMessage("bond.morning_kiss.auto_started", "bond.morning_kiss.auto_started.chat", maid));
            } else {
                markAutoFailure(player, maidUuid, activeWindow.windowId(), null);
            }
        }
    }

    private static void ensureScheduledAttempt(ServerPlayer player, EntityMaid maid, ActiveTimeWindow activeWindow) {
        UUID maidUuid = maid.getUUID();
        String scheduledWindowId = BondManager.getMorningKissScheduledWindowId(player, maidUuid);
        long scheduledAttemptTick = BondManager.getMorningKissScheduledAttemptTick(player, maidUuid);
        if (activeWindow.windowId().equals(scheduledWindowId) && scheduledAttemptTick > 0L) {
            return;
        }

        long earliest = Math.min(activeWindow.windowStartTick() + 20L, activeWindow.windowEndTick());
        long spreadEnd = activeWindow.windowStartTick() + Math.max(1L,
                (activeWindow.windowEndTick() - activeWindow.windowStartTick()) * ModConfig.BOND_MORNING_KISS_AUTO_WINDOW_ATTEMPT_SPREAD_PERCENT.get() / 100L);
        long latest = Math.min(spreadEnd, Math.max(earliest, activeWindow.windowEndTick() - ModConfig.BOND_MORNING_KISS_TIMEOUT_TICKS.get()));
        if (latest < earliest) {
            latest = earliest;
        }

        RandomSource random = player.getRandom();
        long bound = latest - earliest + 1L;
        long offset = bound > 1L ? (long) Math.floor(random.nextDouble() * bound) : 0L;
        long attemptTick = earliest + offset;
        BondManager.setMorningKissScheduledWindowId(player, maidUuid, activeWindow.windowId());
        BondManager.setMorningKissScheduledAttemptTick(player, maidUuid, attemptTick);
    }

    private static void tickActiveTasks(MinecraftServer server) {
        Iterator<PendingMorningKiss> iterator = TASKS.values().iterator();
        while (iterator.hasNext()) {
            PendingMorningKiss task = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(task.playerUuid());
            if (player == null || player.level().dimension() != task.dimension()) {
                iterator.remove();
                continue;
            }
            Entity entity = player.serverLevel().getEntity(task.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
                failTask(iterator, task, player, "bond.morning_kiss.failed_invalid");
                continue;
            }
            if (LapPillowState.isActive(player) && !LapPillowState.isSessionMaid(player, maid.getUUID())) {
                iterator.remove();
                continue;
            }

            long gameTime = player.serverLevel().getGameTime();
            if (gameTime > task.timeoutTick()) {
                failTask(iterator, task, player, "bond.morning_kiss.failed_timeout");
                continue;
            }

            double maxDistance = ModConfig.BOND_MORNING_KISS_MAX_DISTANCE.get();
            if (player.distanceToSqr(maid) > maxDistance * maxDistance) {
                failTask(iterator, task, player, "bond.morning_kiss.failed_distance");
                continue;
            }
            if (maid.getFavorabilityManager().getLevel() < ModConfig.BOND_MORNING_KISS_REQUIRED_FAVORABILITY.get()) {
                failTask(iterator, task, player, "bond.morning_kiss.failed_favorability");
                continue;
            }

            maid.getLookControl().setLookAt(player, 30.0F, 30.0F);

            boolean lapPillowSessionMaid = LapPillowState.isSessionMaid(player, maid.getUUID());
            if (!isReadyToKiss(task, player, maid, gameTime)) {
                if (lapPillowSessionMaid) {
                    maid.getNavigation().stop();
                    continue;
                }
                maid.getNavigation().moveTo(player, 1.0D);
                continue;
            }

            maid.getNavigation().stop();
            ResolvedMorningKissVoice selectedVoice = resolveSelectedVoicePoolEntry(player, maid);
            boolean playKissSound = shouldPlayMorningKissSoundWithVoice(player, maid, selectedVoice);
            if (!KissMaidHandler.performMorningKiss(player, maid, playKissSound)) {
                failTask(iterator, task, player, null);
                continue;
            }

            if (!task.blessingApplied() && ModConfig.BOND_MORNING_KISS_APPLY_MAIDS_PRAYER.get()) {
                KissMaidHandler.applyMaidsPrayer(player, maid, ModConfig.BOND_MORNING_KISS_MAIDS_PRAYER_DURATION.get());
                task = task.withBlessingApplied(true);
            }

            if (!task.dialogueShown()) {
                boolean aiDialogueDispatched = maybeShowDialogue(player, maid, task.dialoguePool());
                if (!aiDialogueDispatched) {
                    playDataDrivenOrConfiguredVoice(player, maid, selectedVoice);
                }
                task = task.withDialogueShown(true);
            }
            if (!lapPillowSessionMaid) {
                YSMActionBridge.playIfAvailable(maid, YSMMaidAnimation.MORNING_KISS);
            }

            int completed = task.completedKisses() + 1;
            if (completed >= task.totalKisses()) {
                iterator.remove();
                onTaskCompleted(task, player, maid.getUUID());
                continue;
            }

            TASKS.put(task.playerUuid(), task.withProgress(
                    completed,
                    gameTime + ModConfig.BOND_MORNING_KISS_KISS_INTERVAL_TICKS.get()
            ));
        }
    }

    private static boolean start(Player player, EntityMaid maid, TriggerSource triggerSource, String autoWindowId, boolean showStartMessage) {
        String failureKey = getFailureKey(player, maid);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (LapPillowState.isActive(serverPlayer) && !LapPillowState.isSessionMaid(serverPlayer, maid.getUUID())) {
            return false;
        }
        if (TASKS.containsKey(serverPlayer.getUUID())) {
            return false;
        }
        if (failureKey != null) {
            if (showStartMessage) {
                showMorningKissMessage(serverPlayer, Component.translatable(failureKey));
            }
            return false;
        }

        long gameTime = serverPlayer.serverLevel().getGameTime();
        ActiveTimeWindow activeTimeWindow = getActiveTimeWindow(serverPlayer.serverLevel());
        PendingMorningKiss task = new PendingMorningKiss(
                serverPlayer.level().dimension(),
                serverPlayer.getUUID(),
                maid.getUUID(),
                gameTime + ModConfig.BOND_MORNING_KISS_TIMEOUT_TICKS.get(),
                rollKissCount(serverPlayer.getRandom()),
                0,
                gameTime,
                false,
                false,
                triggerSource,
                autoWindowId == null ? "" : autoWindowId,
                activeTimeWindow != null ? activeTimeWindow.range().dialoguePool() : DialoguePool.GENERAL
        );
        TASKS.put(serverPlayer.getUUID(), task);
        maid.getNavigation().moveTo(serverPlayer, 1.0D);
        if (showStartMessage) {
            showMorningKissMessage(serverPlayer, startMessage("bond.morning_kiss.started", "bond.morning_kiss.started.chat", maid));
        }
        return true;
    }

    private static void onTaskCompleted(PendingMorningKiss task, ServerPlayer player, UUID maidUuid) {
        if (task.triggerSource() == TriggerSource.AUTO_WINDOW && !task.autoWindowId().isBlank()) {
            BondManager.setMorningKissLastSuccessfulWindowId(player, maidUuid, task.autoWindowId());
            BondManager.clearMorningKissSchedule(player, maidUuid);
        }
    }

    private static void failTask(Iterator<PendingMorningKiss> iterator, PendingMorningKiss task, ServerPlayer player, String failureKey) {
        iterator.remove();
        if (task.triggerSource() == TriggerSource.AUTO_WINDOW && !task.autoWindowId().isBlank()) {
            markAutoFailure(player, task.maidUuid(), task.autoWindowId(), failureKey);
            return;
        }
        if (failureKey != null) {
            showMorningKissMessage(player, Component.translatable(failureKey));
        }
    }

    private static void markAutoFailure(ServerPlayer player, UUID maidUuid, String autoWindowId, String failureKey) {
        BondManager.setMorningKissLastFailedWindowId(player, maidUuid, autoWindowId);
        BondManager.clearMorningKissSchedule(player, maidUuid);
        if (!ModConfig.BOND_MORNING_KISS_AUTO_SILENT_FAILURE.get() && failureKey != null) {
            showMorningKissMessage(player, Component.translatable(failureKey));
        }
    }

    private static List<EntityMaid> selectSingleAutoMaid(ServerPlayer player, ActiveTimeWindow activeWindow, List<EntityMaid> nearbyMaids) {
        String selectedWindowId = BondManager.getMorningKissSelectedWindowId(player);
        if (!Objects.equals(activeWindow.windowId(), selectedWindowId)) {
            EntityMaid chosen = nearbyMaids.getFirst();
            BondManager.setMorningKissSelectedWindowId(player, activeWindow.windowId());
            BondManager.setMorningKissSelectedMaidId(player, chosen.getUUID().toString());
            return List.of(chosen);
        }

        String selectedMaidId = BondManager.getMorningKissSelectedMaidId(player);
        if (selectedMaidId == null || selectedMaidId.isBlank()) {
            EntityMaid chosen = nearbyMaids.getFirst();
            BondManager.setMorningKissSelectedMaidId(player, chosen.getUUID().toString());
            return List.of(chosen);
        }

        for (EntityMaid maid : nearbyMaids) {
            if (selectedMaidId.equals(maid.getUUID().toString())) {
                return List.of(maid);
            }
        }
        return List.of();
    }

    static boolean isAllowedTime(Level level) {
        return getActiveTimeWindow(level) != null;
    }

    private static ActiveTimeWindow getActiveTimeWindow(Level level) {
        long gameTime = level.getDayTime();
        long day = Math.floorDiv(gameTime, 24000L);
        long dayTime = Math.floorMod(gameTime, 24000L);
        List<MorningKissScheduleRules.TimeRange> ranges = resolveAllowedTimeRanges();
        if (ranges.isEmpty()) {
            return null;
        }

        for (int index = 0; index < ranges.size(); index++) {
            MorningKissScheduleRules.TimeRange range = ranges.get(index);
            if (!range.contains(dayTime)) {
                continue;
            }

            long windowStartDay = day;
            if (range.crossesMidnight() && dayTime <= range.endTick()) {
                windowStartDay = day - 1L;
            }
            long windowStartTick = windowStartDay * 24000L + range.startTick();
            long windowEndTick = range.crossesMidnight()
                    ? windowStartDay * 24000L + 24000L + range.endTick()
                    : windowStartDay * 24000L + range.endTick();
            String windowId = level.dimension().location() + "|" + windowStartDay + "|" + index;
            return new ActiveTimeWindow(index, windowId, windowStartTick, windowEndTick, range);
        }
        return null;
    }

    private static List<MorningKissScheduleRules.TimeRange> resolveAllowedTimeRanges() {
        return MorningKissScheduleRules.resolveAllowedTimeRanges(ModConfig.BOND_MORNING_KISS_ALLOWED_TIME_RANGES.get());
    }

    private static int rollKissCount(RandomSource random) {
        int min = getSafeMinKissCount();
        int max = getSafeMaxKissCount();
        return Mth.nextInt(random, min, max);
    }

    private static int getSafeMinKissCount() {
        int min = ModConfig.BOND_MORNING_KISS_MIN_KISS_COUNT.get();
        int max = ModConfig.BOND_MORNING_KISS_MAX_KISS_COUNT.get();
        return MorningKissScheduleRules.safeMinKissCount(min, max);
    }

    private static int getSafeMaxKissCount() {
        int min = ModConfig.BOND_MORNING_KISS_MIN_KISS_COUNT.get();
        int max = ModConfig.BOND_MORNING_KISS_MAX_KISS_COUNT.get();
        return MorningKissScheduleRules.safeMaxKissCount(min, max);
    }

    private static boolean isReadyToKiss(PendingMorningKiss task, ServerPlayer player, EntityMaid maid, long gameTime) {
        if (gameTime < task.nextKissTick()) {
            return false;
        }
        double horizontalDeltaSqr = horizontalDistanceSqr(player, maid);
        double verticalDelta = Math.abs(player.getY() - maid.getY());
        if (horizontalDeltaSqr <= KISS_REACH_DISTANCE_SQR && verticalDelta <= MAX_VERTICAL_DELTA) {
            return true;
        }
        boolean pathDone = maid.getNavigation().isDone();
        boolean isFinalApproach = horizontalDeltaSqr <= LAX_KISS_REACH_DISTANCE_SQR && verticalDelta <= MAX_VERTICAL_DELTA;
        if (pathDone && isFinalApproach) {
            return true;
        }
        return task.completedKisses() > 0 && isFinalApproach;
    }

    private static double horizontalDistanceSqr(Player player, EntityMaid maid) {
        double dx = player.getX() - maid.getX();
        double dz = player.getZ() - maid.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean maybeShowDialogue(ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool) {
        GeneratedDialogueResult generated = tryShowGeneratedDialogue(player, maid, dialoguePool);
        if (generated != GeneratedDialogueResult.MISSING) {
            return true;
        }
        if (tryShowAiDialogue(player, maid, dialoguePool)) {
            return true;
        }
        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileData.getActiveProfile();
        List<String> configuredPool = profile.dialogues().getOrDefault(dialoguePool, List.of());
        if (configuredPool.isEmpty()) {
            configuredPool = profile.dialogues().getOrDefault(DialoguePool.GENERAL, List.of());
        }
        if (!configuredPool.isEmpty()) {
            if (profile.dialogueMode() == MorningKissProfileParser.DialogueMode.APPEND) {
                int vanillaCount = DIALOGUE_KEYS.getOrDefault(dialoguePool, DIALOGUE_KEYS.get(DialoguePool.GENERAL)).length;
                int index = player.getRandom().nextInt(configuredPool.size() + vanillaCount);
                if (index >= configuredPool.size()) {
                    showBuiltinDialogue(player, maid, dialoguePool, index - configuredPool.size());
                    return false;
                }
            }
            showConfiguredDialogue(player, maid, dialoguePool, configuredPool);
            return false;
        }
        showBuiltinDialogue(player, maid, dialoguePool, -1);
        return false;
    }

    private static void showConfiguredDialogue(ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool, List<String> configuredPool) {
        String raw = configuredPool.get(player.getRandom().nextInt(configuredPool.size()));
        showMorningKissDialogue(player, maid, Component.literal(renderTemplate(raw, player, maid, dialoguePool)));
    }

    private static void showBuiltinDialogue(ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool, int preferredIndex) {
        String[] pool = DIALOGUE_KEYS.getOrDefault(dialoguePool, DIALOGUE_KEYS.get(DialoguePool.GENERAL));
        int index = preferredIndex >= 0 && preferredIndex < pool.length ? preferredIndex : player.getRandom().nextInt(pool.length);
        String key = pool[index];
        showMorningKissDialogue(player, maid, Component.translatable(key, getResolvedNameForMode(maid, true)));
    }

    private static GeneratedDialogueResult tryShowGeneratedDialogue(ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool) {
        return MorningKissGeneratedDialogueService.pollCachedLine(maid, dialoguePool, player.getRandom())
                .map(entry -> {
                    showMorningKissDialogue(player, maid, Component.literal(entry.text()));
                    if (entry.hasVoice()) {
                        PacketDistributor.sendToPlayer(player, new MorningKissDataVoicePlayPayload(
                                maid.getId(),
                                maid.getUUID(),
                                entry.voiceFileName(),
                                entry.voiceData()
                        ));
                        return GeneratedDialogueResult.VOICE_PLAYED;
                    }
                    return GeneratedDialogueResult.TEXT_ONLY;
                })
                .orElse(GeneratedDialogueResult.MISSING);
    }

    private static boolean tryShowAiDialogue(ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool) {
        if (!ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.get()
                || !ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_IMMEDIATE_FALLBACK_ENABLED.get()) {
            return false;
        }
        try {
            if (!AIConfig.LLM_ENABLED.get()) {
                TouhouMaidAffection.LOGGER.debug("Skipping live Morning Kiss AI dialogue for {}: TLM LLM is disabled.", maid.getUUID());
                return false;
            }
            LLMSite site = maid.getAiChatManager().getLLMSite();
            if (site == null || !site.enabled()) {
                TouhouMaidAffection.LOGGER.debug("Skipping live Morning Kiss AI dialogue for {}: maid has no enabled LLM site.", maid.getUUID());
                return false;
            }
            String prompt = renderTemplate(ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PROMPT.get(), player, maid, dialoguePool);
            ChatClientInfo clientInfo = new ChatClientInfo(
                    ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get(),
                    MaidDisplayNameResolver.resolveChatSafeDisplayName(maid).getString(),
                    List.of()
            );
            TouhouMaidAffection.LOGGER.info("Dispatching live Morning Kiss AI dialogue for maid {} pool {}.", maid.getUUID(), dialoguePool.name().toLowerCase(Locale.ROOT));
            maid.getAiChatManager().chat(prompt, clientInfo, player);
            return true;
        } catch (Throwable throwable) {
            TouhouMaidAffection.LOGGER.warn("Failed to dispatch live Morning Kiss AI dialogue, falling back to static dialogue.", throwable);
            return false;
        }
    }

    private static String renderTemplate(String raw, ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String maidName = MaidDisplayNameResolver.resolveChatSafeDisplayName(maid).getString();
        return raw
                .replace("{maid}", maidName)
                .replace("{player}", player.getName().getString())
                .replace("{pool}", dialoguePool.name().toLowerCase(Locale.ROOT))
                .replace("{time}", getAllowedTimeRangesText());
    }

    private static boolean playConfiguredVoice(ServerPlayer player, EntityMaid maid) {
        String soundPackId = maid.getSoundPackId();
        if (soundPackId == null || soundPackId.isBlank()) {
            return false;
        }
        MorningKissVoiceSettings settings = BondManager.getMorningKissVoiceSettings(player, maid.getUUID()).withSoundPackId(soundPackId);
        PacketDistributor.sendToPlayer(player, new MorningKissVoicePlayPayload(
                maid.getId(),
                maid.getUUID(),
                soundPackId,
                settings.mode().serializedName(),
                settings.selectedGroup(),
                settings.selectedClip(),
                selectTlmFallbackVoiceId(settings, soundPackId)
        ));
        return true;
    }

    private static String selectTlmFallbackVoiceId(MorningKissVoiceSettings settings, String soundPackId) {
        if (settings.mode() == MorningKissVoiceSettings.Mode.SPECIFIC_CLIP && !settings.selectedClip().isBlank()) {
            return VoicePoolIds.tlm(settings.selectedClip());
        }
        return "";
    }

    private static boolean shouldPlayMorningKissSoundWithVoice(ServerPlayer player, EntityMaid maid, ResolvedMorningKissVoice selectedVoice) {
        if (MorningKissProfileData.shouldPlayKissSoundWithVoice()) {
            return true;
        }
        if (selectedVoice != null) {
            return selectedVoice.isBuiltin();
        }
        return !hasMorningKissVoiceCandidate(player, maid);
    }

    private static boolean hasMorningKissVoiceCandidate(ServerPlayer player, EntityMaid maid) {
        if (InteractionVoiceProfileData.resolveMorningKiss(maid).hasVoices()) {
            return true;
        }
        if (MorningKissProfileData.hasDataPackVoices()) {
            return true;
        }
        return maid.getSoundPackId() != null && !maid.getSoundPackId().isBlank();
    }

    private static void playDataDrivenOrConfiguredVoice(ServerPlayer player, EntityMaid maid, ResolvedMorningKissVoice selectedVoice) {
        if (playResolvedVoicePoolEntry(player, maid, selectedVoice)) {
            return;
        }
        InteractionVoiceProfileData.ResolvedVoiceProfile interactionProfile = InteractionVoiceProfileData.resolveMorningKiss(maid);
        boolean hasUnifiedDataPackVoices = interactionProfile.hasVoices();
        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileData.getActiveProfile();
        boolean hasDataPackVoices = MorningKissProfileData.hasDataPackVoices();
        boolean hasConfiguredVoice = maid.getSoundPackId() != null && !maid.getSoundPackId().isBlank();
        if (hasUnifiedDataPackVoices) {
            if (interactionProfile.voiceMode() == InteractionVoiceProfileParser.VoiceMode.APPEND && hasConfiguredVoice) {
                if (player.getRandom().nextBoolean()) {
                    if (playUnifiedDataPackVoice(player, maid, interactionProfile)) {
                        return;
                    }
                    playConfiguredVoice(player, maid);
                    return;
                }
                if (playConfiguredVoice(player, maid)) {
                    return;
                }
                playUnifiedDataPackVoice(player, maid, interactionProfile);
                return;
            }
            if (playUnifiedDataPackVoice(player, maid, interactionProfile)) {
                return;
            }
            if (interactionProfile.voiceMode() != InteractionVoiceProfileParser.VoiceMode.REPLACE) {
                playConfiguredVoice(player, maid);
            }
            return;
        }
        if (profile.voiceMode() == MorningKissProfileParser.VoiceMode.APPEND && hasDataPackVoices && hasConfiguredVoice) {
            if (player.getRandom().nextBoolean()) {
                if (playDataPackVoice(player, maid)) {
                    return;
                }
                playConfiguredVoice(player, maid);
                return;
            }
            if (playConfiguredVoice(player, maid)) {
                return;
            }
            playDataPackVoice(player, maid);
            return;
        }
        if (playDataPackVoice(player, maid)) {
            return;
        }
        if (!hasDataPackVoices || profile.voiceMode() != MorningKissProfileParser.VoiceMode.REPLACE) {
            playConfiguredVoice(player, maid);
        }
    }

    private static ResolvedMorningKissVoice resolveSelectedVoicePoolEntry(ServerPlayer player, EntityMaid maid) {
        String soundPackId = maid.getSoundPackId() == null ? "" : maid.getSoundPackId();
        MorningKissVoiceSettings settings = BondManager.getMorningKissVoiceSettings(player, maid.getUUID()).withSoundPackId(soundPackId);
        InteractionVoiceProfileData.ResolvedVoiceProfile profile = InteractionVoiceProfileData.resolveMorningKiss(maid);
        List<String> selectedIds = effectiveMorningKissVoiceIds(settings.selectedVoiceIds(), profile);
        if (selectedIds.isEmpty()) {
            return null;
        }
        String selectedId = selectVoiceId(selectedIds, settings.mode(), "morning:" + maid.getUUID(), player.getRandom());
        if (selectedId.isBlank()) {
            return null;
        }
        if (VoicePoolIds.BUILTIN_MORNING_KISS.equals(selectedId)) {
            return ResolvedMorningKissVoice.builtin();
        }
        if (VoicePoolIds.isDataPack(selectedId)) {
            return InteractionVoiceProfileData.selectVoiceByFile(profile, VoicePoolIds.value(selectedId))
                    .map(voice -> ResolvedMorningKissVoice.dataPack(selectedId, voice))
                    .orElse(null);
        }
        if (VoicePoolIds.isTlm(selectedId) && !soundPackId.isBlank()) {
            return ResolvedMorningKissVoice.tlm(selectedId);
        }
        return null;
    }

    private static List<String> effectiveMorningKissVoiceIds(List<String> savedIds, InteractionVoiceProfileData.ResolvedVoiceProfile profile) {
        List<String> defaults = defaultMorningKissVoiceIds(profile);
        boolean includeBasePool = VoicePoolSelection.shouldIncludeBasePool(
                profile.voiceMode().name().toLowerCase(Locale.ROOT),
                profile.fileNames()
        );
        if (savedIds == null || savedIds.isEmpty()) {
            return defaults;
        }
        if (includeBasePool) {
            return savedIds;
        }
        List<String> dataPackOnly = savedIds.stream()
                .filter(VoicePoolIds::isDataPack)
                .filter(id -> profile.fileNames().contains(VoicePoolIds.value(id)))
                .distinct()
                .toList();
        return dataPackOnly.isEmpty() ? defaults : dataPackOnly;
    }

    private static List<String> defaultMorningKissVoiceIds(InteractionVoiceProfileData.ResolvedVoiceProfile profile) {
        ArrayList<String> ids = new ArrayList<>();
        boolean includeBasePool = VoicePoolSelection.shouldIncludeBasePool(
                profile.voiceMode().name().toLowerCase(Locale.ROOT),
                profile.fileNames()
        );
        if (includeBasePool) {
            ids.add(VoicePoolIds.BUILTIN_MORNING_KISS);
        }
        ids.addAll(profile.fileNames().stream().map(VoicePoolIds::dataPack).toList());
        return ids;
    }

    private static boolean playResolvedVoicePoolEntry(ServerPlayer player, EntityMaid maid, ResolvedMorningKissVoice selectedVoice) {
        if (selectedVoice == null) {
            return false;
        }
        if (selectedVoice.isBuiltin()) {
            return true;
        }
        if (selectedVoice.dataPackVoice() != null) {
            InteractionVoiceProfileData.DataPackVoice voice = selectedVoice.dataPackVoice();
            PacketDistributor.sendToPlayer(player, new MorningKissDataVoicePlayPayload(
                    maid.getId(),
                    maid.getUUID(),
                    voice.fileName(),
                    voice.data()
            ));
            return true;
        }
        if (VoicePoolIds.isTlm(selectedVoice.selectedId())) {
            String soundPackId = maid.getSoundPackId() == null ? "" : maid.getSoundPackId();
            if (soundPackId.isBlank()) {
                return false;
            }
            MorningKissVoiceSettings settings = BondManager.getMorningKissVoiceSettings(player, maid.getUUID()).withSoundPackId(soundPackId);
            PacketDistributor.sendToPlayer(player, new MorningKissVoicePlayPayload(
                    maid.getId(),
                    maid.getUUID(),
                    soundPackId,
                    settings.mode().serializedName(),
                    "",
                    VoicePoolIds.value(selectedVoice.selectedId()),
                    selectedVoice.selectedId()
            ));
            return true;
        }
        return false;
    }

    private static String selectVoiceId(List<String> ids, MorningKissVoiceSettings.Mode mode, String key, RandomSource random) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return switch (mode) {
            case RANDOM_ALL -> ids.get(random.nextInt(ids.size()));
            case RANDOM_GROUP -> {
                int index = VOICE_SEQUENCE_INDEX.getOrDefault(key, 0);
                VOICE_SEQUENCE_INDEX.put(key, (index + 1) % ids.size());
                yield ids.get(Math.floorMod(index, ids.size()));
            }
            case SPECIFIC_CLIP -> ids.getFirst();
        };
    }

    private static boolean playDataPackVoice(ServerPlayer player, EntityMaid maid) {
        return MorningKissProfileData.selectVoice(player.getRandom())
                .map(voice -> {
                    TouhouMaidAffection.LOGGER.info("Sending morning kiss data-pack voice '{}' ({} bytes) to {}",
                            voice.fileName(), voice.data().length, player.getGameProfile().getName());
                    PacketDistributor.sendToPlayer(player, new MorningKissDataVoicePlayPayload(
                            maid.getId(),
                            maid.getUUID(),
                            voice.fileName(),
                            voice.data()
                    ));
                    return true;
                })
                .orElse(false);
    }

    private static boolean playUnifiedDataPackVoice(ServerPlayer player, EntityMaid maid,
                                                    InteractionVoiceProfileData.ResolvedVoiceProfile profile) {
        return InteractionVoiceProfileData.selectVoice(profile, player.getRandom())
                .map(voice -> {
                    TouhouMaidAffection.LOGGER.info("Sending unified morning kiss data-pack voice '{}' ({} bytes) to {}",
                            voice.fileName(), voice.data().length, player.getGameProfile().getName());
                    PacketDistributor.sendToPlayer(player, new MorningKissDataVoicePlayPayload(
                            maid.getId(),
                            maid.getUUID(),
                            voice.fileName(),
                            voice.data()
                    ));
                    return true;
                })
                .orElse(false);
    }

    private static void showMorningKissMessage(ServerPlayer player, Component message) {
        if (isChatMode()) {
            player.sendSystemMessage(message);
            return;
        }
        player.displayClientMessage(message, true);
    }

    private static void showMorningKissDialogue(ServerPlayer player, EntityMaid maid, Component message) {
        if (!ModConfig.BOND_MORNING_KISS_DIALOGUE_CHAT_BUBBLE_ENABLED.get()) {
            showMorningKissMessage(player, message);
            return;
        }
        try {
            long key = maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(message));
            if (key >= 0L) {
                return;
            }
        } catch (Throwable throwable) {
            TouhouMaidAffection.LOGGER.warn("Failed to show morning kiss dialogue as TLM chat bubble.", throwable);
        }
        showMorningKissMessage(player, message);
    }

    private static Component startMessage(String actionBarKey, String chatKey, EntityMaid maid) {
        boolean chatMode = isChatMode();
        return Component.translatable(chatMode ? chatKey : actionBarKey, getResolvedNameForMode(maid, chatMode));
    }

    private static Component getResolvedNameForMode(EntityMaid maid, boolean chatMode) {
        return chatMode ? MaidDisplayNameResolver.resolveChatSafeDisplayName(maid) : MaidDisplayNameResolver.resolveDisplayName(maid);
    }

    private static boolean isChatMode() {
        String mode = ModConfig.BOND_MORNING_KISS_MESSAGE_DISPLAY_MODE.get();
        return "chat".equalsIgnoreCase(mode);
    }

    private record PendingMorningKiss(
            ResourceKey<Level> dimension,
            UUID playerUuid,
            UUID maidUuid,
            long timeoutTick,
            int totalKisses,
            int completedKisses,
            long nextKissTick,
            boolean blessingApplied,
            boolean dialogueShown,
            TriggerSource triggerSource,
            String autoWindowId,
            DialoguePool dialoguePool
    ) {
        private PendingMorningKiss withProgress(int completedKisses, long nextKissTick) {
            return new PendingMorningKiss(dimension, playerUuid, maidUuid, timeoutTick, totalKisses, completedKisses, nextKissTick, blessingApplied, dialogueShown, triggerSource, autoWindowId, dialoguePool);
        }

        private PendingMorningKiss withBlessingApplied(boolean blessingApplied) {
            return new PendingMorningKiss(dimension, playerUuid, maidUuid, timeoutTick, totalKisses, completedKisses, nextKissTick, blessingApplied, dialogueShown, triggerSource, autoWindowId, dialoguePool);
        }

        private PendingMorningKiss withDialogueShown(boolean dialogueShown) {
            return new PendingMorningKiss(dimension, playerUuid, maidUuid, timeoutTick, totalKisses, completedKisses, nextKissTick, blessingApplied, dialogueShown, triggerSource, autoWindowId, dialoguePool);
        }
    }

    private record ActiveTimeWindow(int index, String windowId, long windowStartTick, long windowEndTick, MorningKissScheduleRules.TimeRange range) {
    }

    private record ResolvedMorningKissVoice(String selectedId, InteractionVoiceProfileData.DataPackVoice dataPackVoice) {
        private static ResolvedMorningKissVoice builtin() {
            return new ResolvedMorningKissVoice(VoicePoolIds.BUILTIN_MORNING_KISS, null);
        }

        private static ResolvedMorningKissVoice dataPack(String selectedId, InteractionVoiceProfileData.DataPackVoice voice) {
            return new ResolvedMorningKissVoice(selectedId, voice);
        }

        private static ResolvedMorningKissVoice tlm(String selectedId) {
            return new ResolvedMorningKissVoice(selectedId, null);
        }

        private boolean isBuiltin() {
            return VoicePoolIds.BUILTIN_MORNING_KISS.equals(selectedId);
        }
    }

    private enum TriggerSource {
        MANUAL,
        AUTO_WINDOW
    }

    private enum GeneratedDialogueResult {
        MISSING,
        TEXT_ONLY,
        VOICE_PLAYED
    }

}
