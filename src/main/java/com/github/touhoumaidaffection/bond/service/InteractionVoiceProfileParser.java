package com.github.touhoumaidaffection.bond.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InteractionVoiceProfileParser {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private InteractionVoiceProfileParser() {
    }

    public static InteractionVoiceProfile merge(InteractionVoiceProfile base, JsonObject root) {
        if (root == null) {
            return base;
        }
        FeatureVoiceProfile morningKiss = mergeFeature(base.morningKiss(), root.get("morning_kiss"), Feature.MORNING_KISS);
        FeatureVoiceProfile emergencyRescue = mergeFeature(base.emergencyRescue(), root.get("emergency_rescue"), Feature.EMERGENCY_RESCUE);
        List<MaidVoiceOverride> maidOverrides = new ArrayList<>(base.maidOverrides());
        if (root.has("maids") && root.get("maids").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("maids")) {
                MaidVoiceOverride override = parseMaidOverride(element);
                if (override != null) {
                    maidOverrides.add(override);
                }
            }
        }
        return new InteractionVoiceProfile(morningKiss, emergencyRescue, List.copyOf(maidOverrides));
    }

    public static InteractionVoiceProfile mergeIsolatedFeature(InteractionVoiceProfile base, JsonObject root, Feature feature) {
        if (root == null) {
            return base;
        }
        FeatureVoiceProfile featureProfile = mergeFeature(base.featureProfile(feature), root, feature);
        return feature == Feature.MORNING_KISS
                ? new InteractionVoiceProfile(featureProfile, base.emergencyRescue(), base.maidOverrides())
                : new InteractionVoiceProfile(base.morningKiss(), featureProfile, base.maidOverrides());
    }

    private static MaidVoiceOverride parseMaidOverride(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject root = element.getAsJsonObject();
        MaidMatcher matcher = parseMatcher(root.get("match"));
        if (matcher.isEmpty()) {
            return null;
        }
        FeatureVoicePatch morningKiss = parseFeaturePatch(root.get("morning_kiss"), Feature.MORNING_KISS);
        FeatureVoicePatch emergencyRescue = parseFeaturePatch(root.get("emergency_rescue"), Feature.EMERGENCY_RESCUE);
        if (morningKiss == null && emergencyRescue == null) {
            return null;
        }
        return new MaidVoiceOverride(matcher, morningKiss, emergencyRescue);
    }

    private static MaidMatcher parseMatcher(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return MaidMatcher.empty();
        }
        JsonObject root = element.getAsJsonObject();
        return new MaidMatcher(
                readString(root, "uuid"),
                readString(root, "name"),
                readString(root, "model"),
                readString(root, "sound_pack"),
                readString(root, "ysm_model")
        );
    }

    private static FeatureVoiceProfile mergeFeature(FeatureVoiceProfile base, JsonElement element, Feature feature) {
        FeatureVoicePatch patch = parseFeaturePatch(element, feature);
        return patch == null ? base : patch.applyTo(base);
    }

    private static FeatureVoicePatch parseFeaturePatch(JsonElement element, Feature feature) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject root = element.getAsJsonObject();
        VoiceMode voiceMode = null;
        List<String> voiceFiles = null;
        RescueOptions rescueOptions = null;
        if (root.has("voice_mode")) {
            voiceMode = VoiceMode.fromName(root.get("voice_mode").getAsString(), null);
        }
        if (root.has("voice_files")) {
            voiceFiles = parseVoiceFiles(root.get("voice_files"));
        }
        if (feature == Feature.EMERGENCY_RESCUE) {
            rescueOptions = parseRescueOptions(root);
        }
        return new FeatureVoicePatch(voiceMode, voiceFiles, rescueOptions);
    }

    private static RescueOptions parseRescueOptions(JsonObject root) {
        String soundEventId = null;
        Boolean allowClientOverride = null;
        Double maxClientSoundDurationSeconds = null;
        String requiredClientSoundFormat = null;

        if (root.has("sound_event")) {
            soundEventId = parseSoundEventId(root.get("sound_event").getAsString());
        }
        if (root.has("allow_client_override")) {
            allowClientOverride = root.get("allow_client_override").getAsBoolean();
        }
        if (root.has("max_client_sound_duration_seconds")) {
            maxClientSoundDurationSeconds = sanitizeMaxDuration(root.get("max_client_sound_duration_seconds").getAsDouble(), null);
        } else if (root.has("max_duration_seconds")) {
            maxClientSoundDurationSeconds = sanitizeMaxDuration(root.get("max_duration_seconds").getAsDouble(), null);
        }
        if (root.has("required_client_sound_format")) {
            requiredClientSoundFormat = normalizeFormat(root.get("required_client_sound_format").getAsString(), null);
        } else if (root.has("required_format")) {
            requiredClientSoundFormat = normalizeFormat(root.get("required_format").getAsString(), null);
        }
        return new RescueOptions(soundEventId, allowClientOverride, maxClientSoundDurationSeconds, requiredClientSoundFormat);
    }

    private static List<String> parseVoiceFiles(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        ArrayList<String> output = new ArrayList<>();
        for (JsonElement value : array) {
            if (!value.isJsonPrimitive()) {
                continue;
            }
            String path = normalizeVoicePath(value.getAsString());
            if (!path.isBlank()) {
                output.add(path);
            }
        }
        return List.copyOf(output);
    }

    private static String normalizeVoicePath(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.contains("\\")) {
            return "";
        }
        String path = raw.trim();
        if (path.isBlank()
                || path.startsWith("/")
                || path.contains("..")
                || !path.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            return "";
        }
        return path;
    }

    private static String parseSoundEventId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return RESOURCE_ID.matcher(trimmed).matches() ? trimmed : null;
    }

    private static Double sanitizeMaxDuration(double raw, Double fallback) {
        if (!Double.isFinite(raw) || raw <= 0.0D) {
            return fallback;
        }
        return Math.max(0.1D, Math.min(30.0D, raw));
    }

    private static String normalizeFormat(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static String readString(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonPrimitive()) {
            return "";
        }
        String value = root.get(key).getAsString();
        return value == null ? "" : value.trim();
    }

    public enum Feature {
        MORNING_KISS,
        EMERGENCY_RESCUE
    }

    public enum VoiceMode {
        REPLACE,
        APPEND;

        public static VoiceMode fromName(String raw, VoiceMode fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return VoiceMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

    public record InteractionVoiceProfile(
            FeatureVoiceProfile morningKiss,
            FeatureVoiceProfile emergencyRescue,
            List<MaidVoiceOverride> maidOverrides
    ) {
        public static InteractionVoiceProfile defaults() {
            return new InteractionVoiceProfile(
                    FeatureVoiceProfile.defaults(),
                    FeatureVoiceProfile.defaults(),
                    List.of()
            );
        }

        public FeatureVoiceProfile resolve(Feature feature, MaidContext maid) {
            FeatureVoiceProfile resolved = featureProfile(feature);
            for (MaidVoiceOverride override : maidOverrides) {
                if (!override.matcher().matches(maid)) {
                    continue;
                }
                FeatureVoicePatch patch = feature == Feature.MORNING_KISS ? override.morningKiss() : override.emergencyRescue();
                if (patch != null) {
                    resolved = patch.applyTo(resolved);
                }
            }
            return resolved;
        }

        public FeatureVoiceProfile featureProfile(Feature feature) {
            return feature == Feature.MORNING_KISS ? morningKiss : emergencyRescue;
        }
    }

    public record FeatureVoiceProfile(
            VoiceMode voiceMode,
            List<String> voiceFiles,
            RescueOptions rescueOptions
    ) {
        public static FeatureVoiceProfile defaults() {
            return new FeatureVoiceProfile(VoiceMode.APPEND, List.of(), RescueOptions.defaults());
        }
    }

    public record RescueOptions(
            String soundEventId,
            Boolean allowClientOverride,
            Double maxClientSoundDurationSeconds,
            String requiredClientSoundFormat
    ) {
        public static RescueOptions defaults() {
            return new RescueOptions(null, null, null, null);
        }

        RescueOptions merge(RescueOptions patch) {
            if (patch == null) {
                return this;
            }
            return new RescueOptions(
                    patch.soundEventId == null ? soundEventId : patch.soundEventId,
                    patch.allowClientOverride == null ? allowClientOverride : patch.allowClientOverride,
                    patch.maxClientSoundDurationSeconds == null ? maxClientSoundDurationSeconds : patch.maxClientSoundDurationSeconds,
                    patch.requiredClientSoundFormat == null ? requiredClientSoundFormat : patch.requiredClientSoundFormat
            );
        }
    }

    public record MaidContext(
            String uuid,
            String name,
            String model,
            String soundPack,
            String ysmModel
    ) {
    }

    public record MaidMatcher(
            String uuid,
            String name,
            String model,
            String soundPack,
            String ysmModel
    ) {
        static MaidMatcher empty() {
            return new MaidMatcher("", "", "", "", "");
        }

        boolean isEmpty() {
            return uuid.isBlank() && name.isBlank() && model.isBlank() && soundPack.isBlank() && ysmModel.isBlank();
        }

        boolean matches(MaidContext maid) {
            if (maid == null) {
                return false;
            }
            return matchesIgnoreCase(uuid, maid.uuid())
                    && matchesIgnoreCase(name, maid.name())
                    && matchesIgnoreCase(model, maid.model())
                    && matchesIgnoreCase(soundPack, maid.soundPack())
                    && matchesIgnoreCase(ysmModel, maid.ysmModel());
        }

        private static boolean matchesIgnoreCase(String expected, String actual) {
            return expected == null || expected.isBlank() || expected.equalsIgnoreCase(actual == null ? "" : actual);
        }
    }

    public record MaidVoiceOverride(
            MaidMatcher matcher,
            FeatureVoicePatch morningKiss,
            FeatureVoicePatch emergencyRescue
    ) {
    }

    public record FeatureVoicePatch(
            VoiceMode voiceMode,
            List<String> voiceFiles,
            RescueOptions rescueOptions
    ) {
        FeatureVoiceProfile applyTo(FeatureVoiceProfile base) {
            return new FeatureVoiceProfile(
                    voiceMode == null ? base.voiceMode() : voiceMode,
                    voiceFiles == null ? base.voiceFiles() : voiceFiles,
                    base.rescueOptions().merge(rescueOptions)
            );
        }
    }
}
