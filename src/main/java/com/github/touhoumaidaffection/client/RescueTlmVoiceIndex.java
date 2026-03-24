package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.resource.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.SoundData;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class RescueTlmVoiceIndex {
    private static final Map<String, VoicePackIndex> CACHE = new ConcurrentHashMap<>();
    private static final String MAID_SOUND_PREFIX = "sounds/maid/";
    private static final ResourceLocation DEFAULT_SOUND_EVENT = ResourceLocation.withDefaultNamespace("entity.player.levelup");

    private RescueTlmVoiceIndex() {
    }

    public static List<VoiceEntry> getEntries(String soundPackId) {
        return getPackIndex(soundPackId).entries();
    }

    public static List<VoiceGroup> getGroups(String soundPackId) {
        return getPackIndex(soundPackId).groups();
    }

    public static List<VoiceEntry> getEntriesForGroup(String soundPackId, String groupKey) {
        if (groupKey == null || groupKey.isBlank()) {
            return List.of();
        }
        return getPackIndex(soundPackId).entries().stream()
                .filter(entry -> groupKey.equals(entry.groupKey()))
                .toList();
    }

    public static VoiceEntry getEntry(String soundPackId, String clipKey) {
        if (clipKey == null || clipKey.isBlank()) {
            return null;
        }
        return getPackIndex(soundPackId).entryByKey().get(clipKey);
    }

    public static SoundBuffer loadSoundBuffer(String soundPackId, String clipKey) {
        VoiceEntry entry = getEntry(soundPackId, clipKey);
        if (entry == null) {
            return null;
        }
        return switch (entry.source()) {
            case FileVoiceSource fileSource -> loadSoundBufferFromFile(fileSource.path());
            case ZipVoiceSource zipSource -> loadSoundBufferFromZip(zipSource.zipPath(), zipSource.entryName());
        };
    }

    private static VoicePackIndex getPackIndex(String soundPackId) {
        if (soundPackId == null || soundPackId.isBlank()) {
            return VoicePackIndex.EMPTY;
        }
        return CACHE.computeIfAbsent(soundPackId, RescueTlmVoiceIndex::buildPackIndex);
    }

    private static VoicePackIndex buildPackIndex(String soundPackId) {
        LinkedHashMap<String, VoiceEntry> entries = new LinkedHashMap<>();
        scanCustomPackFolder(soundPackId, entries);
        scanModsFolder(soundPackId, entries);

        List<VoiceEntry> sortedEntries = entries.values().stream()
                .sorted(Comparator.comparing(VoiceEntry::groupOrder)
                        .thenComparing(VoiceEntry::groupDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(VoiceEntry::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(VoiceEntry::clipKey))
                .toList();
        if (sortedEntries.isEmpty()) {
            return VoicePackIndex.EMPTY;
        }

        List<VoiceGroup> groups = sortedEntries.stream()
                .collect(java.util.stream.Collectors.groupingBy(VoiceEntry::groupKey, LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    VoiceEntry first = entry.getValue().getFirst();
                    return new VoiceGroup(entry.getKey(), first.groupDisplayName(), entry.getValue().size(), first.groupOrder());
                })
                .sorted(Comparator.comparingInt(VoiceGroup::order).thenComparing(VoiceGroup::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        LinkedHashMap<String, VoiceEntry> entryByKey = new LinkedHashMap<>();
        for (VoiceEntry entry : sortedEntries) {
            entryByKey.put(entry.clipKey(), entry);
        }
        return new VoicePackIndex(sortedEntries, groups, Map.copyOf(entryByKey));
    }

    private static void scanCustomPackFolder(String soundPackId, Map<String, VoiceEntry> entries) {
        Path packFolder = CustomPackLoader.PACK_FOLDER;
        if (!Files.isDirectory(packFolder)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(packFolder)) {
            stream.forEach(candidate -> {
                if (Files.isDirectory(candidate)) {
                    scanFolderPack(candidate, soundPackId, entries);
                } else if (Files.isRegularFile(candidate) && candidate.getFileName().toString().endsWith(".zip")) {
                    scanZipPack(candidate, soundPackId, entries);
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static void scanModsFolder(String soundPackId, Map<String, VoiceEntry> entries) {
        Path modsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("mods");
        if (!Files.isDirectory(modsDir)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(modsDir)) {
            stream.forEach(candidate -> {
                if (Files.isDirectory(candidate)) {
                    scanFolderPack(candidate, soundPackId, entries);
                } else if (Files.isRegularFile(candidate) && (candidate.getFileName().toString().endsWith(".jar") || candidate.getFileName().toString().endsWith(".zip"))) {
                    scanZipPack(candidate, soundPackId, entries);
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static void scanFolderPack(Path root, String soundPackId, Map<String, VoiceEntry> entries) {
        Path maidSoundDir = root.resolve("assets").resolve(soundPackId).resolve("sounds").resolve("maid");
        if (!Files.isDirectory(maidSoundDir)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(maidSoundDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .forEach(path -> {
                        String relativePath = maidSoundDir.relativize(path).toString().replace('\\', '/');
                        addEntry(entries, toVoiceEntry(relativePath, new FileVoiceSource(path, relativePath)));
                    });
        } catch (IOException ignored) {
        }
    }

    private static void scanZipPack(Path zipPath, String soundPackId, Map<String, VoiceEntry> entries) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            String prefix = "assets/" + soundPackId + "/" + MAID_SOUND_PREFIX;
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(prefix))
                    .filter(entry -> entry.getName().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .forEach(entry -> {
                        String relativePath = entry.getName().substring(prefix.length());
                        addEntry(entries, toVoiceEntry(relativePath, new ZipVoiceSource(zipPath, entry.getName(), relativePath)));
                    });
        } catch (IOException ignored) {
        }
    }

    private static void addEntry(Map<String, VoiceEntry> entries, VoiceEntry entry) {
        if (entry != null) {
            entries.putIfAbsent(entry.clipKey(), entry);
        }
    }

    private static VoiceEntry toVoiceEntry(String relativePathRaw, VoiceSource source) {
        if (relativePathRaw == null || relativePathRaw.isBlank()) {
            return null;
        }
        String relativePath = relativePathRaw.replace('\\', '/');
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        if (relativePath.isBlank() || !relativePath.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            return null;
        }

        int slash = relativePath.lastIndexOf('/');
        String groupPath = slash >= 0 ? relativePath.substring(0, slash) : "general";
        String fileName = slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
        String groupKey = groupPath.toLowerCase(Locale.ROOT);

        return new VoiceEntry(
                relativePath.toLowerCase(Locale.ROOT),
                groupKey,
                humanizePath(groupPath),
                humanizeFileName(fileName),
                Component.literal(source.describe()),
                DEFAULT_SOUND_EVENT,
                groupOrder(groupKey),
                source
        );
    }

    private static int groupOrder(String groupKey) {
        String firstSegment = groupKey;
        int slash = firstSegment.indexOf('/');
        if (slash >= 0) {
            firstSegment = firstSegment.substring(0, slash);
        }
        return switch (firstSegment) {
            case "environment" -> 0;
            case "chat" -> 1;
            case "idle" -> 2;
            case "attack" -> 3;
            case "hurt" -> 4;
            case "death" -> 5;
            case "interact" -> 6;
            default -> 10;
        };
    }

    private static String humanizePath(String path) {
        if (path == null || path.isBlank()) {
            return "General";
        }
        String[] segments = path.replace('\\', '/').split("/");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(titleCase(segment.replace('_', ' ').replace('-', ' ')));
        }
        return builder.isEmpty() ? "General" : builder.toString();
    }

    private static String humanizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Clip";
        }
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String normalized = stem.replace('_', ' ').replace('-', ' ').trim();
        return normalized.isBlank() ? "Clip" : titleCase(normalized);
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] words = raw.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    private static SoundBuffer loadSoundBufferFromFile(Path path) {
        List<SoundData> sounds = new ArrayList<>(1);
        OggReader.readSoundDataFromFile(path.toFile(), sounds, MarkerManager.getMarker("RescueTlmVoice"));
        return toSoundBuffer(sounds);
    }

    private static SoundBuffer loadSoundBufferFromZip(Path zipPath, String entryName) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            List<SoundData> sounds = new ArrayList<>(1);
            OggReader.readSoundDataFromZip(zip, entry, entry.getName(), sounds, MarkerManager.getMarker("RescueTlmVoice"));
            return toSoundBuffer(sounds);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static SoundBuffer toSoundBuffer(List<SoundData> sounds) {
        if (sounds.isEmpty()) {
            return null;
        }
        SoundData sound = sounds.getFirst();
        return new SoundBuffer(sound.byteBuffer(), sound.audioFormat());
    }

    public record VoiceEntry(
            String clipKey,
            String groupKey,
            String groupDisplayName,
            String displayName,
            Component detail,
            ResourceLocation soundEventId,
            int groupOrder,
            VoiceSource source
    ) {
    }

    public record VoiceGroup(String key, String displayName, int entryCount, int order) {
    }

    private record VoicePackIndex(List<VoiceEntry> entries, List<VoiceGroup> groups, Map<String, VoiceEntry> entryByKey) {
        private static final VoicePackIndex EMPTY = new VoicePackIndex(List.of(), List.of(), Map.of());
    }

    public sealed interface VoiceSource permits FileVoiceSource, ZipVoiceSource {
        String describe();
    }

    public record FileVoiceSource(Path path, String relativePath) implements VoiceSource {
        @Override
        public String describe() {
            return relativePath;
        }
    }

    public record ZipVoiceSource(Path zipPath, String entryName, String relativePath) implements VoiceSource {
        @Override
        public String describe() {
            return relativePath;
        }
    }
}
