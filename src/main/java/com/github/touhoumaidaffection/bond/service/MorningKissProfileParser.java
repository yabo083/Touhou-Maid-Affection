package com.github.touhoumaidaffection.bond.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class MorningKissProfileParser {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private MorningKissProfileParser() {
    }

    static MorningKissProfile merge(MorningKissProfile base, JsonObject root) {
        String kissSoundEventId = base.kissSoundEventId();
        DialogueMode dialogueMode = base.dialogueMode();
        VoiceMode voiceMode = base.voiceMode();
        boolean playKissSoundWithVoice = base.playKissSoundWithVoice();
        Map<MorningKissScheduleRules.DialoguePool, List<String>> dialogues = new EnumMap<>(base.dialogues());
        List<String> voiceFiles = base.voiceFiles();

        if (root.has("kiss_sound_event")) {
            kissSoundEventId = parseSoundEventId(root.get("kiss_sound_event").getAsString(), kissSoundEventId);
        }
        if (root.has("dialogue_mode")) {
            dialogueMode = DialogueMode.fromName(root.get("dialogue_mode").getAsString(), dialogueMode);
        }
        if (root.has("voice_mode")) {
            voiceMode = VoiceMode.fromName(root.get("voice_mode").getAsString(), voiceMode);
        }
        if (root.has("play_kiss_sound_with_voice")) {
            playKissSoundWithVoice = root.get("play_kiss_sound_with_voice").getAsBoolean();
        }
        if (root.has("dialogue") && root.get("dialogue").isJsonObject()) {
            JsonObject dialogueRoot = root.getAsJsonObject("dialogue");
            for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
                List<String> parsed = parseDialogueList(dialogueRoot.get(pool.name().toLowerCase(Locale.ROOT)));
                if (!parsed.isEmpty()) {
                    dialogues.put(pool, parsed);
                }
            }
        }
        if (root.has("voice_files")) {
            voiceFiles = parseVoiceFiles(root.get("voice_files"));
        }

        return new MorningKissProfile(kissSoundEventId, dialogueMode, voiceMode, playKissSoundWithVoice, Map.copyOf(dialogues), voiceFiles);
    }

    private static List<String> parseDialogueList(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        java.util.ArrayList<String> output = new java.util.ArrayList<>();
        for (JsonElement value : array) {
            if (!value.isJsonPrimitive()) {
                continue;
            }
            String text = value.getAsString();
            if (text != null && !text.isBlank()) {
                output.add(text.trim());
            }
        }
        return output;
    }

    private static List<String> parseVoiceFiles(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        java.util.ArrayList<String> output = new java.util.ArrayList<>();
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

    private static String parseSoundEventId(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim();
        return RESOURCE_ID.matcher(trimmed).matches() ? trimmed : fallback;
    }

    record MorningKissProfile(
            String kissSoundEventId,
            DialogueMode dialogueMode,
            VoiceMode voiceMode,
            boolean playKissSoundWithVoice,
            Map<MorningKissScheduleRules.DialoguePool, List<String>> dialogues,
            List<String> voiceFiles
    ) {
        static final String DEFAULT_KISS_SOUND_EVENT_ID = "touhou_maid_affection:touhou_maid_affection.kiss";

        static MorningKissProfile defaults() {
            Map<MorningKissScheduleRules.DialoguePool, List<String>> dialogues =
                    new EnumMap<>(MorningKissScheduleRules.DialoguePool.class);
            for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
                dialogues.put(pool, List.of());
            }
            return new MorningKissProfile(
                    DEFAULT_KISS_SOUND_EVENT_ID,
                    DialogueMode.REPLACE,
                    VoiceMode.APPEND,
                    true,
                    Map.copyOf(dialogues),
                    List.of()
            );
        }
    }

    enum DialogueMode {
        REPLACE,
        APPEND;

        static DialogueMode fromName(String raw, DialogueMode fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return DialogueMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

    enum VoiceMode {
        REPLACE,
        APPEND;

        static VoiceMode fromName(String raw, VoiceMode fallback) {
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
}
