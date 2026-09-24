package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.service.MorningKissScheduleRules.DialoguePool;
import com.github.touhoumaidaffection.network.MorningKissDataVoicePlayPayload;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MorningKissDialogueService {
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

    private MorningKissDialogueService() {
    }

    static boolean show(ServerPlayer player, EntityMaid maid, DialoguePool dialoguePool, String pairedSubtitle) {
        GeneratedDialogueResult generated = tryShowGeneratedDialogue(player, maid, dialoguePool);
        if (generated != GeneratedDialogueResult.MISSING) {
            return true;
        }
        boolean bilingualVoicePending = requiresBilingualGeneratedVoice(maid);
        if (!bilingualVoicePending && tryShowAiDialogue(player, maid, dialoguePool)) {
            return true;
        }
        if (!bilingualVoicePending && pairedSubtitle != null && !pairedSubtitle.isBlank()) {
            showDialogue(player, maid, Component.literal(renderTemplate(pairedSubtitle, player, maid, dialoguePool)));
            return false;
        }
        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileData.getActiveProfile();
        List<MorningKissDataPackEntries.DialogueLine> configuredPool =
                profile.dialogues().getOrDefault(dialoguePool, List.of());
        if (configuredPool.isEmpty()) {
            configuredPool = profile.dialogues().getOrDefault(DialoguePool.GENERAL, List.of());
        }
        if (!configuredPool.isEmpty()) {
            String[] builtinKeys = DIALOGUE_KEYS.getOrDefault(dialoguePool, DIALOGUE_KEYS.get(DialoguePool.GENERAL));
            List<MorningKissDataPackEntries.DialogueChoice> choices = MorningKissDataPackEntries.buildDialogueChoices(
                    configuredPool,
                    List.of(builtinKeys),
                    player.getLanguage(),
                    profile.dialogueMode() == MorningKissProfileParser.DialogueMode.APPEND
            );
            MorningKissDataPackEntries.DialogueChoice chosen = MorningKissDataPackEntries.pickDialogue(
                    choices, MorningKissLanguageSettings.displayLanguage(), player.getRandom().nextInt());
            if (chosen.isBuiltin()) {
                showBuiltinDialogue(player, maid, dialoguePool, chosen.builtinIndex());
            } else {
                showConfiguredDialogue(player, maid, dialoguePool, chosen.configured().text());
            }
            return bilingualVoicePending;
        }
        showBuiltinDialogue(player, maid, dialoguePool, -1);
        return bilingualVoicePending;
    }

    static void showMessage(ServerPlayer player, Component message) {
        if (isChatMode()) {
            player.sendSystemMessage(message);
            return;
        }
        player.displayClientMessage(message, true);
    }

    static Component startMessage(String actionBarKey, String chatKey, EntityMaid maid) {
        boolean chatMode = isChatMode();
        return Component.translatable(chatMode ? chatKey : actionBarKey, resolvedName(maid, chatMode));
    }

    private static void showConfiguredDialogue(
            ServerPlayer player,
            EntityMaid maid,
            DialoguePool dialoguePool,
            String rawText
    ) {
        showDialogue(player, maid, Component.literal(renderTemplate(rawText, player, maid, dialoguePool)));
    }

    private static void showBuiltinDialogue(
            ServerPlayer player,
            EntityMaid maid,
            DialoguePool dialoguePool,
            int preferredIndex
    ) {
        String[] pool = DIALOGUE_KEYS.getOrDefault(dialoguePool, DIALOGUE_KEYS.get(DialoguePool.GENERAL));
        int index = preferredIndex >= 0 && preferredIndex < pool.length ? preferredIndex : player.getRandom().nextInt(pool.length);
        showDialogue(player, maid, Component.translatable(pool[index], resolvedName(maid, true)));
    }

    private static GeneratedDialogueResult tryShowGeneratedDialogue(
            ServerPlayer player,
            EntityMaid maid,
            DialoguePool dialoguePool
    ) {
        return MorningKissGeneratedDialogueService.pollCachedLine(maid, dialoguePool, player.getRandom())
                .map(entry -> {
                    showDialogue(player, maid, Component.literal(entry.text()));
                    if (entry.hasVoice()) {
                        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MorningKissDataVoicePlayPayload(
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
                    MorningKissLanguageSettings.liveChatLanguage(),
                    MaidDisplayNameResolver.resolveChatSafeDisplayName(maid).getString(),
                    List.of()
            );
            TouhouMaidAffection.LOGGER.info(
                    "Dispatching live Morning Kiss AI dialogue for maid {} pool {}.",
                    maid.getUUID(),
                    dialoguePool.name().toLowerCase(Locale.ROOT)
            );
            maid.getAiChatManager().chat(prompt, clientInfo, player);
            return true;
        } catch (Throwable throwable) {
            TouhouMaidAffection.LOGGER.warn(
                    "Failed to dispatch live Morning Kiss AI dialogue, falling back to static dialogue.",
                    throwable
            );
            return false;
        }
    }

    private static boolean requiresBilingualGeneratedVoice(EntityMaid maid) {
        if (!ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_ENABLED.get()
                || !ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_TTS_ENABLED.get()) {
            return false;
        }
        return MorningKissGeneratedDialogueLanguage.requiresTranslation(
                MorningKissGeneratedDialogueService.resolveChatLanguage(maid),
                MorningKissGeneratedDialogueService.resolveVoiceTextLanguage(maid)
        );
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
                .replace("{time}", MorningKissScheduleRules.formatAllowedTimeRanges(
                        ModConfig.BOND_MORNING_KISS_ALLOWED_TIME_RANGES.get()
                ));
    }

    private static void showDialogue(ServerPlayer player, EntityMaid maid, Component message) {
        if (!ModConfig.BOND_MORNING_KISS_DIALOGUE_CHAT_BUBBLE_ENABLED.get()) {
            showMessage(player, message);
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
        showMessage(player, message);
    }

    private static Component resolvedName(EntityMaid maid, boolean chatMode) {
        return chatMode
                ? MaidDisplayNameResolver.resolveChatSafeDisplayName(maid)
                : MaidDisplayNameResolver.resolveDisplayName(maid);
    }

    private static boolean isChatMode() {
        return "chat".equalsIgnoreCase(ModConfig.BOND_MORNING_KISS_MESSAGE_DISPLAY_MODE.get());
    }

    private enum GeneratedDialogueResult {
        MISSING,
        TEXT_ONLY,
        VOICE_PLAYED
    }
}
