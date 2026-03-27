package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class EmergencyRescueSoundProfileData {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation PROFILE_PATH =
            new ResourceLocation(TouhouMaidAffection.MOD_ID, "rescue_sound/profile.json");

    private static volatile EmergencyRescueSoundProfile activeProfile = EmergencyRescueSoundProfile.defaults();

    private EmergencyRescueSoundProfileData() {
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static EmergencyRescueSoundProfile getActiveProfile() {
        return activeProfile;
    }

    private static void reloadFrom(ResourceManager resourceManager) {
        EmergencyRescueSoundProfile profile = EmergencyRescueSoundProfile.defaults();
        for (PackResources packResources : resourceManager.listPacks().toList()) {
            IoSupplier<InputStream> resource = packResources.getResource(PackType.SERVER_DATA, PROFILE_PATH);
            if (resource == null) {
                continue;
            }
            try (InputStream inputStream = resource.get();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    profile = merge(profile, root);
                }
            } catch (Exception ex) {
                TouhouMaidAffection.LOGGER.warn("Failed to load emergency rescue sound profile from {}", PROFILE_PATH, ex);
            }
        }

        activeProfile = profile;
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue sound profile loaded: event={}, allowClientOverride={}, maxClientDuration={}s, format={}",
                profile.soundEventId(),
                profile.allowClientOverride(),
                String.format(java.util.Locale.ROOT, "%.2f", profile.maxClientSoundDurationSeconds()),
                profile.requiredClientSoundFormat()
        );
    }

    private static EmergencyRescueSoundProfile merge(EmergencyRescueSoundProfile base, JsonObject root) {
        String soundEventId = base.soundEventId();
        boolean allowClientOverride = base.allowClientOverride();
        double maxDuration = base.maxClientSoundDurationSeconds();
        String requiredFormat = base.requiredClientSoundFormat();

        if (root.has("sound_event")) {
            soundEventId = parseSoundEventId(root.get("sound_event").getAsString(), soundEventId);
        }
        if (root.has("allow_client_override")) {
            allowClientOverride = root.get("allow_client_override").getAsBoolean();
        }
        if (root.has("max_client_sound_duration_seconds")) {
            maxDuration = sanitizeMaxDuration(root.get("max_client_sound_duration_seconds").getAsDouble(), maxDuration);
        } else if (root.has("max_duration_seconds")) {
            maxDuration = sanitizeMaxDuration(root.get("max_duration_seconds").getAsDouble(), maxDuration);
        }
        if (root.has("required_client_sound_format")) {
            requiredFormat = normalizeFormat(root.get("required_client_sound_format").getAsString(), requiredFormat);
        } else if (root.has("required_format")) {
            requiredFormat = normalizeFormat(root.get("required_format").getAsString(), requiredFormat);
        }

        return new EmergencyRescueSoundProfile(soundEventId, allowClientOverride, maxDuration, requiredFormat);
    }

    private static String parseSoundEventId(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(raw.trim());
        if (parsed == null) {
            TouhouMaidAffection.LOGGER.warn("Invalid emergency rescue sound event id '{}', keeping '{}'", raw, fallback);
            return fallback;
        }
        return parsed.toString();
    }

    private static double sanitizeMaxDuration(double raw, double fallback) {
        if (!Double.isFinite(raw) || raw <= 0.0D) {
            return fallback;
        }
        return Math.max(0.1D, Math.min(30.0D, raw));
    }

    private static String normalizeFormat(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isBlank()) {
            return fallback;
        }
        return trimmed;
    }

    public record EmergencyRescueSoundProfile(
            String soundEventId,
            boolean allowClientOverride,
            double maxClientSoundDurationSeconds,
            String requiredClientSoundFormat
    ) {
        private static final String DEFAULT_SOUND_EVENT_ID = "minecraft:entity.player.levelup";
        private static final double DEFAULT_MAX_CLIENT_SOUND_DURATION_SECONDS = 4.0D;
        private static final String DEFAULT_REQUIRED_CLIENT_SOUND_FORMAT = "ogg";

        public static EmergencyRescueSoundProfile defaults() {
            return new EmergencyRescueSoundProfile(
                    DEFAULT_SOUND_EVENT_ID,
                    true,
                    DEFAULT_MAX_CLIENT_SOUND_DURATION_SECONDS,
                    DEFAULT_REQUIRED_CLIENT_SOUND_FORMAT
            );
        }
    }

    private static final class ReloadListener implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            reloadFrom(resourceManager);
        }
    }
}
