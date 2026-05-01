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
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class MorningKissProfileData {
    private static final Gson GSON = new Gson();
    private static final int MAX_DATA_PACK_VOICE_BYTES = 2 * 1024 * 1024;
    private static final ResourceLocation PROFILE_PATH =
            new ResourceLocation(TouhouMaidAffection.MOD_ID, "morning_kiss/profile.json");

    private static volatile MorningKissProfileParser.MorningKissProfile activeProfile =
            MorningKissProfileParser.MorningKissProfile.defaults();
    private static volatile List<DataPackVoice> activeVoices = List.of();

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

    public static Optional<DataPackVoice> selectVoice(net.minecraft.util.RandomSource random) {
        List<DataPackVoice> voices = activeVoices;
        if (voices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(voices.get(random.nextInt(voices.size())));
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
                    profile = MorningKissProfileParser.merge(profile, root);
                }
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to load morning kiss profile from {}", PROFILE_PATH, ex);
            }
        }

        List<DataPackVoice> voices = loadVoices(resourceManager, profile.voiceFiles());
        activeProfile = profile;
        activeVoices = voices;
        TouhouMaidAffection.LOGGER.info("Morning kiss profile loaded: sound={}, dialogueMode={}, voiceMode={}, voices={}, voiceFiles={}, aiDialogue={}",
                profile.kissSoundEventId(), profile.dialogueMode().name().toLowerCase(java.util.Locale.ROOT),
                profile.voiceMode().name().toLowerCase(java.util.Locale.ROOT), voices.size(), profile.voiceFiles(), profile.aiDialogue().enabled());
    }

    private static List<DataPackVoice> loadVoices(ResourceManager resourceManager, List<String> voiceFiles) {
        if (voiceFiles.isEmpty()) {
            return List.of();
        }
        ArrayList<DataPackVoice> voices = new ArrayList<>();
        for (String voiceFile : voiceFiles) {
            ResourceLocation voicePath = new ResourceLocation(
                    TouhouMaidAffection.MOD_ID,
                    "morning_kiss/voices/" + voiceFile
            );
            byte[] data = readVoiceBytes(resourceManager, voicePath);
            if (data.length > 0) {
                voices.add(new DataPackVoice(voiceFile, data));
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
                data = inputStream.readAllBytes();
                if (data.length > MAX_DATA_PACK_VOICE_BYTES) {
                    TouhouMaidAffection.LOGGER.warn("Morning kiss data-pack voice {} is too large ({} bytes), max is {} bytes.",
                            voicePath, data.length, MAX_DATA_PACK_VOICE_BYTES);
                    data = new byte[0];
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

    public record DataPackVoice(String fileName, byte[] data) {
    }

    private static final class ReloadListener implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            reloadFrom(resourceManager);
        }
    }
}
