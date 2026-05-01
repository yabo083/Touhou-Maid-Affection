package com.github.touhoumaidaffection.bond.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final int MAX_LINE_LENGTH = 48;
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
        if (value.length() > MAX_LINE_LENGTH) {
            return "";
        }
        if (value.isBlank()) {
            return "";
        }
        return value;
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

    record Entry(String text, String ttsText, String voiceFileName, byte[] voiceData) {
        Entry {
            text = text == null ? "" : text.trim();
            ttsText = ttsText == null || ttsText.isBlank() ? text : ttsText.trim();
            voiceFileName = voiceFileName == null ? "" : voiceFileName.trim();
            voiceData = voiceData == null ? new byte[0] : voiceData;
        }

        boolean hasVoice() {
            return !voiceFileName.isBlank() && voiceData.length > 0;
        }

        boolean hasText() {
            return !text.isBlank();
        }
    }

    private record CacheKey(UUID maidUuid, MorningKissScheduleRules.DialoguePool pool) {
    }
}
