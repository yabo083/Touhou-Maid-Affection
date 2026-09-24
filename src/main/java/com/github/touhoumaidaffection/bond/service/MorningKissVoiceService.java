package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.bond.VoicePoolSelection;
import com.github.touhoumaidaffection.network.MorningKissDataVoicePlayPayload;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MorningKissVoiceService {
    private static final Map<String, Integer> VOICE_SEQUENCE_INDEX = new HashMap<>();

    private MorningKissVoiceService() {
    }

    static Selection select(ServerPlayer player, EntityMaid maid) {
        String soundPackId = maid.getSoundPackId() == null ? "" : maid.getSoundPackId();
        MorningKissVoiceSettings settings = BondManager.getMorningKissVoiceSettings(player, maid.getUUID()).withSoundPackId(soundPackId);
        InteractionVoiceProfileData.ResolvedVoiceProfile profile = InteractionVoiceProfileData.resolveMorningKiss(maid);
        List<String> selectedIds = effectiveVoiceIds(settings.selectedVoiceIds(), profile);
        if (selectedIds.isEmpty()) {
            return null;
        }
        String selectedId = selectVoiceId(selectedIds, settings.mode(), "morning:" + maid.getUUID(), player.getRandom());
        if (selectedId.isBlank()) {
            return null;
        }
        if (VoicePoolIds.BUILTIN_MORNING_KISS.equals(selectedId)) {
            return Selection.builtin();
        }
        if (VoicePoolIds.isDataPack(selectedId)) {
            return InteractionVoiceProfileData.selectVoiceByFile(profile, VoicePoolIds.value(selectedId))
                    .map(voice -> Selection.dataPack(selectedId, voice))
                    .orElse(null);
        }
        if (VoicePoolIds.isTlm(selectedId) && !soundPackId.isBlank()) {
            return Selection.tlm(selectedId);
        }
        return null;
    }

    static boolean shouldPlayKissSound(ServerPlayer player, EntityMaid maid, Selection selection) {
        if (MorningKissProfileData.shouldPlayKissSoundWithVoice()) {
            return true;
        }
        if (selection != null) {
            return selection.isBuiltin();
        }
        return !hasVoiceCandidate(player, maid);
    }

    static void play(ServerPlayer player, EntityMaid maid, Selection selection) {
        if (playSelection(player, maid, selection)) {
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
                selectTlmFallbackVoiceId(settings)
        ));
        return true;
    }

    private static String selectTlmFallbackVoiceId(MorningKissVoiceSettings settings) {
        if (settings.mode() == MorningKissVoiceSettings.Mode.SPECIFIC_CLIP && !settings.selectedClip().isBlank()) {
            return VoicePoolIds.tlm(settings.selectedClip());
        }
        return "";
    }

    private static boolean hasVoiceCandidate(ServerPlayer player, EntityMaid maid) {
        if (InteractionVoiceProfileData.resolveMorningKiss(maid).hasVoices()) {
            return true;
        }
        if (MorningKissProfileData.hasDataPackVoices()) {
            return true;
        }
        return maid.getSoundPackId() != null && !maid.getSoundPackId().isBlank();
    }

    private static List<String> effectiveVoiceIds(
            List<String> savedIds,
            InteractionVoiceProfileData.ResolvedVoiceProfile profile
    ) {
        List<String> defaults = defaultVoiceIds(profile);
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

    private static List<String> defaultVoiceIds(InteractionVoiceProfileData.ResolvedVoiceProfile profile) {
        ArrayList<String> ids = new ArrayList<>();
        boolean includeBasePool = VoicePoolSelection.shouldIncludeBasePool(
                profile.voiceMode().name().toLowerCase(Locale.ROOT),
                profile.fileNames()
        );
        if (includeBasePool) {
            ids.add(VoicePoolIds.BUILTIN_MORNING_KISS);
        }
        ids.addAll(MorningKissProfileData
                .filterVoiceFilesByLanguage(profile.fileNames(), MorningKissLanguageSettings.voiceLanguage())
                .stream()
                .map(VoicePoolIds::dataPack)
                .toList());
        return ids;
    }

    private static boolean playSelection(ServerPlayer player, EntityMaid maid, Selection selection) {
        if (selection == null) {
            return false;
        }
        if (selection.isBuiltin()) {
            return true;
        }
        if (selection.dataPackVoice() != null) {
            InteractionVoiceProfileData.DataPackVoice voice = selection.dataPackVoice();
            PacketDistributor.sendToPlayer(player, new MorningKissDataVoicePlayPayload(
                    maid.getId(),
                    maid.getUUID(),
                    voice.fileName(),
                    voice.data()
            ));
            return true;
        }
        if (VoicePoolIds.isTlm(selection.selectedId())) {
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
                    VoicePoolIds.value(selection.selectedId()),
                    selection.selectedId()
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
        return MorningKissProfileData.selectVoice(player.getRandom(), MorningKissLanguageSettings.voiceLanguage())
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

    private static boolean playUnifiedDataPackVoice(
            ServerPlayer player,
            EntityMaid maid,
            InteractionVoiceProfileData.ResolvedVoiceProfile profile
    ) {
        List<InteractionVoiceProfileData.DataPackVoice> eligible = eligibleDataPackVoices(profile);
        if (eligible.isEmpty()) {
            return false;
        }
        InteractionVoiceProfileData.DataPackVoice voice = eligible.get(player.getRandom().nextInt(eligible.size()));
        TouhouMaidAffection.LOGGER.info("Sending unified morning kiss data-pack voice '{}' ({} bytes) to {}",
                voice.fileName(), voice.data().length, player.getGameProfile().getName());
        PacketDistributor.sendToPlayer(player, new MorningKissDataVoicePlayPayload(
                maid.getId(),
                maid.getUUID(),
                voice.fileName(),
                voice.data()
        ));
        return true;
    }

    private static List<InteractionVoiceProfileData.DataPackVoice> eligibleDataPackVoices(
            InteractionVoiceProfileData.ResolvedVoiceProfile profile
    ) {
        String target = MorningKissLanguageSettings.voiceLanguage();
        if (target.isBlank()) {
            return profile.voices();
        }
        return MorningKissDataPackEntries.selectByLanguage(
                profile.voices(),
                voice -> MorningKissProfileData.voiceLanguageOf(voice.fileName()),
                target
        );
    }

    /** 返回选中数据包语音配对的可选字幕；没有配对、或与目标显示语种不匹配时返回空串。 */
    static String pairedSubtitle(Selection selection) {
        if (selection == null || selection.dataPackVoice() == null) {
            return "";
        }
        return MorningKissProfileData.pairedSubtitle(
                selection.dataPackVoice().fileName(),
                MorningKissLanguageSettings.displayLanguage()
        );
    }

    record Selection(String selectedId, InteractionVoiceProfileData.DataPackVoice dataPackVoice) {
        private static Selection builtin() {
            return new Selection(VoicePoolIds.BUILTIN_MORNING_KISS, null);
        }

        private static Selection dataPack(String selectedId, InteractionVoiceProfileData.DataPackVoice voice) {
            return new Selection(selectedId, voice);
        }

        private static Selection tlm(String selectedId) {
            return new Selection(selectedId, null);
        }

        private boolean isBuiltin() {
            return VoicePoolIds.BUILTIN_MORNING_KISS.equals(selectedId);
        }
    }
}
