package com.github.touhoumaidaffection.bond.settings;

import com.github.touhoumaidaffection.network.TmaGiftStatusPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises production packet decoding into the state and projections consumed by the Status card. */
class TmaGiftStatusSelectionIntegrationTest {
    @Test
    void decodedPagesDriveUuidSelectionRefreshAndActualItemTimeProjection() {
        TmaGiftStatusSelection model = new TmaGiftStatusSelection();
        model.refresh();
        List<TmaGiftStatusWire.MaidStatus> first = IntStream.range(0, 16).mapToObj(i -> maid(i, false)).toList();
        assertEquals(1, deliver(model, page(0, 17, first), 1_000_000_000L));
        assertTrue(model.loading());
        deliver(model, page(1, 17, List.of(maid(16, false))), 2_000_000_000L);
        model.select(id(16));
        assertEquals(id(16), model.selected().maid().maidUuid());
        assertEquals(List.of(new TmaGiftStatusWire.Gift("minecraft:apple", 3),
                new TmaGiftStatusWire.Gift("minecraft:bread", 1)),
                TmaGiftStatusSelection.preparedItems(model.selected().maid()));
        assertEquals(9L, TmaGiftStatusSelection.preparationSeconds(model.selected(), 3_000_000_000L));
        assertEquals(4L, TmaGiftStatusSelection.cooldownSeconds(model.selected(), 3_000_000_000L));
        assertEquals("minecraft:carrot", model.selected().maid().lastGiftId());
        assertEquals(900L, model.selected().maid().lastDeliveryAtMs());
        assertEquals(TmaGiftStatusWire.DeliveryState.TOO_FAR, model.selected().maid().state());

        // The same maid moves to the first page; selection is identity, never a row index or name.
        model.refresh();
        List<TmaGiftStatusWire.MaidStatus> reordered = IntStream.range(1, 17)
                .mapToObj(i -> maid(i, i == 16)).toList();
        deliver(model, page(0, 17, reordered), 4_000_000_000L);
        assertEquals(id(16), model.selectedUuid());
        assertEquals(List.of(new TmaGiftStatusWire.Gift("minecraft:diamond", 2)),
                TmaGiftStatusSelection.preparedItems(model.selected().maid()));
        deliver(model, page(1, 17, List.of(maid(0, false))), 8_000_000_000L);
        assertEquals(5L, TmaGiftStatusSelection.preparationSeconds(model.selected(), 9_000_000_000L),
                "A later page must not reset the selected maid's snapshot clock");
        assertEquals("minecraft:bread", model.selected().maid().lastGiftId());
        assertEquals(950L, model.selected().maid().lastDeliveryAtMs());
        assertEquals(0L, TmaGiftStatusSelection.preparationSeconds(model.selected(), 30_000_000_000L));
        assertEquals(950L, model.selected().maid().lastDeliveryAtMs(),
                "An elapsed preparation timer must not invent an actual delivery");
        assertEquals(2, model.selected().maid().preparedCount());
        assertEquals(TmaGiftStatusWire.DeliveryState.TOO_FAR, model.selected().maid().state());

        model.refresh();
        deliver(model, page(0, 1, List.of(maid(3, false))), 40_000_000_000L);
        assertEquals(id(3), model.selectedUuid());
        model.refresh();
        deliver(model, page(0, 0, List.of()), 50_000_000_000L);
        assertTrue(model.complete());
        assertNull(model.selected());
        assertNull(model.selectedUuid());
    }

    private static int deliver(TmaGiftStatusSelection model, TmaGiftStatusWire.Status status, long receivedNanos) {
        ByteBuf bytes = Unpooled.buffer();
        try {
            TmaGiftStatusPayload.STREAM_CODEC.encode(bytes, new TmaGiftStatusPayload(status));
            TmaGiftStatusPayload decoded = TmaGiftStatusPayload.STREAM_CODEC.decode(bytes);
            return model.accept(decoded.status(), receivedNanos);
        } finally {
            bytes.release();
        }
    }

    private static TmaGiftStatusWire.Status page(int page, int total, List<TmaGiftStatusWire.MaidStatus> maids) {
        return new TmaGiftStatusWire.Status(1000L, true, false, true, 100, 20, 7, page, total, maids);
    }

    private static String id(int index) { return new UUID(0L, index + 1L).toString(); }
    private static TmaGiftStatusWire.MaidStatus maid(int index, boolean updated) {
        List<TmaGiftStatusWire.Gift> gifts = updated
                ? List.of(new TmaGiftStatusWire.Gift("minecraft:diamond", 2))
                : List.of(new TmaGiftStatusWire.Gift("minecraft:apple", 1),
                        new TmaGiftStatusWire.Gift("minecraft:bread", 1),
                        new TmaGiftStatusWire.Gift("minecraft:apple", 2));
        return new TmaGiftStatusWire.MaidStatus(id(index), "Same name", updated ? 2 : 4, updated ? 2 : 4,
                gifts, 11_000L, updated ? 950L : 900L, updated ? "minecraft:bread" : "minecraft:carrot",
                TmaGiftStatusWire.DeliveryState.TOO_FAR, 5);
    }
}
