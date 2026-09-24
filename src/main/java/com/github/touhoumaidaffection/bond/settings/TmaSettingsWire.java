package com.github.touhoumaidaffection.bond.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-logic codec for the settings channel payloads.
 *
 * <p>The byte-level transport (netty {@code ByteBuf}, Minecraft {@code ByteBufCodecs}) lives in
 * {@code network/TmaSettingsByteBuf}; this class owns the entry-list structure itself, so the
 * encode/decode round trip can be exercised by plain JUnit tests without any Minecraft class on the
 * classpath.
 */
public final class TmaSettingsWire {
    /** Hard cap on the number of entries carried by one packet. */
    public static final int MAX_ENTRIES = 32;

    private TmaSettingsWire() {
    }

    /** One logical setting key together with its (already canonicalised) string value. */
    public record Entry(String key, String value) {
    }

    /** Minimal byte sink used to keep the codec transport agnostic. */
    public interface Sink {
        void writeInt(int value);

        void writeString(String value);
    }

    /** Minimal byte source used to keep the codec transport agnostic. */
    public interface Source {
        int readInt();

        String readString();
    }

    public static void writeEntries(Sink sink, List<Entry> entries) {
        List<Entry> safeEntries = sanitize(entries);
        sink.writeInt(safeEntries.size());
        for (Entry entry : safeEntries) {
            sink.writeString(entry.key());
            sink.writeString(entry.value());
        }
    }

    public static List<Entry> readEntries(Source source) {
        int count = Math.max(0, Math.min(source.readInt(), MAX_ENTRIES));
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String key = source.readString();
            String value = source.readString();
            entries.add(new Entry(key == null ? "" : key, value == null ? "" : value));
        }
        return List.copyOf(entries);
    }

    /** Drops {@code null} entries and truncates the list to {@link #MAX_ENTRIES}. */
    public static List<Entry> sanitize(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>(Math.min(entries.size(), MAX_ENTRIES));
        for (Entry entry : entries) {
            if (result.size() >= MAX_ENTRIES) {
                break;
            }
            if (entry == null || entry.key() == null || entry.value() == null) {
                continue;
            }
            result.add(entry);
        }
        return List.copyOf(result);
    }
}