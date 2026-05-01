package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class InteractionVoiceProfileData {
    private static final Gson GSON = new Gson();
    private static final int MAX_DATA_PACK_VOICE_BYTES = 2 * 1024 * 1024;
    private static final ResourceLocation MORNING_PROFILE_PATH =
            new ResourceLocation(TouhouMaidAffection.MOD_ID, "morning_kiss/profile.json");
    private static final ResourceLocation RESCUE_PROFILE_PATH =
            new ResourceLocation(TouhouMaidAffection.MOD_ID, "emergency_rescue/profile.json");

    private static volatile InteractionVoiceProfileParser.InteractionVoiceProfile activeProfile =
            InteractionVoiceProfileParser.InteractionVoiceProfile.defaults();
    private static volatile Map<InteractionVoiceProfileParser.Feature, Map<String, DataPackVoice>> activeVoices = Map.of();

    private InteractionVoiceProfileData() {
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static ResolvedVoiceProfile resolveMorningKiss(EntityMaid maid) {
        return resolve(InteractionVoiceProfileParser.Feature.MORNING_KISS, contextOf(maid));
    }

    public static ResolvedVoiceProfile resolveEmergencyRescue(String maidUuid, BondData.MaidProfileSnapshot profile) {
        return resolve(InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE, contextOf(maidUuid, profile));
    }

    private static ResolvedVoiceProfile resolve(InteractionVoiceProfileParser.Feature feature,
                                                InteractionVoiceProfileParser.MaidContext context) {
        InteractionVoiceProfileParser.FeatureVoiceProfile profile = activeProfile.resolve(feature, context);
        Map<String, DataPackVoice> featureVoices = activeVoices.getOrDefault(feature, Map.of());
        List<DataPackVoice> voices = profile.voiceFiles().stream()
                .map(featureVoices::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ResolvedVoiceProfile(profile.voiceMode(), voices, profile.rescueOptions());
    }

    public static Optional<DataPackVoice> selectVoice(ResolvedVoiceProfile profile, RandomSource random) {
        if (profile == null || profile.voices().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(profile.voices().get(random.nextInt(profile.voices().size())));
    }

    public static Optional<DataPackVoice> selectVoiceByFile(ResolvedVoiceProfile profile, String fileName) {
        if (profile == null || fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        return profile.voices().stream()
                .filter(voice -> voice.fileName().equals(fileName))
                .findFirst();
    }

    private static InteractionVoiceProfileParser.MaidContext contextOf(EntityMaid maid) {
        if (maid == null) {
            return new InteractionVoiceProfileParser.MaidContext("", "", "", "", "");
        }
        return new InteractionVoiceProfileParser.MaidContext(
                maid.getUUID().toString(),
                maid.getName().getString(),
                maid.getModelId() == null ? "" : maid.getModelId().toString(),
                maid.getSoundPackId(),
                maid.isYsmModel() ? maid.getYsmModelId() : ""
        );
    }

    private static InteractionVoiceProfileParser.MaidContext contextOf(String maidUuid, BondData.MaidProfileSnapshot profile) {
        if (profile == null) {
            return new InteractionVoiceProfileParser.MaidContext(safe(maidUuid), "", "", "", "");
        }
        return new InteractionVoiceProfileParser.MaidContext(
                safe(maidUuid),
                profile.displayName(),
                profile.modelId(),
                profile.soundPackId(),
                profile.ysmModelId()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void reloadFrom(ResourceManager resourceManager) {
        InteractionVoiceProfileParser.InteractionVoiceProfile profile =
                InteractionVoiceProfileParser.InteractionVoiceProfile.defaults();
        profile = loadFeatureProfile(resourceManager, profile, MORNING_PROFILE_PATH, InteractionVoiceProfileParser.Feature.MORNING_KISS);
        profile = loadFeatureProfile(resourceManager, profile, RESCUE_PROFILE_PATH, InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE);

        Map<InteractionVoiceProfileParser.Feature, Map<String, DataPackVoice>> voices = new EnumMap<>(InteractionVoiceProfileParser.Feature.class);
        voices.put(InteractionVoiceProfileParser.Feature.MORNING_KISS,
                loadVoiceFiles(resourceManager, collectFeatureVoiceFiles(profile, InteractionVoiceProfileParser.Feature.MORNING_KISS), "morning_kiss"));
        voices.put(InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE,
                loadVoiceFiles(resourceManager, collectFeatureVoiceFiles(profile, InteractionVoiceProfileParser.Feature.EMERGENCY_RESCUE), "emergency_rescue"));
        activeProfile = profile;
        activeVoices = voices;
        TouhouMaidAffection.LOGGER.info(
                "Interaction voice profile loaded: morningFiles={}, rescueFiles={}, maidOverrides={}, loadedVoices={}",
                profile.morningKiss().voiceFiles().size(),
                profile.emergencyRescue().voiceFiles().size(),
                profile.maidOverrides().size(),
                voices.values().stream().mapToInt(Map::size).sum()
        );
    }

    private static InteractionVoiceProfileParser.InteractionVoiceProfile loadFeatureProfile(ResourceManager resourceManager,
                                                                                            InteractionVoiceProfileParser.InteractionVoiceProfile profile,
                                                                                            ResourceLocation profilePath,
                                                                                            InteractionVoiceProfileParser.Feature feature) {
        InteractionVoiceProfileParser.InteractionVoiceProfile current = profile;
        for (PackResources packResources : resourceManager.listPacks().toList()) {
            IoSupplier<InputStream> resource = packResources.getResource(PackType.SERVER_DATA, profilePath);
            if (resource == null) {
                continue;
            }
            try (InputStream inputStream = resource.get();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    current = InteractionVoiceProfileParser.mergeIsolatedFeature(current, root, feature);
                }
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to load interaction voice profile from {}", profilePath, ex);
            }
        }
        return current;
    }

    private static LinkedHashSet<String> collectFeatureVoiceFiles(InteractionVoiceProfileParser.InteractionVoiceProfile profile,
                                                                  InteractionVoiceProfileParser.Feature feature) {
        LinkedHashSet<String> output = new LinkedHashSet<>();
        output.addAll(profile.featureProfile(feature).voiceFiles());
        for (InteractionVoiceProfileParser.MaidVoiceOverride override : profile.maidOverrides()) {
            InteractionVoiceProfileParser.FeatureVoicePatch patch =
                    feature == InteractionVoiceProfileParser.Feature.MORNING_KISS ? override.morningKiss() : override.emergencyRescue();
            if (patch != null && patch.voiceFiles() != null) {
                output.addAll(patch.voiceFiles());
            }
        }
        return output;
    }

    private static Map<String, DataPackVoice> loadVoiceFiles(ResourceManager resourceManager, LinkedHashSet<String> voiceFiles, String folder) {
        return voiceFiles.stream()
                .map(path -> loadVoice(resourceManager, folder, path))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableMap(DataPackVoice::fileName, Function.identity(), (left, right) -> right));
    }

    private static Optional<DataPackVoice> loadVoice(ResourceManager resourceManager, String folder, String voiceFile) {
        ResourceLocation voicePath = new ResourceLocation(
                TouhouMaidAffection.MOD_ID,
                folder + "/voices/" + voiceFile
        );
        byte[] data = new byte[0];
        for (PackResources packResources : resourceManager.listPacks().toList()) {
            IoSupplier<InputStream> resource = packResources.getResource(PackType.SERVER_DATA, voicePath);
            if (resource == null) {
                continue;
            }
            try (InputStream inputStream = resource.get()) {
                data = inputStream.readAllBytes();
                if (data.length > MAX_DATA_PACK_VOICE_BYTES) {
                    TouhouMaidAffection.LOGGER.warn("Interaction data-pack voice {} is too large ({} bytes), max is {} bytes.",
                            voicePath, data.length, MAX_DATA_PACK_VOICE_BYTES);
                    data = new byte[0];
                }
                if (data.length > 0) {
                    TouhouMaidAffection.LOGGER.info("Loaded interaction data-pack voice {} ({} bytes)", voicePath, data.length);
                }
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to load interaction data-pack voice {}", voicePath, ex);
            }
        }
        return data.length == 0 ? Optional.empty() : Optional.of(new DataPackVoice(voiceFile, data));
    }

    public record ResolvedVoiceProfile(
            InteractionVoiceProfileParser.VoiceMode voiceMode,
            List<DataPackVoice> voices,
            InteractionVoiceProfileParser.RescueOptions rescueOptions
    ) {
        public boolean hasVoices() {
            return !voices.isEmpty();
        }

        public List<String> fileNames() {
            return voices.stream().map(DataPackVoice::fileName).toList();
        }
    }

    public record DataPackVoice(String fileName, byte[] data) {
    }

    private static final class ReloadListener implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            reloadFrom(resourceManager);
        }
    }
}
