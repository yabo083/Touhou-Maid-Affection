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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class MorningKissGeneratedDialogueService {
    private static final MorningKissGeneratedDialogueCache CACHE = new MorningKissGeneratedDialogueCache();
    private static final Set<RequestKey> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private MorningKissGeneratedDialogueService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick(event.getServer());
    }

    static void tick(MinecraftServer server) {
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
        Optional<MorningKissGeneratedDialogueCache.Entry> entry = CACHE.pollRandom(maidUuid, pool, random);
        if (entry.isPresent()) {
            return entry;
        }
        if (pool != MorningKissScheduleRules.DialoguePool.GENERAL) {
            return CACHE.pollRandom(maidUuid, MorningKissScheduleRules.DialoguePool.GENERAL, random);
        }
        return Optional.empty();
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
        if (!IN_FLIGHT.add(key)) {
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
            requestGeneration(player, maid, pool, client, key);
            return;
        }
        IN_FLIGHT.remove(key);
    }

    private static void requestGeneration(ServerPlayer player, EntityMaid maid,
                                          MorningKissScheduleRules.DialoguePool pool, LLMClient client, RequestKey key) {
        String prompt = buildPrompt(ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PROMPT.get(), player, maid, pool);
        List<LLMMessage> messages = List.of(
                LLMMessage.systemChat(maid, "你正在生成 Minecraft 早安吻台词缓存。"),
                LLMMessage.userChat(maid, prompt)
        );
        debug("Requesting morning kiss AI dialogue warmup for maid {} pool {}.", maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
        client.chat(new LLMCallback(maid.getAiChatManager(), new java.util.ArrayList<>(messages), false) {
            @Override
            public void onFailure(@Nullable java.net.http.HttpRequest request, Throwable throwable, int errorCode) {
                TouhouMaidAffection.LOGGER.warn("Morning kiss AI dialogue warmup failed for {} in pool {}: {}",
                        maid.getUUID(), pool.name().toLowerCase(Locale.ROOT), throwable.getMessage());
                IN_FLIGHT.remove(key);
            }

            @Override
            public void onSuccess(ResponseChat responseChat) {
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
                if (tryWarmVoice(maid, pool, lines)) {
                    IN_FLIGHT.remove(key);
                    return;
                }
                for (String line : lines) {
                    CACHE.add(maid.getUUID(), pool, new MorningKissGeneratedDialogueCache.Entry(line, line, "", new byte[0]));
                }
                TouhouMaidAffection.LOGGER.info("Cached {} text-only morning kiss AI dialogue line(s) for maid {} pool {}.",
                        lines.size(), maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                IN_FLIGHT.remove(key);
            }
        });
    }

    private static boolean tryWarmVoice(EntityMaid maid, MorningKissScheduleRules.DialoguePool pool, List<String> lines) {
        if (!ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED.get()) {
            return false;
        }
        TTSSite ttsSite = maid.getAiChatManager().getTTSSite();
        if (!AIConfig.TTS_ENABLED.get() || ttsSite == null || !ttsSite.enabled()) {
            debug("Morning kiss AI dialogue TTS skipped for {}: TTS is disabled or not configured.", maid.getUUID());
            return false;
        }
        TTSClient ttsClient = ttsSite.client();
        if (ttsClient == null || ttsClient instanceof TTSSystemServices) {
            debug("Morning kiss AI dialogue TTS skipped for {}: no remote TTS client.", maid.getUUID());
            return false;
        }
        if (lines.isEmpty()) {
            return false;
        }
        String language = maid.getAiChatManager().getTTSLanguage();
        String ttsLanguage = language;
        int underscore = ttsLanguage.indexOf('_');
        if (underscore > 0) {
            ttsLanguage = ttsLanguage.substring(0, underscore);
        }
        TTSConfig config = new TTSConfig(maid.getAiChatManager().getTTSModel(), ttsLanguage);
        for (String line : lines) {
            ttsClient.play(line, config, new TTSCallback(maid, line, -1L) {
                @Override
                public void onFailure(@Nullable java.net.http.HttpRequest request, Throwable throwable, int errorCode) {
                    TouhouMaidAffection.LOGGER.warn("Morning kiss AI dialogue TTS failed for maid {} pool {}: {}",
                            maid.getUUID(), pool.name().toLowerCase(Locale.ROOT), throwable.getMessage());
                    CACHE.add(maid.getUUID(), pool, new MorningKissGeneratedDialogueCache.Entry(line, line, "", new byte[0]));
                }

                @Override
                public void onSuccess(byte[] data) {
                    String extension = MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(data).orElse("");
                    if (extension.isBlank()) {
                        TouhouMaidAffection.LOGGER.warn("Morning kiss AI dialogue TTS for maid {} pool {} did not return playable audio data; cached text only.",
                                maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                        CACHE.add(maid.getUUID(), pool, new MorningKissGeneratedDialogueCache.Entry(line, line, "", new byte[0]));
                        return;
                    }
                    String fileName = "generated/" + maid.getUUID() + "/" + pool.name().toLowerCase(Locale.ROOT) + "/" + Integer.toHexString(line.hashCode()) + "." + extension;
                    CACHE.add(maid.getUUID(), pool, new MorningKissGeneratedDialogueCache.Entry(line, line, fileName, data));
                    TouhouMaidAffection.LOGGER.info("Cached morning kiss AI dialogue voice '{}' ({} bytes) for maid {} pool {}.",
                            fileName, data.length, maid.getUUID(), pool.name().toLowerCase(Locale.ROOT));
                }
            });
        }
        return true;
    }

    private static String buildPrompt(String rawPrompt, ServerPlayer player, EntityMaid maid, MorningKissScheduleRules.DialoguePool pool) {
        String maidName = MaidDisplayNameResolver.resolveChatSafeDisplayName(maid).getString();
        String playerName = player.getName().getString();
        String prompt = rawPrompt == null ? "" : rawPrompt;
        prompt = prompt.replace("{maid}", maidName)
                .replace("{player}", playerName)
                .replace("{pool}", pool.name().toLowerCase(Locale.ROOT))
                .replace("{time}", MorningKissService.getAllowedTimeRangesText());
        return prompt + "\n请输出 3 句候选台词，每句单独一行，不要编号，不要解释，不要重复，不要加引号。";
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

    private record RequestKey(UUID playerUuid, UUID maidUuid) {
    }
}
