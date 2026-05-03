package com.github.touhoumaidaffection.bond.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

final class MorningKissGeneratedDialogueStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ROOT_DIR = "generated_morning_kiss";

    private MorningKissGeneratedDialogueStorage() {
    }

    static Path storageRoot(Path worldRoot) {
        return worldRoot.resolve(ROOT_DIR);
    }

    static void save(Path worldRoot, Map<UUID, Map<MorningKissScheduleRules.DialoguePool, List<MorningKissGeneratedDialogueCache.Entry>>> snapshot) throws IOException {
        Path root = storageRoot(worldRoot);
        deleteDirectory(root);
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Map<MorningKissScheduleRules.DialoguePool, List<MorningKissGeneratedDialogueCache.Entry>>> maidEntry : snapshot.entrySet()) {
            UUID maidUuid = maidEntry.getKey();
            if (maidUuid == null || maidEntry.getValue() == null) {
                continue;
            }
            for (Map.Entry<MorningKissScheduleRules.DialoguePool, List<MorningKissGeneratedDialogueCache.Entry>> poolEntry : maidEntry.getValue().entrySet()) {
                MorningKissScheduleRules.DialoguePool pool = poolEntry.getKey();
                List<MorningKissGeneratedDialogueCache.Entry> entries = poolEntry.getValue();
                if (pool == null || entries == null || entries.isEmpty()) {
                    continue;
                }
                Path poolDir = root.resolve(maidUuid.toString()).resolve(poolName(pool));
                Files.createDirectories(poolDir);
                for (int index = 0; index < entries.size(); index++) {
                    writeEntry(poolDir, index + 1, entries.get(index));
                }
            }
        }
    }

    static Map<UUID, Map<MorningKissScheduleRules.DialoguePool, List<MorningKissGeneratedDialogueCache.Entry>>> load(Path worldRoot) throws IOException {
        Path root = storageRoot(worldRoot);
        Map<UUID, Map<MorningKissScheduleRules.DialoguePool, List<MorningKissGeneratedDialogueCache.Entry>>> snapshot = new HashMap<>();
        if (!Files.isDirectory(root)) {
            return snapshot;
        }
        try (Stream<Path> maidDirs = Files.list(root)) {
            for (Path maidDir : maidDirs.filter(Files::isDirectory).sorted().toList()) {
                UUID maidUuid = parseUuid(maidDir.getFileName().toString()).orElse(null);
                if (maidUuid == null) {
                    continue;
                }
                Map<MorningKissScheduleRules.DialoguePool, List<MorningKissGeneratedDialogueCache.Entry>> pools =
                        snapshot.computeIfAbsent(maidUuid, ignored -> new EnumMap<>(MorningKissScheduleRules.DialoguePool.class));
                try (Stream<Path> poolDirs = Files.list(maidDir)) {
                    for (Path poolDir : poolDirs.filter(Files::isDirectory).sorted().toList()) {
                        MorningKissScheduleRules.DialoguePool pool = parsePool(poolDir.getFileName().toString()).orElse(null);
                        if (pool == null) {
                            continue;
                        }
                        List<MorningKissGeneratedDialogueCache.Entry> entries;
                        try (Stream<Path> jsonFiles = Files.list(poolDir)) {
                            entries = jsonFiles
                                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                                    .sorted()
                                    .map(MorningKissGeneratedDialogueStorage::readEntry)
                                    .flatMap(Optional::stream)
                                    .toList();
                        }
                        if (!entries.isEmpty()) {
                            pools.put(pool, entries);
                        }
                    }
                }
            }
        }
        return snapshot;
    }

    private static void writeEntry(Path poolDir, int displayIndex, MorningKissGeneratedDialogueCache.Entry entry) throws IOException {
        if (entry == null || !entry.hasText()) {
            return;
        }
        String baseName = String.format(Locale.ROOT, "%03d", displayIndex);
        String voiceFile = "";
        if (entry.hasVoice()) {
            String extension = MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(entry.voiceData()).orElse("ogg");
            voiceFile = baseName + "." + extension;
            Files.write(poolDir.resolve(voiceFile), entry.voiceData());
        }
        JsonObject root = new JsonObject();
        root.addProperty("text", entry.text());
        root.addProperty("tts_text", entry.ttsText());
        root.addProperty("voice_file", voiceFile);
        root.addProperty("text_language", entry.textLanguage());
        root.addProperty("voice_language", entry.voiceLanguage());
        root.addProperty("maid_name", entry.maidName());
        try (Writer writer = Files.newBufferedWriter(poolDir.resolve(baseName + ".json"), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static Optional<MorningKissGeneratedDialogueCache.Entry> readEntry(Path jsonPath) {
        try (Reader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String text = getString(root, "text");
            if (MorningKissGeneratedDialogueCache.normalizeLine(text).isBlank()) {
                return Optional.empty();
            }
            String voiceFile = getString(root, "voice_file");
            byte[] voiceData = readVoiceData(jsonPath.getParent(), voiceFile);
            return Optional.of(new MorningKissGeneratedDialogueCache.Entry(
                    text,
                    getString(root, "tts_text"),
                    voiceData.length == 0 ? "" : voiceFile,
                    voiceData,
                    getString(root, "text_language"),
                    getString(root, "voice_language"),
                    getString(root, "maid_name")
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static byte[] readVoiceData(Path poolDir, String voiceFile) throws IOException {
        if (poolDir == null || voiceFile == null || voiceFile.isBlank() || voiceFile.contains("/") || voiceFile.contains("\\") || voiceFile.contains("..")) {
            return new byte[0];
        }
        Path voicePath = poolDir.resolve(voiceFile).normalize();
        if (!voicePath.startsWith(poolDir.normalize()) || !Files.isRegularFile(voicePath)) {
            return new byte[0];
        }
        byte[] data = Files.readAllBytes(voicePath);
        return MorningKissGeneratedDialogueCache.detectPlayableVoiceExtension(data).isPresent() ? data : new byte[0];
    }

    private static String getString(JsonObject root, String key) {
        return root != null && root.has(key) && root.get(key).isJsonPrimitive()
                ? root.get(key).getAsString()
                : "";
    }

    private static Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Optional<MorningKissScheduleRules.DialoguePool> parsePool(String raw) {
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            if (poolName(pool).equalsIgnoreCase(raw)) {
                return Optional.of(pool);
            }
        }
        return Optional.empty();
    }

    private static String poolName(MorningKissScheduleRules.DialoguePool pool) {
        return pool.name().toLowerCase(Locale.ROOT);
    }

    private static void deleteDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.toAbsolutePath().normalize().startsWith(normalizedRoot)) {
                    throw new IOException("Refusing to delete outside generated dialogue cache root: " + path);
                }
                Files.deleteIfExists(path);
            }
        }
    }
}
