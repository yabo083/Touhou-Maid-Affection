package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.client.resource.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.SoundData;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MorningKissVoiceIndex {
    private static final Map<String, VoicePackIndex> CACHE = new ConcurrentHashMap<>();
    private static final String ENVIRONMENT_PATH_PREFIX = "sounds/maid/environment/";

    private MorningKissVoiceIndex() {
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
        return CACHE.computeIfAbsent(soundPackId, MorningKissVoiceIndex::buildPackIndex);
    }

    private static VoicePackIndex buildPackIndex(String soundPackId) {
        LinkedHashMap<String, VoiceEntry> entries = new LinkedHashMap<>();
        scanCustomPackFolder(soundPackId, entries);
        scanModsFolder(soundPackId, entries);
        List<VoiceEntry> sortedEntries = entries.values().stream()
                .sorted(Comparator.comparing(VoiceEntry::groupOrder)
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
                .sorted(Comparator.comparingInt(VoiceGroup::order))
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
        Path environmentDir = root.resolve("assets").resolve(soundPackId).resolve(ENVIRONMENT_PATH_PREFIX);
        if (!Files.isDirectory(environmentDir)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(environmentDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".ogg"))
                    .forEach(path -> addEntry(entries, toVoiceEntry(path.getFileName().toString(), new FileVoiceSource(path))));
        } catch (IOException ignored) {
        }
    }

    private static void scanZipPack(Path zipPath, String soundPackId, Map<String, VoiceEntry> entries) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            String prefix = "assets/" + soundPackId + "/" + ENVIRONMENT_PATH_PREFIX.replace('\\', '/');
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(prefix))
                    .filter(entry -> entry.getName().endsWith(".ogg"))
                    .forEach(entry -> addEntry(entries, toVoiceEntry(entry.getName().substring(prefix.length()), new ZipVoiceSource(zipPath, entry.getName()))));
        } catch (IOException ignored) {
        }
    }

    private static void addEntry(Map<String, VoiceEntry> entries, VoiceEntry entry) {
        if (entry != null) {
            entries.putIfAbsent(entry.clipKey(), entry);
        }
    }

    private static VoiceEntry toVoiceEntry(String fileName, VoiceSource source) {
        VoiceCategory category = VoiceCategory.fromFileName(fileName);
        if (category == null) {
            return null;
        }
        String clipKey = category.key() + "/" + fileName.toLowerCase(Locale.ROOT);
        String displayName = humanizeClipName(fileName, category.filePrefix());
        return new VoiceEntry(
                clipKey,
                category.key(),
                category.displayName(),
                displayName,
                Component.literal(source.describe()),
                category.soundEvent(),
                category.order(),
                source
        );
    }

    private static SoundBuffer loadSoundBufferFromFile(Path path) {
        List<SoundData> sounds = new ArrayList<>(1);
        OggReader.readSoundDataFromFile(path.toFile(), sounds, MarkerManager.getMarker("MorningKissVoice"));
        return toSoundBuffer(sounds);
    }

    private static SoundBuffer loadSoundBufferFromZip(Path zipPath, String entryName) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            List<SoundData> sounds = new ArrayList<>(1);
            OggReader.readSoundDataFromZip(zip, entry, entry.getName(), sounds, MarkerManager.getMarker("MorningKissVoice"));
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

    private static String humanizeClipName(String fileName, String prefixToRemove) {
        String stem = fileName;
        int dot = stem.lastIndexOf('.');
        if (dot > 0) {
            stem = stem.substring(0, dot);
        }
        String normalized = stem;
        if (normalized.toLowerCase(Locale.ROOT).startsWith(prefixToRemove.toLowerCase(Locale.ROOT))) {
            normalized = normalized.substring(prefixToRemove.length());
        }
        normalized = normalized.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.matches("^\\d+$")) {
            return prefixToRemove.substring(0, 1).toUpperCase(Locale.ROOT) + prefixToRemove.substring(1) + " " + normalized;
        }
        if (normalized.isBlank()) {
            return prefixToRemove.substring(0, 1).toUpperCase(Locale.ROOT) + prefixToRemove.substring(1);
        }
        String[] words = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
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

    private sealed interface VoiceSource permits FileVoiceSource, ZipVoiceSource {
        String describe();
    }

    private record FileVoiceSource(Path path) implements VoiceSource {
        @Override
        public String describe() {
            return path.getFileName().toString();
        }
    }

    private record ZipVoiceSource(Path zipPath, String entryName) implements VoiceSource {
        @Override
        public String describe() {
            int slash = entryName.lastIndexOf('/');
            return slash >= 0 ? entryName.substring(slash + 1) : entryName;
        }
    }

    private enum VoiceCategory {
        MORNING("morning", "Morning", "morning", InitSounds.MAID_MORNING.get().getLocation(), 0),
        NIGHT("night", "Night", "night", InitSounds.MAID_NIGHT.get().getLocation(), 1);

        private final String key;
        private final String displayName;
        private final String filePrefix;
        private final ResourceLocation soundEvent;
        private final int order;

        VoiceCategory(String key, String displayName, String filePrefix, ResourceLocation soundEvent, int order) {
            this.key = key;
            this.displayName = displayName;
            this.filePrefix = filePrefix;
            this.soundEvent = soundEvent;
            this.order = order;
        }

        public String key() {
            return key;
        }

        public String displayName() {
            return displayName;
        }

        public String filePrefix() {
            return filePrefix;
        }

        public ResourceLocation soundEvent() {
            return soundEvent;
        }

        public int order() {
            return order;
        }

        public static VoiceCategory fromFileName(String fileName) {
            if (fileName == null) {
                return null;
            }
            String normalized = fileName.toLowerCase(Locale.ROOT);
            for (VoiceCategory category : values()) {
                if (normalized.startsWith(category.filePrefix()) && normalized.endsWith(".ogg")) {
                    return category;
                }
            }
            return null;
        }
    }
}
