package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TmaSettingsWireTest {
    @Test
    void entriesRoundTripThroughTheCodec() {
        List<TmaSettingsWire.Entry> entries = List.of(
                new TmaSettingsWire.Entry("morning_kiss.enabled", "true"),
                new TmaSettingsWire.Entry("morning_kiss.display_language", "zh_cn"),
                new TmaSettingsWire.Entry("emergency_rescue.enabled", "false")
        );

        MemoryBuffer buffer = new MemoryBuffer();
        TmaSettingsWire.writeEntries(buffer, entries);

        assertEquals(entries, TmaSettingsWire.readEntries(buffer));
    }

    @Test
    void emptyEntryListRoundTrips() {
        MemoryBuffer buffer = new MemoryBuffer();
        TmaSettingsWire.writeEntries(buffer, List.of());

        assertTrue(TmaSettingsWire.readEntries(buffer).isEmpty());
        assertEquals(1, buffer.tokenCount());
    }

    @Test
    void statePayloadShapeRoundTripsEntriesAndPermissionFlag() {
        List<TmaSettingsWire.Entry> entries = List.of(
                new TmaSettingsWire.Entry("maid_prayer_buff.enabled", "true"),
                new TmaSettingsWire.Entry("morning_kiss.ai_dialogue_language", "tlm")
        );

        MemoryBuffer buffer = new MemoryBuffer();
        TmaSettingsWire.writeEntries(buffer, entries);
        buffer.writeInt(1);

        assertEquals(entries, TmaSettingsWire.readEntries(buffer));
        assertEquals(1, buffer.readInt());
    }

    @Test
    void codecCapsTheEntryCountAtTheProtocolLimit() {
        List<TmaSettingsWire.Entry> oversized = new ArrayList<>();
        for (int index = 0; index < TmaSettingsWire.MAX_ENTRIES + 5; index++) {
            oversized.add(new TmaSettingsWire.Entry("key." + index, "value." + index));
        }

        MemoryBuffer buffer = new MemoryBuffer();
        TmaSettingsWire.writeEntries(buffer, oversized);

        List<TmaSettingsWire.Entry> decoded = TmaSettingsWire.readEntries(buffer);
        assertEquals(TmaSettingsWire.MAX_ENTRIES, decoded.size());
        assertEquals("key.0", decoded.get(0).key());
        assertEquals("key." + (TmaSettingsWire.MAX_ENTRIES - 1), decoded.get(TmaSettingsWire.MAX_ENTRIES - 1).key());
    }

    @Test
    void decodeClampsAHostileEntryCount() {
        MemoryBuffer buffer = new MemoryBuffer();
        buffer.writeInt(4096);
        for (int index = 0; index < 4096; index++) {
            buffer.writeString("key." + index);
            buffer.writeString("value." + index);
        }

        assertEquals(TmaSettingsWire.MAX_ENTRIES, TmaSettingsWire.readEntries(buffer).size());
    }

    @Test
    void sanitizeDropsNullEntriesAndTruncates() {
        List<TmaSettingsWire.Entry> entries = new ArrayList<>();
        entries.add(new TmaSettingsWire.Entry("morning_kiss.enabled", "true"));
        entries.add(null);
        entries.add(new TmaSettingsWire.Entry(null, "true"));
        entries.add(new TmaSettingsWire.Entry("morning_kiss.auto_enabled", null));

        assertEquals(List.of(new TmaSettingsWire.Entry("morning_kiss.enabled", "true")), TmaSettingsWire.sanitize(entries));
        assertTrue(TmaSettingsWire.sanitize(null).isEmpty());
        assertTrue(TmaSettingsWire.sanitize(List.of()).isEmpty());
    }

    /** In-memory stand-in for the netty/Minecraft byte buffer used on the wire. */
    private static final class MemoryBuffer implements TmaSettingsWire.Sink, TmaSettingsWire.Source {
        private final List<String> tokens = new ArrayList<>();
        private int cursor;

        @Override
        public void writeInt(int value) {
            tokens.add("i" + value);
        }

        @Override
        public void writeString(String value) {
            tokens.add("s" + value);
        }

        @Override
        public int readInt() {
            return Integer.parseInt(tokens.get(cursor++).substring(1));
        }

        @Override
        public String readString() {
            return tokens.get(cursor++).substring(1);
        }

        int tokenCount() {
            return tokens.size();
        }
    }
}