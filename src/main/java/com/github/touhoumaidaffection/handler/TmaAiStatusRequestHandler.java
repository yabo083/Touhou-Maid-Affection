package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.world.backups.MaidBackupsManager;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.service.MorningKissGeneratedDialogueService;
import com.github.touhoumaidaffection.bond.service.MorningKissScheduleRules;
import com.github.touhoumaidaffection.bond.settings.TmaAiStatusWire;
import com.github.touhoumaidaffection.network.TmaAiCacheClearPayload;
import com.github.touhoumaidaffection.network.TmaAiStatusPayload;
import com.github.touhoumaidaffection.network.TmaAiStatusRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the read-only Morning Kiss AI status channel.
 *
 * <p>Permission model: any player may read the status, but the maid list is restricted to the maids
 * that player owns (resolved through Touhou Little Maid's per-owner maid backup index). Clearing the
 * cache requires operator permission level {@value #REQUIRED_PERMISSION_LEVEL} and is audited, and it
 * reuses exactly the same {@link MorningKissGeneratedDialogueService} methods as the
 * {@code /tma morning_kiss clear_ai_cache} command.
 */
public final class TmaAiStatusRequestHandler {
    /** Matches the vanilla operator level used by the settings channel and the rescue commands. */
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private TmaAiStatusRequestHandler() {
    }

    public static void handleStatusRequest(TmaAiStatusRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendStatus(player);
            }
        });
    }

    public static void handleClear(TmaAiCacheClearPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.hasPermissions(REQUIRED_PERMISSION_LEVEL)) {
                TouhouMaidAffection.LOGGER.warn(
                        "[TMA AI Status] Rejected cache clear from player={} scope={} (requires permission level {})",
                        player.getScoreboardName(),
                        payload.request().scope(),
                        REQUIRED_PERMISSION_LEVEL
                );
                return;
            }
            clear(player, payload.request());
            // Push a fresh status so every open status tab converges without an extra round trip.
            sendStatus(player);
        });
    }

    private static void clear(ServerPlayer player, TmaAiStatusWire.ClearRequest request) {
        String scope = request.scope().name();
        String maidUuid = request.maidUuid();
        String pool = request.pool();
        int removed;
        UUID parsedMaid = parseUuid(maidUuid);
        switch (request.scope()) {
            case MAID -> removed = parsedMaid == null ? 0 : MorningKissGeneratedDialogueService.clearCache(parsedMaid);
            case POOL -> {
                MorningKissScheduleRules.DialoguePool parsedPool = parsePool(pool);
                removed = parsedMaid == null || parsedPool == null
                        ? 0
                        : MorningKissGeneratedDialogueService.clearCache(parsedMaid, parsedPool);
            }
            default -> removed = MorningKissGeneratedDialogueService.clearCache();
        }
        TouhouMaidAffection.LOGGER.info(
                "[TMA AI Status] player={} cleared morning kiss AI cache scope={} maid={} pool={} removed={}",
                player.getScoreboardName(),
                scope,
                maidUuid,
                pool,
                removed
        );
    }

    private static void sendStatus(ServerPlayer player) {
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TmaAiStatusPayload(buildStatus(player)));
    }

    private static TmaAiStatusWire.Status buildStatus(ServerPlayer player) {
        MorningKissGeneratedDialogueService.CacheStats stats = MorningKissGeneratedDialogueService.cacheStats();

        Map<UUID, String> ownedNames = new LinkedHashMap<>();
        try {
            MaidBackupsManager.getMaidIndexMap(player).forEach((maidUuid, data) ->
                    ownedNames.put(maidUuid, data.name() == null ? "" : data.name().getString()));
        } catch (RuntimeException ex) {
            // The backup index is best-effort: an unreadable index must not break the status tab.
            TouhouMaidAffection.LOGGER.warn("[TMA AI Status] Failed to read maid index for player={}",
                    player.getScoreboardName(), ex);
        }

        Map<UUID, MorningKissGeneratedDialogueService.MaidCacheStats> cacheByMaid = new LinkedHashMap<>();
        for (MorningKissGeneratedDialogueService.MaidCacheStats maid : stats.maids()) {
            cacheByMaid.put(maid.maidUuid(), maid);
        }

        int target = Math.max(1, ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL.get());
        List<TmaAiStatusWire.MaidStatus> maids = new ArrayList<>();
        for (Map.Entry<UUID, String> owned : ownedNames.entrySet()) {
            MorningKissGeneratedDialogueService.MaidCacheStats cache = cacheByMaid.get(owned.getKey());
            String name = owned.getValue();
            if (name == null || name.isBlank()) {
                name = cache == null ? "" : cache.maidLabel();
            }
            maids.add(new TmaAiStatusWire.MaidStatus(
                    owned.getKey().toString(),
                    name == null ? "" : name,
                    cache == null ? 0 : cache.totalEntries(),
                    target,
                    poolsOf(cache)
            ));
        }
        maids.sort(Comparator.comparing(TmaAiStatusWire.MaidStatus::name, String.CASE_INSENSITIVE_ORDER));

        return new TmaAiStatusWire.Status(
                ModConfig.BOND_MORNING_KISS_ENABLED.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_IMMEDIATE_FALLBACK_ENABLED.get(),
                languageOrAuto(MorningKissGeneratedDialogueService.globalDisplayLanguage()),
                languageOrAuto(MorningKissGeneratedDialogueService.globalVoiceLanguage()),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_VOICE_LANGUAGE.get(),
                target,
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_SCAN_INTERVAL_TICKS.get(),
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_CONSUME_ON_USE.get(),
                stats.totalEntries(),
                stats.voiceEntries(),
                stats.maidCount(),
                stats.inFlightRequests(),
                stats.revision(),
                player.hasPermissions(REQUIRED_PERMISSION_LEVEL),
                maids
        );
    }

    /** Always emits one entry per dialogue pool (0 when the maid has nothing cached for it). */
    private static List<TmaAiStatusWire.PoolStatus> poolsOf(MorningKissGeneratedDialogueService.MaidCacheStats cache) {
        Map<String, MorningKissGeneratedDialogueService.PoolCacheStats> byName = new LinkedHashMap<>();
        if (cache != null) {
            for (MorningKissGeneratedDialogueService.PoolCacheStats pool : cache.pools()) {
                byName.put(pool.pool(), pool);
            }
        }
        List<TmaAiStatusWire.PoolStatus> pools = new ArrayList<>();
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            String name = pool.name().toLowerCase(Locale.ROOT);
            MorningKissGeneratedDialogueService.PoolCacheStats stats = byName.get(name);
            pools.add(new TmaAiStatusWire.PoolStatus(
                    name,
                    stats == null ? 0 : stats.totalEntries(),
                    stats == null ? 0 : stats.voiceEntries()
            ));
        }
        return List.copyOf(pools);
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static MorningKissScheduleRules.DialoguePool parsePool(String raw) {
        if (raw == null) {
            return null;
        }
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            if (pool.name().equalsIgnoreCase(raw)) {
                return pool;
            }
        }
        return null;
    }

    private static String languageOrAuto(String normalized) {
        return normalized == null || normalized.isBlank() ? "auto" : normalized;
    }
}