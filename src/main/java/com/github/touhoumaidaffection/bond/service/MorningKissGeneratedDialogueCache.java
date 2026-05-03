package com.github.touhoumaidaffection.bond.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.RandomSource;

final class MorningKissGeneratedDialogueCache {
    private static final int DEFAULT_MAX_LINES_PER_POOL = 8;
    private static final int MAX_LINE_DISPLAY_WIDTH = 96;
    private static final int MAX_VOICE_BYTES = 2 * 1024 * 1024;

    private final int maxLinesPerPool;
    private final Map<CacheKey, Deque<Entry>> entries = new HashMap<>();

    MorningKissGeneratedDialogueCache() {
        this(DEFAULT_MAX_LINES_PER_POOL);
    }

    MorningKissGeneratedDialogueCache(int maxLinesPerPool) {
        this.maxLinesPerPool = Math.max(1, maxLinesPerPool);
    }

    static List<String> normalizeLines(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> output = new LinkedHashSet<>();
        for (String line : raw.split("\\R")) {
            String normalized = normalizeLine(line);
            if (!normalized.isBlank()) {
                output.add(normalized);
            }
        }
        return List.copyOf(output);
    }

    static boolean isMaybeOgg(byte[] data) {
        return data != null
                && data.length >= 4
                && data[0] == 'O'
                && data[1] == 'g'
                && data[2] == 'g'
                && data[3] == 'S';
    }

    static Optional<String> detectPlayableVoiceExtension(byte[] data) {
        if (isMaybeOgg(data)) {
            return Optional.of("ogg");
        }
        if (isMaybeMp3(data)) {
            return Optional.of("mp3");
        }
        return Optional.empty();
    }

