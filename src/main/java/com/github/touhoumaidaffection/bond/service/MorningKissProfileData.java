package com.github.touhoumaidaffection.bond.service;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class MorningKissProfileData {
    private static final Gson GSON = new Gson();
    private static final int MAX_DATA_PACK_VOICE_BYTES = 2 * 1024 * 1024;
    private static final ResourceLocation PROFILE_PATH =
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "morning_kiss/profile.json");

    private static volatile MorningKissProfileParser.MorningKissProfile activeProfile =
            MorningKissProfileParser.MorningKissProfile.defaults();
    private static volatile List<DataPackVoice> activeVoices = List.of();
    private static volatile Map<String, MorningKissDataPackEntries.VoiceFile> activeVoiceEntries = Map.of();

    private MorningKissProfileData() {
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static MorningKissProfileParser.MorningKissProfile getActiveProfile() {
        return activeProfile;
    }

    public static String getKissSoundEventId() {
        return activeProfile.kissSoundEventId();
    }

    public static boolean shouldPlayKissSoundWithVoice() {
        return activeProfile.playKissSoundWithVoice();
    }

    public static Optional<DataPackVoice> selectVoice(net.minecraft.util.RandomSource random, String targetLanguage) {
        List<DataPackVoice> eligible = eligibleVoices(targetLanguage);
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(eligible.get(random.nextInt(eligible.size())));
    }

    /** 按目标配音语种筛选候选语音（匹配 → 未标记 → 全部）；语种为空时不过滤。 */
    public static List<DataPackVoice> eligibleVoices(String targetLanguage) {
        return MorningKissDataPackEntries.selectByLanguage(activeVoices, DataPackVoice::language, targetLanguage);
    }

    /** 按目标配音语种筛选给定的文件名列表（用于统一语音池）；语种为空时不过滤。 */
    public static List<String> filterVoiceFilesByLanguage(List<String> fileNames, String targetLanguage) {
        return MorningKissDataPackEntries.selectByLanguage(fileNames, MorningKissProfileData::voiceLanguageOf, targetLanguage);
    }

    /** 返回某个数据包语音文件声明的语种；未声明或未知返回空串。 */
    public static String voiceLanguageOf(String fileName) {
        MorningKissDataPackEntries.VoiceFile entry = activeVoiceEntries.get(fileName);
        return entry == null ? "" : entry.language();
    }

    /**
     * 返回某个数据包语音文件配对的可选字幕。
     *
     * <p>没有配对文本、或配对文本的 {@code text_language} 与目标显示语种不匹配时返回空串，
     * 调用方应回退到随机台词。</p>
     */
    public static String pairedSubtitle(String fileName, String targetDisplayLanguage) {
        return MorningKissDataPackEntries.pairedSubtitle(activeVoiceEntries.get(fileName), targetDisplayLanguage);
    }

    public static boolean hasDataPackVoices() {
        return !activeVoices.isEmpty();
    }

    private static void reloadFrom(ResourceManager resourceManager) {
        MorningKissProfileParser.MorningKissProfile profile = MorningKissProfileParser.MorningKissProfile.defaults();
        for (PackResources packResources : resourceManager.listPacks().toList()) {
            IoSupplier<InputStream> resource = packResources.getResource(PackType.SERVER_DATA, PROFILE_PATH);
            if (resource == null) {
                continue;
            }
            try (InputStream inputStream = resource.get();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    profile = MorningKissProfileParser.merge(profile, root, message ->
                            TouhouMaidAffection.LOGGER.debug("{} ({})", message, PROFILE_PATH));
                }
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to load morning kiss profile from {}", PROFILE_PATH, ex);
            }
        }

        Map<String, MorningKissDataPackEntries.VoiceFile> voiceEntries = new LinkedHashMap<>();
        for (MorningKissDataPackEntries.VoiceFile voiceFile : profile.voiceFiles()) {
            voiceEntries.putIfAbsent(voiceFile.file(), voiceFile);
        }
        List<DataPackVoice> voices = loadVoices(resourceManager, profile.voiceFiles());
        activeProfile = profile;
        activeVoiceEntries = Map.copyOf(voiceEntries);
        activeVoices = voices;
        TouhouMaidAffection.LOGGER.info("Morning kiss profile loaded: sound={}, dialogueMode={}, voiceMode={}, voices={}, voiceFiles={}",
                profile.kissSoundEventId(), profile.dialogueMode().name().toLowerCase(Locale.ROOT),
                profile.voiceMode().name().toLowerCase(Locale.ROOT), voices.size(),
                profile.voiceFiles().stream().map(MorningKissDataPackEntries.VoiceFile::file).toList());
    }

    private static List<DataPackVoice> loadVoices(ResourceManager resourceManager,
                                                  List<MorningKissDataPackEntries.VoiceFile> voiceFiles) {
        if (voiceFiles.isEmpty()) {
            return List.of();
        }
        ArrayList<DataPackVoice> voices = new ArrayList<>();
        for (MorningKissDataPackEntries.VoiceFile voiceFile : voiceFiles) {
            ResourceLocation voicePath = ResourceLocation.fromNamespaceAndPath(
                    TouhouMaidAffection.MOD_ID,
                    "morning_kiss/voices/" + voiceFile.file()
            );
            byte[] data = readVoiceBytes(resourceManager, voicePath);
            if (data.length > 0) {
                voices.add(new DataPackVoice(voiceFile.file(), data, voiceFile.language()));
            }
        }
        return List.copyOf(voices);
    }

    private static byte[] readVoiceBytes(ResourceManager resourceManager, ResourceLocation voicePath) {
        byte[] data = new byte[0];
        for (PackResources packResources : resourceManager.listPacks().toList()) {
            IoSupplier<InputStream> resource = packResources.getResource(PackType.SERVER_DATA, voicePath);
            if (resource == null) {
                continue;
            }
            try (InputStream inputStream = resource.get()) {
                BoundedVoiceDataReader.ReadResult result = BoundedVoiceDataReader.read(inputStream, MAX_DATA_PACK_VOICE_BYTES);
                data = result.data();
                if (result.exceededLimit()) {
                    TouhouMaidAffection.LOGGER.warn("Morning kiss data-pack voice {} exceeds the {} byte limit.",
                            voicePath, MAX_DATA_PACK_VOICE_BYTES);
                }
                if (data.length > 0) {
                    TouhouMaidAffection.LOGGER.info("Loaded morning kiss data-pack voice {} ({} bytes)",
                            voicePath, data.length);
                }
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to load morning kiss data-pack voice {}", voicePath, ex);
            }
        }
        return data;
    }

    public record DataPackVoice(String fileName, byte[] data, String language) {
    }

    private static final class ReloadListener implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            reloadFrom(resourceManager);
        }
    }
}