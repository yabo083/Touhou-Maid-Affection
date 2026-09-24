package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TmaAiStatusWireTest {
    @Test
    void statusRoundTripsThroughTheCodec() {
        TmaAiStatusWire.Status status = new TmaAiStatusWire.Status(
                true, true, false, false,
                "zh_cn", "ja_jp", "tlm", "inherit",
                4, 1200, true,
                24, 18, 3, 1, 42L, true,
                List.of(
                        new TmaAiStatusWire.MaidStatus("uuid-1", "芙兰朵露", 9, 4, List.of(
                                new TmaAiStatusWire.PoolStatus("morning", 5, 4),
                                new TmaAiStatusWire.PoolStatus("evening", 4, 2)
                        )),
                        new TmaAiStatusWire.MaidStatus("uuid-2", "十六夜咲夜", 0, 4, List.of())
                )
        );

        MemoryBuffer buffer = new MemoryBuffer();
        TmaAiStatusWire.writeStatus(buffer, status);

        assertEquals(status, TmaAiStatusWire.readStatus(buffer));
    }

    @Test
    void statusDerivesTextOnlyCounts() {
        TmaAiStatusWire.Status status = new TmaAiStatusWire.Status(
                true, true, true, true, "auto", "auto", "tlm", "inherit",
                4, 200, false, 10, 6, 2, 0, 7L, false, List.of()
        );

        assertEquals(4, status.textOnlyEntries());

        TmaAiStatusWire.MaidStatus maid = new TmaAiStatusWire.MaidStatus("u", "n", 10, 4, List.of(
                new TmaAiStatusWire.PoolStatus("morning", 6, 4),
                new TmaAiStatusWire.PoolStatus("evening", 4, 2)
        ));
        assertEquals(6, maid.voiceEntries());
        assertEquals(4, maid.textOnlyEntries());
        assertEquals(1, new TmaAiStatusWire.PoolStatus("morning", 6, 5).textOnlyEntries());
    }

    @Test
    void clearRequestRoundTripsEveryScope() {
        for (TmaAiStatusWire.ClearRequest request : List.of(
                new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.ALL, "", ""),
                new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.MAID, "uuid-1", ""),
                new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.POOL, "uuid-1", "morning")
        )) {
            MemoryBuffer buffer = new MemoryBuffer();
            TmaAiStatusWire.writeClear(buffer, request);
            assertEquals(request, TmaAiStatusWire.readClear(buffer));
        }
    }

    @Test
    void clearRequestDefaultsNullScopeToAll() {
        TmaAiStatusWire.ClearRequest request = new TmaAiStatusWire.ClearRequest(null, null, null);
        assertEquals(TmaAiStatusWire.ClearScope.ALL, request.scope());
        assertEquals("", request.maidUuid());
        assertEquals("", request.pool());
        assertNull(TmaAiStatusWire.ClearScope.byName("nope"));
        assertEquals(TmaAiStatusWire.ClearScope.POOL, TmaAiStatusWire.ClearScope.byName("pool"));
    }

    @Test
    void decodeClampsHostileMaidAndPoolCounts() {
        MemoryBuffer buffer = new MemoryBuffer();
        // 17 scalar fields before the maid list.
        buffer.writeBoolean(true);
        buffer.writeBoolean(true);
        buffer.writeBoolean(true);
        buffer.writeBoolean(true);
        buffer.writeString("auto");
        buffer.writeString("auto");
        buffer.writeString("tlm");
        buffer.writeString("inherit");
        buffer.writeInt(4);
        buffer.writeInt(200);
        buffer.writeBoolean(false);
        buffer.writeInt(0);
        buffer.writeInt(0);
        buffer.writeInt(0);
        buffer.writeInt(0);
        buffer.writeLong(1L);
        buffer.writeBoolean(false);
        // Hostile counts: claim 4096 maids of 4096 pools each. The decoder must stop at the protocol
        // limits, so only that many maids/pools need to be written.
        buffer.writeInt(4096);
        for (int maid = 0; maid < TmaAiStatusWire.MAX_MAIDS; maid++) {
            buffer.writeString("u" + maid);
            buffer.writeString("n" + maid);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(4096);
            for (int pool = 0; pool < TmaAiStatusWire.MAX_POOLS; pool++) {
                buffer.writeString("p" + pool);
                buffer.writeInt(0);
                buffer.writeInt(0);
            }
        }

        TmaAiStatusWire.Status status = TmaAiStatusWire.readStatus(buffer);
        assertEquals(TmaAiStatusWire.MAX_MAIDS, status.maids().size());
        assertEquals(TmaAiStatusWire.MAX_POOLS, status.maids().get(0).pools().size());
    }

    @Test
    void sanitizeTruncatesMaidsAndPools() {
        List<TmaAiStatusWire.MaidStatus> maids = new ArrayList<>();
        for (int index = 0; index < TmaAiStatusWire.MAX_MAIDS + 5; index++) {
            List<TmaAiStatusWire.PoolStatus> pools = new ArrayList<>();
            for (int pool = 0; pool < TmaAiStatusWire.MAX_POOLS + 3; pool++) {
                pools.add(new TmaAiStatusWire.PoolStatus("p" + pool, 1, 0));
            }
            maids.add(new TmaAiStatusWire.MaidStatus("u" + index, "n" + index, 1, 4, pools));
        }
        maids.add(null);

        List<TmaAiStatusWire.MaidStatus> sanitized = TmaAiStatusWire.sanitizeMaids(maids);
        assertEquals(TmaAiStatusWire.MAX_MAIDS, sanitized.size());
        assertEquals(TmaAiStatusWire.MAX_POOLS, sanitized.get(0).pools().size());
        assertTrue(TmaAiStatusWire.sanitizeMaids(null).isEmpty());
        assertTrue(TmaAiStatusWire.sanitizeMaids(List.of()).isEmpty());
    }

    /** In-memory stand-in for the netty/Minecraft byte buffer used on the wire. */
    private static final class MemoryBuffer implements TmaAiStatusWire.Sink, TmaAiStatusWire.Source {
        private final List<String> tokens = new ArrayList<>();
        private int cursor;

        @Override
        public void writeInt(int value) {
            tokens.add("i" + value);
        }

        @Override
        public void writeLong(long value) {
            tokens.add("l" + value);
        }

        @Override
        public void writeBoolean(boolean value) {
            tokens.add("b" + value);
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
        public long readLong() {
            return Long.parseLong(tokens.get(cursor++).substring(1));
        }

        @Override
        public boolean readBoolean() {
            return Boolean.parseBoolean(tokens.get(cursor++).substring(1));
        }

        @Override
        public String readString() {
            return tokens.get(cursor++).substring(1);
        }
    }
}