    static List<String> limitToRemainingCapacity(List<String> lines, int currentSize, int targetSize) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        int remaining = Math.max(0, targetSize - Math.max(0, currentSize));
        if (remaining <= 0) {
            return List.of();
        }
        if (lines.size() <= remaining) {
            return lines;
        }
        return List.copyOf(lines.subList(0, remaining));
    }

    private static boolean isMaybeMp3(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }
        if (data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }
        return data.length >= 2
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xE0) == 0xE0;
    }

    synchronized void add(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, Entry entry) {
        if (maidUuid == null || pool == null || entry == null) {
            return;
        }
        Deque<Entry> queue = entries.computeIfAbsent(new CacheKey(maidUuid, pool), key -> new ArrayDeque<>());
        if (queue.size() >= maxLinesPerPool) {
            queue.removeFirst();
        }
        queue.addLast(entry);
    }

    synchronized boolean addIfBelowTarget(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, Entry entry,
                                          int targetSize) {
        if (maidUuid == null || pool == null || entry == null) {
            return false;
        }
        int boundedTarget = Math.max(1, Math.min(maxLinesPerPool, targetSize));
        Deque<Entry> queue = entries.computeIfAbsent(new CacheKey(maidUuid, pool), key -> new ArrayDeque<>());
        if (queue.size() >= boundedTarget) {
            return false;
        }
        queue.addLast(entry);
        return true;
    }

    synchronized Optional<Entry> pollRandom(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, RandomSource random) {
        if (maidUuid == null || pool == null) {
            return Optional.empty();
        }
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }
        int index = random.nextInt(queue.size());
        int current = 0;
        java.util.Iterator<Entry> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (current++ == index) {
                iterator.remove();
                if (queue.isEmpty()) {
                    entries.remove(new CacheKey(maidUuid, pool));
                }
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    synchronized Optional<Entry> peekRandom(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, RandomSource random) {
        if (maidUuid == null || pool == null) {
            return Optional.empty();
        }
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }
        int index = random.nextInt(queue.size());
        int current = 0;
        for (Entry entry : queue) {
            if (current++ == index) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    synchronized Optional<Entry> pollFirst(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
        if (maidUuid == null || pool == null) {
            return Optional.empty();
        }
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }
        Entry entry = queue.removeFirst();
        if (queue.isEmpty()) {
            entries.remove(new CacheKey(maidUuid, pool));
        }
        return Optional.of(entry);
    }

    synchronized int size(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        return queue == null ? 0 : queue.size();
    }

    synchronized boolean isEmpty(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
        return size(maidUuid, pool) == 0;
    }

    synchronized int clear(UUID maidUuid) {
        if (maidUuid == null) {
            return 0;
        }
        int removed = 0;
        for (MorningKissScheduleRules.DialoguePool pool : MorningKissScheduleRules.DialoguePool.values()) {
            Deque<Entry> queue = entries.remove(new CacheKey(maidUuid, pool));
            if (queue != null) {
                removed += queue.size();
            }
        }
        return removed;
    }

    synchronized int clear(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
        if (maidUuid == null || pool == null) {
            return 0;
        }
        Deque<Entry> queue = entries.remove(new CacheKey(maidUuid, pool));
        return queue == null ? 0 : queue.size();
    }

    synchronized int removeAt(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, int index) {
        if (maidUuid == null || pool == null || index < 0) {
            return 0;
        }
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        if (queue == null || index >= queue.size()) {
            return 0;
        }
        List<Entry> values = new ArrayList<>(queue);
        values.remove(index);
        if (values.isEmpty()) {
            entries.remove(new CacheKey(maidUuid, pool));
        } else {
            entries.put(new CacheKey(maidUuid, pool), new ArrayDeque<>(values));
        }
        return 1;
    }

    synchronized boolean clearVoiceAt(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, int index) {
        if (maidUuid == null || pool == null || index < 0) {
            return false;
        }
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        if (queue == null || index >= queue.size()) {
            return false;
        }
        List<Entry> values = new ArrayList<>(queue);
        Entry existing = values.get(index);
        if (!existing.hasVoice()) {
            return false;
        }
        values.set(index, existing.withoutVoice());
        entries.put(new CacheKey(maidUuid, pool), new ArrayDeque<>(values));
        return true;
    }

    synchronized Optional<Entry> entryAt(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool, int index) {
        if (maidUuid == null || pool == null || index < 0) {
            return Optional.empty();
        }
        Deque<Entry> queue = entries.get(new CacheKey(maidUuid, pool));
        if (queue == null || index >= queue.size()) {
            return Optional.empty();
        }
        int current = 0;
        for (Entry entry : queue) {
            if (current++ == index) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    synchronized int clearAll() {
        int removed = 0;
        for (Deque<Entry> queue : entries.values()) {
            removed += queue.size();
        }
        entries.clear();
        return removed;
    }

    synchronized Map<UUID, Map<MorningKissScheduleRules.DialoguePool, List<Entry>>> snapshot() {
        Map<UUID, Map<MorningKissScheduleRules.DialoguePool, List<Entry>>> snapshot = new HashMap<>();
        for (Map.Entry<CacheKey, Deque<Entry>> cacheEntry : entries.entrySet()) {
            snapshot.computeIfAbsent(cacheEntry.getKey().maidUuid(), ignored -> new EnumMap<>(MorningKissScheduleRules.DialoguePool.class))
                    .put(cacheEntry.getKey().pool(), List.copyOf(cacheEntry.getValue()));
        }
        return snapshot;
    }

    synchronized void replaceAll(Map<UUID, Map<MorningKissScheduleRules.DialoguePool, List<Entry>>> snapshot) {
        entries.clear();
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Map<MorningKissScheduleRules.DialoguePool, List<Entry>>> maidEntry : snapshot.entrySet()) {
            UUID maidUuid = maidEntry.getKey();
            if (maidUuid == null || maidEntry.getValue() == null) {
                continue;
            }
            for (Map.Entry<MorningKissScheduleRules.DialoguePool, List<Entry>> poolEntry : maidEntry.getValue().entrySet()) {
                MorningKissScheduleRules.DialoguePool pool = poolEntry.getKey();
                List<Entry> values = poolEntry.getValue();
                if (pool == null || values == null || values.isEmpty()) {
                    continue;
                }
                Deque<Entry> queue = new ArrayDeque<>();
                for (Entry entry : values) {
                    if (entry != null && entry.hasText()) {
                        if (queue.size() >= maxLinesPerPool) {
                            queue.removeFirst();
                        }
                        queue.addLast(entry);
                    }
                }
                if (!queue.isEmpty()) {
                    entries.put(new CacheKey(maidUuid, pool), queue);
                }
            }
        }
    }

    synchronized Stats stats(long revision, int inFlightRequests) {
        int totalEntries = 0;
        int voiceEntries = 0;
        Map<UUID, MaidStatsBuilder> maidStats = new HashMap<>();
        for (Map.Entry<CacheKey, Deque<Entry>> cacheEntry : entries.entrySet()) {
            UUID maidUuid = cacheEntry.getKey().maidUuid();
            MaidStatsBuilder maidBuilder = maidStats.computeIfAbsent(maidUuid, MaidStatsBuilder::new);
            for (Entry entry : cacheEntry.getValue()) {
                totalEntries++;
                maidBuilder.add(cacheEntry.getKey().pool(), entry);
                if (entry.hasVoice()) {
                    voiceEntries++;
                }
            }
        }
        List<MaidStats> maids = maidStats.values().stream()
                .map(MaidStatsBuilder::build)
                .sorted((left, right) -> left.label().compareToIgnoreCase(right.label()))
                .toList();
        return new Stats(totalEntries, maids.size(), voiceEntries, revision, Math.max(0, inFlightRequests), maids);
    }

    static String normalizeLine(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        while (!value.isBlank()) {
            String stripped = stripLeadingMarker(value);
            if (stripped.equals(value)) {
                break;
            }
            value = stripped;
        }
        value = stripWrappingQuotes(value);
        if (displayWidth(value) > MAX_LINE_DISPLAY_WIDTH) {
            return "";
        }
        if (value.isBlank()) {
            return "";
        }
        return value;
    }

    private static int displayWidth(String value) {
        int width = 0;
        for (int index = 0; index < value.length(); index++) {
            width += value.charAt(index) <= 0x7F ? 1 : 2;
        }
        return width;
    }

    private static String stripLeadingMarker(String raw) {
        String value = raw.trim();
        if (value.startsWith("- ") || value.startsWith("* ") || value.startsWith("• ")) {
            return value.substring(2).trim();
        }
        int dot = value.indexOf(". ");
        if (dot > 0 && dot < 4 && value.substring(0, dot).chars().allMatch(Character::isDigit)) {
            return value.substring(dot + 2).trim();
        }
        int rightParen = value.indexOf(") ");
        if (rightParen > 0 && rightParen < 4 && value.substring(0, rightParen).chars().allMatch(Character::isDigit)) {
            return value.substring(rightParen + 2).trim();
        }
        return value;
    }

    private static String stripWrappingQuotes(String raw) {
        String value = raw.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '“' && last == '”')
                    || (first == '‘' && last == '’')
                    || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    record Entry(String text, String ttsText, String voiceFileName, byte[] voiceData,
                 String textLanguage, String voiceLanguage, String maidName) {
        Entry(String text, String ttsText, String voiceFileName, byte[] voiceData) {
            this(text, ttsText, voiceFileName, voiceData, "", "", "");
        }

        Entry(String text, String ttsText, String voiceFileName, byte[] voiceData,
              String textLanguage, String voiceLanguage) {
            this(text, ttsText, voiceFileName, voiceData, textLanguage, voiceLanguage, "");
        }

        Entry {
            text = text == null ? "" : text.trim();
            ttsText = ttsText == null || ttsText.isBlank() ? text : ttsText.trim();
            voiceFileName = voiceFileName == null ? "" : voiceFileName.trim();
            voiceData = voiceData == null ? new byte[0] : voiceData;
            textLanguage = textLanguage == null ? "" : textLanguage.trim();
            voiceLanguage = voiceLanguage == null ? "" : voiceLanguage.trim();
            maidName = maidName == null ? "" : maidName.trim();
        }

        boolean hasVoice() {
            return !voiceFileName.isBlank() && voiceData.length > 0;
        }

        boolean hasText() {
            return !text.isBlank();
        }

        Entry withoutVoice() {
            return new Entry(text, ttsText, "", new byte[0], textLanguage, "", maidName);
        }
    }

    record Stats(int totalEntries, int maidCount, int voiceEntries, long revision, int inFlightRequests,
                 List<MaidStats> maids) {
        int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    record MaidStats(UUID maidUuid, String maidName, int totalEntries, int voiceEntries, List<PoolStats> pools) {
        int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }

        String label() {
            return maidName.isBlank() ? maidUuid.toString() : maidName;
        }
    }

    record PoolStats(MorningKissScheduleRules.DialoguePool pool, String textLanguage, String voiceLanguage,
                     int totalEntries, int voiceEntries) {
        int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    private static final class MaidStatsBuilder {
        private final UUID maidUuid;
        private final Map<PoolGroupKey, PoolStatsBuilder> pools = new HashMap<>();
        private String maidName = "";
        private int totalEntries;
        private int voiceEntries;

        private MaidStatsBuilder(UUID maidUuid) {
            this.maidUuid = maidUuid;
        }

        private void add(MorningKissScheduleRules.DialoguePool pool, Entry entry) {
            if (maidName.isBlank() && !entry.maidName().isBlank()) {
                maidName = entry.maidName();
            }
            totalEntries++;
            if (entry.hasVoice()) {
                voiceEntries++;
            }
            PoolGroupKey key = new PoolGroupKey(pool, entry.textLanguage(), entry.voiceLanguage());
            pools.computeIfAbsent(key, PoolStatsBuilder::new).add(entry);
        }

        private MaidStats build() {
            List<PoolStats> poolStats = pools.values().stream()
                    .map(PoolStatsBuilder::build)
                    .sorted((left, right) -> {
                        int poolCompare = left.pool().name().compareTo(right.pool().name());
                        if (poolCompare != 0) {
                            return poolCompare;
                        }
                        int textCompare = left.textLanguage().compareToIgnoreCase(right.textLanguage());
                        if (textCompare != 0) {
                            return textCompare;
                        }
                        return left.voiceLanguage().compareToIgnoreCase(right.voiceLanguage());
                    })
                    .toList();
            return new MaidStats(maidUuid, maidName, totalEntries, voiceEntries, poolStats);
        }
    }

    private static final class PoolStatsBuilder {
        private final PoolGroupKey key;
        private int totalEntries;
        private int voiceEntries;

        private PoolStatsBuilder(PoolGroupKey key) {
            this.key = key;
        }

        private void add(Entry entry) {
            totalEntries++;
            if (entry.hasVoice()) {
                voiceEntries++;
            }
        }

        private PoolStats build() {
            return new PoolStats(key.pool(), key.textLanguage(), key.voiceLanguage(), totalEntries, voiceEntries);
        }
    }

    private record PoolGroupKey(MorningKissScheduleRules.DialoguePool pool, String textLanguage, String voiceLanguage) {
    }

    private record CacheKey(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
    }
}
