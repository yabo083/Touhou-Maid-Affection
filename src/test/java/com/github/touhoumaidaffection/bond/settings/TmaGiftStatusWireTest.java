package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TmaGiftStatusWireTest {
    private static TmaGiftStatusWire.Status sample() {
        return new TmaGiftStatusWire.Status(1_800_000_000_000L, true, false, true,
                1234, 30, 7, 1, 17, List.of(new TmaGiftStatusWire.MaidStatus(
                "00000000-0000-0000-0000-000000000001", "咲夜", 5, 1,
                List.of(new TmaGiftStatusWire.Gift("minecraft:diamond", 3)),
                1_800_000_060_000L, 1_799_999_000_000L, "minecraft:apple",
                TmaGiftStatusWire.DeliveryState.COOLDOWN, 12)));
    }

    @Test
    void preservesOwnerRowsItemsAndServerTimes() {
        Buffer buffer = encoded();
        assertEquals(sample(), TmaGiftStatusWire.readStatus(buffer));
        assertEquals(buffer.values.size(), buffer.cursor);
    }

    @Test
    void rejectsMalformedCollectionLengthsBeforeReadingEntries() {
        for (int length : new int[]{-1, 17, Integer.MAX_VALUE}) {
            Buffer buffer = encoded();
            buffer.values.set(9, length);
            assertThrows(IllegalArgumentException.class, () -> TmaGiftStatusWire.readStatus(buffer));
            assertEquals(10, buffer.cursor);
        }
        for (int length : new int[]{-1, 65, Integer.MAX_VALUE}) {
            Buffer buffer = encoded();
            buffer.values.set(14, length);
            assertThrows(IllegalArgumentException.class, () -> TmaGiftStatusWire.readStatus(buffer));
            assertEquals(15, buffer.cursor);
        }
    }

    @Test
    void rejectsOversizedNamesUnknownStatesAndInvalidValues() {
        for (Object[] mutation : List.of(new Object[]{11, "x".repeat(257)},
                new Object[]{15, "x".repeat(257)}, new Object[]{20, 10}, new Object[]{20, -1},
                new Object[]{5, 0}, new Object[]{5, 1441}, new Object[]{6, 65},
                new Object[]{12, -1}, new Object[]{13, 65}, new Object[]{16, 0},
                new Object[]{21, -1}, new Object[]{7, -1})) {
            Buffer buffer = encoded();
            buffer.values.set((Integer) mutation[0], mutation[1]);
            assertThrows(IllegalArgumentException.class, () -> TmaGiftStatusWire.readStatus(buffer));
        }
    }

    @Test
    void handlesMaximumPageAndCollectionBoundsWithoutLosingValues() {
        String longest = "a".repeat(256);
        TmaGiftStatusWire.Gift gift = new TmaGiftStatusWire.Gift(longest, 64);
        TmaGiftStatusWire.MaidStatus maid = new TmaGiftStatusWire.MaidStatus(longest, longest,
                64, 64, Collections.nCopies(64, gift), 0, 0, longest,
                TmaGiftStatusWire.DeliveryState.READY, 0);
        TmaGiftStatusWire.Status status = new TmaGiftStatusWire.Status(123, false, true, false,
                0, 1440, 64, TmaGiftStatusWire.MAX_PAGE, Integer.MAX_VALUE, Collections.nCopies(16, maid));
        Buffer buffer = new Buffer();
        TmaGiftStatusWire.writeStatus(buffer, status);
        assertEquals(status, TmaGiftStatusWire.readStatus(buffer));
        assertEquals(TmaGiftStatusWire.MAX_PAGE, TmaGiftStatusWire.validatePage(TmaGiftStatusWire.MAX_PAGE));
        assertThrows(IllegalArgumentException.class, () -> TmaGiftStatusWire.validatePage(-1));
        assertThrows(IllegalArgumentException.class, () -> TmaGiftStatusWire.validatePage(TmaGiftStatusWire.MAX_PAGE + 1));
        assertThrows(IllegalArgumentException.class, () -> new TmaGiftStatusWire.Status(1, true, false,
                false, 1, 1, 1, 0, 17, Collections.nCopies(17, maid)));
        assertThrows(IllegalArgumentException.class, () -> new TmaGiftStatusWire.MaidStatus("u", "n", 1, 1,
                Collections.nCopies(65, gift), 0, 0, "", TmaGiftStatusWire.DeliveryState.READY, 0));
    }

    private static Buffer encoded() {
        Buffer buffer = new Buffer();
        TmaGiftStatusWire.writeStatus(buffer, sample());
        return buffer;
    }

    private static final class Buffer implements TmaGiftStatusWire.Sink, TmaGiftStatusWire.Source {
        final List<Object> values = new ArrayList<>();
        int cursor;
        public void writeInt(int value) { values.add(value); }
        public void writeLong(long value) { values.add(value); }
        public void writeBoolean(boolean value) { values.add(value); }
        public void writeString(String value) { values.add(value); }
        public int readInt() { return (Integer) values.get(cursor++); }
        public long readLong() { return (Long) values.get(cursor++); }
        public boolean readBoolean() { return (Boolean) values.get(cursor++); }
        public String readString() { return (String) values.get(cursor++); }
    }
}
