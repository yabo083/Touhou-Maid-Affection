package com.github.touhoumaidaffection.command;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueData;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueService;
import com.github.touhoumaidaffection.bond.service.MorningKissGeneratedDialogueService;
import com.github.touhoumaidaffection.bond.service.MorningKissScheduleRules;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Locale;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class RescueCommand {
    private RescueCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tma")
                .then(Commands.literal("rescue")
                        .executes(context -> executeSelfQuery(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setEnabled(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setEnabled(context.getSource(), false)))
                        .then(Commands.literal("toggle")
                                .executes(context -> toggleEnabled(context.getSource())))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> clearPoolAndResetUnlock(context.getSource())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> clearPoolAndResetUnlock(context.getSource()))))
                .then(Commands.literal("morning_kiss")
                        .executes(context -> showMorningKissStatus(context.getSource()))
                        .then(Commands.literal("status")
                                .executes(context -> showMorningKissStatus(context.getSource())))
                        .then(Commands.literal("ai")
                                .then(Commands.literal("on")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> setMorningKissAi(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> setMorningKissAi(context.getSource(), false))))
                        .then(Commands.literal("tts")
                                .then(Commands.literal("on")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> setMorningKissTts(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> setMorningKissTts(context.getSource(), false))))
                        .then(Commands.literal("cache")
                                .executes(context -> showMorningKissCache(context.getSource())))
                        .then(Commands.literal("clear_ai_cache")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> clearMorningKissAiCache(context.getSource()))
                                .then(Commands.literal("all")
                                        .executes(context -> clearMorningKissAiCache(context.getSource())))
                                .then(Commands.literal("maid")
                                        .then(Commands.argument("maid_uuid", StringArgumentType.word())
                                                .executes(context -> clearMorningKissAiCacheForMaid(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "maid_uuid")))))
                                .then(Commands.literal("pool")
                                        .then(Commands.argument("maid_uuid", StringArgumentType.word())
                                                .then(Commands.argument("pool", StringArgumentType.word())
                                                        .executes(context -> clearMorningKissAiCacheForPool(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "maid_uuid"),
                                                                StringArgumentType.getString(context, "pool"))))))
                                .then(Commands.literal("entry")
                                        .then(Commands.argument("maid_uuid", StringArgumentType.word())
                                                .then(Commands.argument("pool", StringArgumentType.word())
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .executes(context -> removeMorningKissAiCacheEntry(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "maid_uuid"),
                                                                        StringArgumentType.getString(context, "pool"),
                                                                        IntegerArgumentType.getInteger(context, "index")))))))
                                .then(Commands.literal("voice")
                                        .then(Commands.argument("maid_uuid", StringArgumentType.word())
                                                .then(Commands.argument("pool", StringArgumentType.word())
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .executes(context -> clearMorningKissAiCacheEntryVoice(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "maid_uuid"),
                                                                        StringArgumentType.getString(context, "pool"),
                                                                        IntegerArgumentType.getInteger(context, "index"))))))))));
    }

    private static int executeSelfQuery(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmergencyRescueService.refreshChargesIfNeeded(player);

        int current = EmergencyRescueData.getChargeCount(player);
        int max = EmergencyRescueData.getMaxChargeCount(player);
        long currentDay = EmergencyRescueService.getCurrentRescueDay(player);
        long lastReplenishDay = EmergencyRescueData.getLastReplenishDay(player);
        boolean refreshedToday = currentDay <= lastReplenishDay;
        boolean globalEnabled = ModConfig.BOND_EMERGENCY_RESCUE_ENABLED.get();
        boolean playerEnabled = EmergencyRescueData.isRescueEnabled(player);
        boolean effectiveEnabled = globalEnabled && playerEnabled;

        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.header"), false);
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.charges", current, max), false);
        source.sendSuccess(() -> Component.translatable(
                globalEnabled
                        ? "command.touhou_maid_affection.rescue.global.on"
                        : "command.touhou_maid_affection.rescue.global.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                playerEnabled
                        ? "command.touhou_maid_affection.rescue.personal.on"
                        : "command.touhou_maid_affection.rescue.personal.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                effectiveEnabled
                        ? "command.touhou_maid_affection.rescue.effective.on"
                        : "command.touhou_maid_affection.rescue.effective.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                refreshedToday
                        ? "command.touhou_maid_affection.rescue.refresh.tomorrow"
                        : "command.touhou_maid_affection.rescue.refresh.pending"
        ), false);
        return current;
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmergencyRescueData.setRescueEnabled(player, enabled);
        source.sendSuccess(() -> Component.translatable(
                enabled
                        ? "command.touhou_maid_affection.rescue.personal.set_on"
                        : "command.touhou_maid_affection.rescue.personal.set_off"
        ), false);
        return enabled ? 1 : 0;
    }

    private static int toggleEnabled(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean next = !EmergencyRescueData.isRescueEnabled(player);
        EmergencyRescueData.setRescueEnabled(player, next);
        source.sendSuccess(() -> Component.translatable(
                next
                        ? "command.touhou_maid_affection.rescue.personal.set_on"
                        : "command.touhou_maid_affection.rescue.personal.set_off"
        ), false);
        return next ? 1 : 0;
    }

    private static int clearPoolAndResetUnlock(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BondData bondData = BondData.of(player);
        int resetCount = bondData.resetAbilityForAllMaids("emergency_heal");
        EmergencyRescueData.clearPoolAndRegistration(player);
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.rescue.clear.done", resetCount),
                true
        );
        return resetCount;
    }

    private static int clearMorningKissAiCache(CommandSourceStack source) {
        int removed = MorningKissGeneratedDialogueService.clearCache();
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.done", removed),
                true
        );
        return removed;
    }

    private static int clearMorningKissAiCacheForMaid(CommandSourceStack source, String maidUuidRaw) {
        UUID maidUuid = parseUuidOrReply(source, maidUuidRaw);
        if (maidUuid == null) {
            return 0;
        }
        int removed = MorningKissGeneratedDialogueService.clearCache(maidUuid);
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.maid.done", maidUuid.toString(), removed),
                true
        );
        return removed;
    }

    private static int clearMorningKissAiCacheForPool(CommandSourceStack source, String maidUuidRaw, String poolRaw) {
        UUID maidUuid = parseUuidOrReply(source, maidUuidRaw);
        MorningKissScheduleRules.DialoguePool pool = parsePoolOrReply(source, poolRaw);
        if (maidUuid == null || pool == null) {
            return 0;
        }
        int removed = MorningKissGeneratedDialogueService.clearCache(maidUuid, pool);
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.pool.done",
                        maidUuid.toString(), poolName(pool), removed),
                true
        );
        return removed;
    }

    private static int removeMorningKissAiCacheEntry(CommandSourceStack source, String maidUuidRaw, String poolRaw, int displayIndex) {
        UUID maidUuid = parseUuidOrReply(source, maidUuidRaw);
        MorningKissScheduleRules.DialoguePool pool = parsePoolOrReply(source, poolRaw);
        if (maidUuid == null || pool == null) {
            return 0;
        }
        int removed = MorningKissGeneratedDialogueService.removeCacheEntry(maidUuid, pool, displayIndex - 1);
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.entry.done",
                        maidUuid.toString(), poolName(pool), displayIndex, removed),
                true
        );
        return removed;
    }

    private static int clearMorningKissAiCacheEntryVoice(CommandSourceStack source, String maidUuidRaw, String poolRaw, int displayIndex) {
        UUID maidUuid = parseUuidOrReply(source, maidUuidRaw);
        MorningKissScheduleRules.DialoguePool pool = parsePoolOrReply(source, poolRaw);
        if (maidUuid == null || pool == null) {
            return 0;
        }
        boolean changed = MorningKissGeneratedDialogueService.clearCacheEntryVoice(maidUuid, pool, displayIndex - 1);
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.voice.done",
                        maidUuid.toString(), poolName(pool), displayIndex, changed ? 1 : 0),
                true
        );
        return changed ? 1 : 0;
    }

    private static int showMorningKissStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.morning_kiss.status.header"), false);
        source.sendSuccess(() -> Component.translatable(
                ModConfig.BOND_MORNING_KISS_ENABLED.get()
                        ? "command.touhou_maid_affection.morning_kiss.status.enabled.on"
                        : "command.touhou_maid_affection.morning_kiss.status.enabled.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.get()
                        ? "command.touhou_maid_affection.morning_kiss.status.ai.on"
                        : "command.touhou_maid_affection.morning_kiss.status.ai.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED.get()
                        ? "command.touhou_maid_affection.morning_kiss.status.tts.on"
                        : "command.touhou_maid_affection.morning_kiss.status.tts.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_IMMEDIATE_FALLBACK_ENABLED.get()
                        ? "command.touhou_maid_affection.morning_kiss.status.fallback.on"
                        : "command.touhou_maid_affection.morning_kiss.status.fallback.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                "command.touhou_maid_affection.morning_kiss.status.language",
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get()
        ), false);
        source.sendSuccess(() -> Component.translatable(
                "command.touhou_maid_affection.morning_kiss.status.cache_policy",
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_SCAN_INTERVAL_TICKS.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_CONSUME_ON_USE.get()
                        ? Component.translatable("command.touhou_maid_affection.morning_kiss.cache.consume.on")
                        : Component.translatable("command.touhou_maid_affection.morning_kiss.cache.consume.off")
        ), false);
        return ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.get() ? 1 : 0;
    }

    private static int setMorningKissAi(CommandSourceStack source, boolean enabled) {
        ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.set(enabled);
        ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PREGENERATE_ENABLED.set(enabled);
        ModConfig.SPEC.save();
        if (!enabled) {
            MorningKissGeneratedDialogueService.clearCache();
        }
        source.sendSuccess(() -> Component.translatable(
                enabled
                        ? "command.touhou_maid_affection.morning_kiss.ai.set_on"
                        : "command.touhou_maid_affection.morning_kiss.ai.set_off"
        ), true);
        return enabled ? 1 : 0;
    }

    private static int setMorningKissTts(CommandSourceStack source, boolean enabled) {
        ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED.set(enabled);
        ModConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable(
                enabled
                        ? "command.touhou_maid_affection.morning_kiss.tts.set_on"
                        : "command.touhou_maid_affection.morning_kiss.tts.set_off"
        ), true);
        return enabled ? 1 : 0;
    }

    private static int showMorningKissCache(CommandSourceStack source) {
        MorningKissGeneratedDialogueService.CacheStats stats = MorningKissGeneratedDialogueService.cacheStats();
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.morning_kiss.cache.header"), false);
        source.sendSuccess(() -> Component.translatable(
                "command.touhou_maid_affection.morning_kiss.cache.entries",
                stats.totalEntries(),
                stats.voiceEntries(),
                stats.textOnlyEntries()
        ), false);
        source.sendSuccess(() -> Component.translatable(
                "command.touhou_maid_affection.morning_kiss.cache.runtime",
                stats.maidCount(),
                stats.inFlightRequests(),
                stats.revision()
        ), false);
        for (MorningKissGeneratedDialogueService.MaidCacheStats maid : stats.maids()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.touhou_maid_affection.morning_kiss.cache.maid",
                    maid.maidLabel(),
                    maid.maidUuid().toString(),
                    maid.totalEntries(),
                    maid.voiceEntries(),
                    maid.textOnlyEntries()
            ), false);
            for (MorningKissGeneratedDialogueService.PoolCacheStats pool : maid.pools()) {
                source.sendSuccess(() -> Component.translatable(
                        "command.touhou_maid_affection.morning_kiss.cache.pool",
                        pool.pool(),
                        pool.textLanguage(),
                        pool.voiceLanguage(),
                        pool.totalEntries(),
                        pool.voiceEntries(),
                        pool.textOnlyEntries()
                ), false);
            }
        }
        for (MorningKissGeneratedDialogueService.InFlightStats pending : stats.inFlight()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.touhou_maid_affection.morning_kiss.cache.in_flight",
                    pending.maidLabel(),
                    pending.maidUuid().toString(),
                    pending.pool(),
                    pending.phase(),
                    pending.chatLanguage(),
                    pending.ttsLanguage(),
                    pending.pendingVoiceCallbacks()
            ), false);
        }
        return stats.totalEntries();
    }

    private static UUID parseUuidOrReply(CommandSourceStack source, String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.invalid_uuid", raw));
            return null;
        }
    }

    private static MorningKissScheduleRules.DialoguePool parsePoolOrReply(CommandSourceStack source, String raw) {
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            if (poolName(pool).equals(raw.toLowerCase(Locale.ROOT))) {
                return pool;
            }
        }
        source.sendFailure(Component.translatable("command.touhou_maid_affection.morning_kiss.ai_cache.clear.invalid_pool", raw));
        return null;
    }

    private static String poolName(MorningKissScheduleRules.DialoguePool pool) {
        return pool.name().toLowerCase(Locale.ROOT);
    }

}

