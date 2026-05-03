package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.TTSCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class MorningKissGeneratedDialogueService {
    private static final MorningKissGeneratedDialogueCache CACHE = new MorningKissGeneratedDialogueCache();
    private static final ConcurrentHashMap<RequestKey, InFlightRequest> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, AtomicLong> MAID_REVISIONS = new ConcurrentHashMap<>();
    private static final AtomicLong CACHE_REVISION = new AtomicLong();
    private static volatile Path worldRoot;

    private MorningKissGeneratedDialogueService() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        loadPersistedCache(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        savePersistedCache();
        worldRoot = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick(event.getServer());
    }

    static void tick(MinecraftServer server) {
        if (worldRoot == null) {
            loadPersistedCache(server);
        }
        if (!ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.get()
                || !ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PREGENERATE_ENABLED.get()
                || !AIConfig.LLM_ENABLED.get()) {
            return;
        }
        long gameTime = server.overworld().getGameTime();
        int scanInterval = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_SCAN_INTERVAL_TICKS.get();
        if (scanInterval > 1 && gameTime % scanInterval != 0L) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (MorningKissService.isAllowedTime(player.level())) {
                continue;
            }
            scanPlayer(player);
        }
    }

    static Optional<MorningKissGeneratedDialogueCache.Entry> pollCachedLine(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, RandomSource random) {
        Optional<MorningKissGeneratedDialogueCache.Entry> entry = selectCachedLine(maidUuid, pool, random);
        if (entry.isPresent()) {
            return entry;
        }
        if (pool != MorningKissScheduleRules.DialoguePool.GENERAL) {
            return selectCachedLine(maidUuid, MorningKissScheduleRules.DialoguePool.GENERAL, random);
        }
        return Optional.empty();
    }

    private static Optional<MorningKissGeneratedDialogueCache.Entry> selectCachedLine(UUID maidUuid,
                                                                                     MorningKissScheduleRules.DialoguePool pool,
                                                                                     RandomSource random) {
        if (ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_CONSUME_ON_USE.get()) {
            return CACHE.pollRandom(maidUuid, pool, random);
        }
        return CACHE.peekRandom(maidUuid, pool, random);
    }

    static boolean hasCachedLine(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
        return !CACHE.isEmpty(maidUuid, pool) || !CACHE.isEmpty(maidUuid, MorningKissScheduleRules.DialoguePool.GENERAL);
    }

    private static void scanPlayer(ServerPlayer player) {
        int scanDistance = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_SCAN_DISTANCE.get();
        List<EntityMaid> nearbyMaids = player.serverLevel().getEntitiesOfClass(
                EntityMaid.class,
                player.getBoundingBox().inflate(scanDistance),
                maid -> maid.isAlive()
                        && maid.isOwnedBy(player)
                        && BondManager.isAbilityUnlocked(player, maid.getUUID(), "morning_kiss")
                        && maid.getFavorabilityManager().getLevel() >= ModConfig.BOND_MORNING_KISS_REQUIRED_FAVORABILITY.get()
        );
        if (nearbyMaids.isEmpty()) {
            return;
        }
        for (EntityMaid maid : nearbyMaids) {
            if (!shouldWarmCache(player, maid)) {
                continue;
            }
            warmCache(player, maid);
        }
    }

    private static boolean shouldWarmCache(ServerPlayer player, EntityMaid maid) {
        if (maid == null || player == null || !maid.isAlive()) {
            return false;
        }
        if (maid.getSoundPackId() == null) {
            return true;
        }
        int target = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL.get();
        return CACHE.size(maid.getUUID(), MorningKissScheduleRules.DialoguePool.MORNING) < target
                || CACHE.size(maid.getUUID(), MorningKissScheduleRules.DialoguePool.EVENING) < target
                || CACHE.size(maid.getUUID(), MorningKissScheduleRules.DialoguePool.GENERAL) < target;
    }

    private static void warmCache(ServerPlayer player, EntityMaid maid) {
        LLMSite llmSite = maid.getAiChatManager().getLLMSite();
        if (llmSite == null || !llmSite.enabled()) {
            debug("Morning kiss AI dialogue warmup skipped for {}: maid has no enabled LLM site.", maid.getUUID());
            return;
        }
        RequestKey key = new RequestKey(player.getUUID(), maid.getUUID());
        String maidName = MaidDisplayNameResolver.resolveChatSafeDisplayName(maid).getString();
        InFlightRequest pending = new InFlightRequest(maid.getUUID(), maidName);
        if (IN_FLIGHT.putIfAbsent(key, pending) != null) {
            debug("Morning kiss AI dialogue warmup skipped for {}: request already in flight.", maid.getUUID());
            return;
        }
        LLMClient client = llmSite.client();
        if (client == null) {
            debug("Morning kiss AI dialogue warmup skipped for {}: LLM client is null.", maid.getUUID());
            IN_FLIGHT.remove(key);
            return;
        }
        int target = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL.get();
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            if (CACHE.size(maid.getUUID(), pool) >= target) {
                continue;
            }
            requestGeneration(player, maid, pool, client, key, pending, CACHE_REVISION.get(), maidRevision(maid.getUUID()));
            return;
        }
        IN_FLIGHT.remove(key);
    }

    private static void requestGeneration(ServerPlayer player, EntityMaid maid,
                                          MorningKissScheduleRules.DialoguePool pool, LLMClient client, RequestKey key,
                                          InFlightRequest inFlight, long cacheRevision, long maidRevision) {
        String ttsLanguage = resolveTtsLanguage(maid);
        boolean willWarmVoice = canWarmRemoteVoice(maid);
        String chatLanguage = resolvePregeneratedTextLanguage(maid, willWarmVoice);
        String prompt = buildPrompt(ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PROMPT.get(), player, maid, pool, chatLanguage);
        inFlight.startLlm(pool, chatLanguage, ttsLanguage);
        List<LLMMessage> messages = List.of(
                LLMMessage.systemChat(maid, MorningKissGeneratedDialogueLanguage.systemInstruction(chatLanguage)),
                LLMMessage.userChat(maid, prompt)
        );
        debug("Requesting morning kiss AI dialogue warmup for maid {} pool {}, chatLanguage={}, ttsLanguage={}.",
                maid.getUUID(), pool.name().toLowerCase(Locale.ROOT), displayLanguage(chatLanguage), displayLanguage(ttsLanguage));
        client.chat(new LLMCallback(maid.getAiChatManager(), new java.util.ArrayList<>(messages), false) {
            @Override
            public void onFailure(@Nullable java.net.http.HttpRequest request, Throwable throwable, int errorCode) {
                TouhouMaidAffection.LOGGER.warn("Morning kiss AI dialogue warmup failed for {} in pool {}: {}",
                        maid.getUUID(), pool.name().toLowerCase(Locale.ROOT), throwable.getMessage());
                IN_FLIGHT.remove(key);
            }

            @Override
            public void onSuccess(ResponseChat responseChat) {
                if (!isCurrentCacheRevision(cacheRevision, maid.getUUID(), maidRevision)) {
                    debug("Morning kiss AI dialogue warmup result discarded for {} pool {}: cache was cleared.",
                            maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                    IN_FLIGHT.remove(key);
                    return;
                }
                List<String> lines = MorningKissGeneratedDialogueCache.normalizeLines(responseChat.getChatText());
                if (lines.isEmpty()) {
                    String fallback = MorningKissGeneratedDialogueCache.normalizeLine(responseChat.getChatText());
                    if (!fallback.isBlank()) {
                        lines = List.of(fallback);
                    }
                }
                if (lines.isEmpty()) {
                    debug("Morning kiss AI dialogue warmup returned no usable lines for maid {} pool {}.",
                            maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                    IN_FLIGHT.remove(key);
                    return;
                }
                int target = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL.get();
                int currentSize = CACHE.size(maid.getUUID(), pool);
                lines = MorningKissGeneratedDialogueCache.limitToRemainingCapacity(lines, currentSize, target);
                if (lines.isEmpty()) {
                    debug("Morning kiss AI dialogue warmup discarded for maid {} pool {}: cache already reached target {}.",
                            maid.getUUID(), pool.name().toLowerCase(Locale.ROOT), target);
                    IN_FLIGHT.remove(key);
                    return;
                }
                if (willWarmVoice && tryWarmVoice(maid, pool, lines, chatLanguage, ttsLanguage, inFlight, key, cacheRevision, maidRevision)) {
                    return;
                }
                for (String line : lines) {
                    addIfCurrent(cacheRevision, maidRevision, maid.getUUID(), pool,
                            new MorningKissGeneratedDialogueCache.Entry(line, line, "", new byte[0],
                                    chatLanguage, "", inFlight.maidName()));
                }
                TouhouMaidAffection.LOGGER.info("Cached {} text-only morning kiss AI dialogue line(s) for maid {} pool {}.",
                        lines.size(), maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                IN_FLIGHT.remove(key);
            }
        });
    }

    private static boolean tryWarmVoice(EntityMaid maid, MorningKissScheduleRules.DialoguePool pool, List<String> lines,
                                        String chatLanguage, String ttsLanguage, InFlightRequest inFlight,
                                        RequestKey key, long cacheRevision, long maidRevision) {
        TTSSite ttsSite = maid == null || maid.getAiChatManager() == null ? null : maid.getAiChatManager().getTTSSite();
        TTSClient ttsClient = ttsSite == null ? null : ttsSite.client();
        if (!canWarmRemoteVoice(maid) || ttsClient == null) {
            return false;
        }
        if (lines.isEmpty()) {
            return false;
        }
        TTSConfig config = new TTSConfig(maid.getAiChatManager().getTTSModel(), ttsLanguage);
        inFlight.startTts(lines.size());
        for (String line : lines) {
            ttsClient.play(line, config, new TTSCallback(maid, line, -1L) {
                @Override
                public void onFailure(@Nullable java.net.http.HttpRequest request, Throwable throwable, int errorCode) {
                    TouhouMaidAffection.LOGGER.warn("Morning kiss AI dialogue TTS failed for maid {} pool {}: {}",
                            maid.getUUID(), pool.name().toLowerCase(Locale.ROOT), throwable.getMessage());
                    addIfCurrent(cacheRevision, maidRevision, maid.getUUID(), pool,
                            new MorningKissGeneratedDialogueCache.Entry(line, line, "", new byte[0],
                                    chatLanguage, "", inFlight.maidName()));
                    completeVoiceRequest(key, inFlight);
                }

                @Override
                public void onSuccess(byte[] data) {
                    String extension = MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(data).orElse("");
                    if (extension.isBlank()) {
                        TouhouMaidAffection.LOGGER.warn("Morning kiss AI dialogue TTS for maid {} pool {} did not return playable audio data; cached text only.",
                                maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                        addIfCurrent(cacheRevision, maidRevision, maid.getUUID(), pool,
                                new MorningKissGeneratedDialogueCache.Entry(line, line, "", new byte[0],
                                        chatLanguage, "", inFlight.maidName()));
                        completeVoiceRequest(key, inFlight);
                        return;
                    }
                    String fileName = "generated/" + maid.getUUID() + "/" + pool.name().toLowerCase(Locale.ROOT) + "/" + Integer.toHexString(line.hashCode()) + "." + extension;
                    if (addIfCurrent(cacheRevision, maidRevision, maid.getUUID(), pool,
                            new MorningKissGeneratedDialogueCache.Entry(line, line, fileName, data,
                                    chatLanguage, ttsLanguage, inFlight.maidName()))) {
                        TouhouMaidAffection.LOGGER.info("Cached morning kiss AI dialogue voice '{}' ({} bytes) for maid {} pool {}, chatLanguage={}, ttsLanguage={}.",
                                fileName, data.length, maid.getUUID(), pool.name().toLowerCase(Locale.ROOT),
                                displayLanguage(chatLanguage), displayLanguage(ttsLanguage));
                    }
                    completeVoiceRequest(key, inFlight);
                }
            });
        }
        return true;
    }

    private static void completeVoiceRequest(RequestKey key, InFlightRequest inFlight) {
        if (inFlight.completeOneVoice()) {
            IN_FLIGHT.remove(key, inFlight);
        }
    }

    private static boolean addIfCurrent(long cacheRevision, long maidRevision, UUID maidUuid, MorningKissScheduleRules.DialoguePool pool,
                                        MorningKissGeneratedDialogueCache.Entry entry) {
        if (!isCurrentCacheRevision(cacheRevision, maidUuid, maidRevision)) {
            return false;
        }
        int target = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_CACHE_TARGET_PER_POOL.get();
        boolean added = CACHE.addIfBelowTarget(maidUuid, pool, entry, target);
        if (added) {
            savePersistedCache();
        }
        return added;
    }

    private static boolean isCurrentCacheRevision(long cacheRevision, UUID maidUuid, long maidRevision) {
        return CACHE_REVISION.get() == cacheRevision && maidRevision(maidUuid) == maidRevision;
    }

    private static long maidRevision(UUID maidUuid) {
        if (maidUuid == null) {
            return 0L;
        }
        AtomicLong revision = MAID_REVISIONS.get(maidUuid);
        return revision == null ? 0L : revision.get();
    }

    private static void invalidateMaid(UUID maidUuid) {
        if (maidUuid != null) {
            MAID_REVISIONS.computeIfAbsent(maidUuid, ignored -> new AtomicLong()).incrementAndGet();
        }
    }

    private static boolean canWarmRemoteVoice(EntityMaid maid) {
        if (!ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED.get()) {
            return false;
        }
        if (!AIConfig.TTS_ENABLED.get() || maid == null || maid.getAiChatManager() == null) {
            return false;
        }
        TTSSite ttsSite = maid.getAiChatManager().getTTSSite();
        if (ttsSite == null || !ttsSite.enabled()) {
            debug("Morning kiss AI dialogue TTS skipped for {}: TTS is disabled or not configured.",
                    maid == null ? "unknown" : maid.getUUID());
            return false;
        }
        TTSClient ttsClient = ttsSite.client();
        if (ttsClient == null || ttsClient instanceof TTSSystemServices) {
            debug("Morning kiss AI dialogue TTS skipped for {}: no remote TTS client.", maid.getUUID());
            return false;
        }
        return true;
    }

    private static String buildPrompt(String rawPrompt, ServerPlayer player, EntityMaid maid,
                                      MorningKissScheduleRules.DialoguePool pool, String targetLanguage) {
        String maidName = MaidDisplayNameResolver.resolveChatSafeDisplayName(maid).getString();
        String playerName = player.getName().getString();
        String prompt = rawPrompt == null ? "" : rawPrompt;
        prompt = prompt.replace("{maid}", maidName)
                .replace("{player}", playerName)
                .replace("{pool}", pool.name().toLowerCase(Locale.ROOT))
                .replace("{time}", MorningKissService.getAllowedTimeRangesText());
        return MorningKissGeneratedDialogueLanguage.appendLanguageInstruction(
                prompt,
                targetLanguage
        );
    }

    static String resolvePregeneratedTextLanguage(EntityMaid maid, boolean forGeneratedVoice) {
        String tlmTtsLanguage = maid == null || maid.getAiChatManager() == null ? "" : maid.getAiChatManager().getTTSLanguage();
        String tlmChatLanguage = maid == null || maid.getAiChatManager() == null ? "" : maid.getAiChatManager().getChatLanguage();
        if (forGeneratedVoice) {
            return MorningKissGeneratedDialogueLanguage.resolveGeneratedVoiceTextLanguage(
                    ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get(),
                    tlmTtsLanguage,
                    tlmChatLanguage
            );
        }
        return MorningKissGeneratedDialogueLanguage.resolveGeneratedTextLanguage(
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get(),
                tlmTtsLanguage,
                tlmChatLanguage
        );
    }

    static String resolveChatLanguage(EntityMaid maid) {
        return resolvePregeneratedTextLanguage(maid, false);
    }

    static String resolveTtsLanguage(EntityMaid maid) {
        String configured = MorningKissGeneratedDialogueLanguage.normalizeLanguageCodeForTts(
                ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get());
        if (!configured.isBlank()) {
            return configured;
        }
        return maid == null || maid.getAiChatManager() == null ? "" : maid.getAiChatManager().getTTSLanguage();
    }

    private static String displayLanguage(String language) {
        return language == null || language.isBlank() ? "tlm/default" : language;
    }

    private static void debug(String message, Object... args) {
        if (ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_VERBOSE_LOG.get()) {
            TouhouMaidAffection.LOGGER.debug(message, args);
        }
    }

    static Optional<MorningKissGeneratedDialogueCache.Entry> pollCachedLine(EntityMaid maid, MorningKissScheduleRules.DialoguePool pool, RandomSource random) {
        return pollCachedLine(maid.getUUID(), pool, random);
    }

    static boolean hasCachedLine(EntityMaid maid, MorningKissScheduleRules.DialoguePool pool) {
        return hasCachedLine(maid.getUUID(), pool);
    }

    public static int clearCache() {
        CACHE_REVISION.incrementAndGet();
        int removed = CACHE.clearAll();
        IN_FLIGHT.clear();
        savePersistedCache();
        return removed;
    }

    public static int clearCache(UUID maidUuid) {
        invalidateMaid(maidUuid);
        int removed = CACHE.clear(maidUuid);
        IN_FLIGHT.keySet().removeIf(key -> maidUuid != null && maidUuid.equals(key.maidUuid()));
        savePersistedCache();
        return removed;
    }

    public static int clearCache(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
        invalidateMaid(maidUuid);
        int removed = CACHE.clear(maidUuid, pool);
        IN_FLIGHT.keySet().removeIf(key -> maidUuid != null && maidUuid.equals(key.maidUuid()));
        savePersistedCache();
        return removed;
    }

    public static int removeCacheEntry(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, int zeroBasedIndex) {
        int removed = CACHE.removeAt(maidUuid, pool, zeroBasedIndex);
        if (removed > 0) {
            savePersistedCache();
        }
        return removed;
    }

    public static boolean clearCacheEntryVoice(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, int zeroBasedIndex) {
        boolean changed = CACHE.clearVoiceAt(maidUuid, pool, zeroBasedIndex);
        if (changed) {
            savePersistedCache();
        }
        return changed;
    }

    public static CacheStats cacheStats() {
        MorningKissGeneratedDialogueCache.Stats stats = CACHE.stats(CACHE_REVISION.get(), IN_FLIGHT.size());
        List<MaidCacheStats> maids = stats.maids().stream()
                .map(maid -> new MaidCacheStats(
                        maid.maidUuid(),
                        maid.label(),
                        maid.totalEntries(),
                        maid.voiceEntries(),
                        maid.pools().stream()
                                .map(pool -> new PoolCacheStats(
                                        pool.pool().name().toLowerCase(Locale.ROOT),
                                        displayLanguage(pool.textLanguage()),
                                        displayLanguage(pool.voiceLanguage()),
                                        pool.totalEntries(),
                                        pool.voiceEntries()
                                ))
                                .toList()
                ))
                .toList();
        List<InFlightStats> inFlight = IN_FLIGHT.values().stream()
                .map(request -> new InFlightStats(
                        request.maidUuid(),
                        request.maidName(),
                        request.poolName(),
                        displayLanguage(request.chatLanguage()),
                        displayLanguage(request.ttsLanguage()),
                        request.phase(),
                        request.pendingVoiceCallbacks()
                ))
                .sorted((left, right) -> left.maidLabel().compareToIgnoreCase(right.maidLabel()))
                .toList();
        return new CacheStats(stats.totalEntries(), stats.maidCount(), stats.voiceEntries(), stats.revision(), stats.inFlightRequests(), maids, inFlight);
    }

    private static void loadPersistedCache(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Path root = server.getWorldPath(LevelResource.ROOT);
        worldRoot = root;
        try {
            CACHE.replaceAll(MorningKissGeneratedDialogueStorage.load(root));
            CACHE_REVISION.incrementAndGet();
            TouhouMaidAffection.LOGGER.info("Loaded persisted morning kiss AI dialogue cache from {}.",
                    MorningKissGeneratedDialogueStorage.storageRoot(root));
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to load persisted morning kiss AI dialogue cache from {}.",
                    MorningKissGeneratedDialogueStorage.storageRoot(root), ex);
        }
    }

    private static void savePersistedCache() {
        Path root = worldRoot;
        if (root == null) {
            return;
        }
        try {
            MorningKissGeneratedDialogueStorage.save(root, CACHE.snapshot());
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to save persisted morning kiss AI dialogue cache to {}.",
                    MorningKissGeneratedDialogueStorage.storageRoot(root), ex);
        }
    }

    private record RequestKey(UUID playerUuid, UUID maidUuid) {
    }

    private static final class InFlightRequest {
        private final UUID maidUuid;
        private final String maidName;
        private final AtomicInteger pendingVoiceCallbacks = new AtomicInteger();
        private volatile String poolName = "";
        private volatile String chatLanguage = "";
        private volatile String ttsLanguage = "";
        private volatile String phase = "llm";

        private InFlightRequest(UUID maidUuid, String maidName) {
            this.maidUuid = maidUuid;
            this.maidName = maidName == null || maidName.isBlank() ? maidUuid.toString() : maidName;
        }

        private void startLlm(MorningKissScheduleRules.DialoguePool pool, String chatLanguage, String ttsLanguage) {
            this.poolName = pool == null ? "" : pool.name().toLowerCase(Locale.ROOT);
            this.chatLanguage = chatLanguage == null ? "" : chatLanguage;
            this.ttsLanguage = ttsLanguage == null ? "" : ttsLanguage;
            this.phase = "llm";
        }

        private void startTts(int callbacks) {
            pendingVoiceCallbacks.set(Math.max(0, callbacks));
            phase = "tts";
        }

        private boolean completeOneVoice() {
            return pendingVoiceCallbacks.decrementAndGet() <= 0;
        }

        private UUID maidUuid() {
            return maidUuid;
        }

        private String maidName() {
            return maidName;
        }

        private String poolName() {
            return poolName;
        }

        private String chatLanguage() {
            return chatLanguage;
        }

        private String ttsLanguage() {
            return ttsLanguage;
        }

        private String phase() {
            return phase;
        }

        private int pendingVoiceCallbacks() {
            return Math.max(0, pendingVoiceCallbacks.get());
        }
    }

    public record CacheStats(int totalEntries, int maidCount, int voiceEntries, long revision, int inFlightRequests,
                             List<MaidCacheStats> maids, List<InFlightStats> inFlight) {
        public int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    public record MaidCacheStats(UUID maidUuid, String maidLabel, int totalEntries, int voiceEntries,
                                 List<PoolCacheStats> pools) {
        public int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    public record PoolCacheStats(String pool, String textLanguage, String voiceLanguage, int totalEntries,
                                 int voiceEntries) {
        public int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    public record InFlightStats(UUID maidUuid, String maidLabel, String pool, String chatLanguage, String ttsLanguage,
                                String phase, int pendingVoiceCallbacks) {
    }
}
