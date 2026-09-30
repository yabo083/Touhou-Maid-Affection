package com.github.touhoumaidaffection.bond.settings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TmaGiftStatusSelectionTest {
    @Test
    void laterPageSelectionSurvivesRefreshUntilItsUuidArrives() {
        TmaGiftStatusSelection model = new TmaGiftStatusSelection();
        model.refresh();
        assertTrue(model.loading());
        assertNull(model.selected());
        assertEquals(1, model.accept(page(0, 17, maids(0, 16)), 0L));
        assertTrue(model.loading());
        assertEquals(-1, model.accept(page(1, 17, maids(16, 17)), 0L));
        model.select(id(16));
        assertEquals(id(16), model.selected().maid().maidUuid());

        model.refresh();
        assertNull(model.selected(), "Do not display the previous snapshot as fresh");
        model.accept(page(0, 17, maids(0, 16)), 100L);
        assertEquals(id(16), model.selectedUuid());
        assertNull(model.selected(), "Do not silently select the first page during loading");
        model.accept(page(1, 17, maids(16, 17)), 200L);
        assertTrue(model.complete());
        assertEquals(id(16), model.selected().maid().maidUuid());
        assertEquals(200L, model.selected().receivedNanos());
    }

    @Test
    void removalFallsBackOnlyAfterCompletionAndEmptyClearsSelection() {
        TmaGiftStatusSelection model = new TmaGiftStatusSelection();
        model.refresh();
        model.accept(page(0, 2, maids(0, 2)), 0L);
        model.select(id(1));
        model.refresh();
        model.accept(page(0, 1, maids(0, 1)), 0L);
        assertEquals(id(0), model.selectedUuid());
        model.refresh();
        model.accept(page(0, 0, List.of()), 0L);
        assertTrue(model.complete());
        assertFalse(model.loading());
        assertNull(model.selectedUuid());
        assertNull(model.selected());
        assertEquals(List.of(), model.options());
    }

    @Test
    void incompleteScanKeepsSelectionIntentAndSessionClearDiscardsEverything() {
        TmaGiftStatusSelection model = new TmaGiftStatusSelection();
        model.refresh();
        model.accept(page(0, 2, maids(0, 2)), 0L);
        model.select(id(1));
        model.refresh();
        model.accept(page(0, 17, maids(2, 18)), 0L);
        model.fail();
        assertFalse(model.complete());
        assertFalse(model.loading());
        assertEquals(id(1), model.selectedUuid());
        assertNull(model.selected());
        assertEquals(-1, model.accept(page(1, 17, maids(1, 2)), 0L));
        assertNull(model.selected(), "A late reply must not revive a failed scan");
        model.clear();
        assertNull(model.selectedUuid());
        assertEquals(List.of(), model.options());
        model.refresh();
        model.accept(page(0, 1, maids(5, 6)), 0L);
        assertEquals(id(5), model.selectedUuid());
    }

    @Test
    void pageWalkIsBoundedByInitialCountAndRejectsOutOfOrderPages() {
        TmaGiftStatusSelection model = new TmaGiftStatusSelection();
        model.refresh();
        assertEquals(-1, model.accept(page(1, 32, maids(16, 32)), 0L));
        assertEquals(List.of(), model.options());
        assertEquals(1, model.accept(page(0, 17, maids(0, 16)), 0L));
        assertEquals(-1, model.accept(page(0, 17, maids(0, 16)), 0L));
        assertTrue(model.loading());
        assertEquals(-1, model.accept(page(1, 100, maids(16, 32)), 0L));
        assertTrue(model.complete(), "New arrivals must not prolong a snapshot scan indefinitely");
        assertEquals(32, model.options().size());
    }

    @Test
    void clampedReplyFinishesAfterMaidRemovalWithoutDuplicateOptions() {
        TmaGiftStatusSelection model = new TmaGiftStatusSelection();
        model.refresh();
        model.accept(page(0, 17, maids(0, 16)), 0L);
        model.select(id(15));
        model.accept(page(0, 15, maids(0, 15)), 100L);
        assertTrue(model.complete());
        assertEquals(15, model.options().size());
        assertEquals(id(0), model.selectedUuid());
        assertEquals(100L, model.selected().receivedNanos());
    }

    private static String id(int index) { return new UUID(0L, index + 1L).toString(); }
    private static List<TmaGiftStatusWire.MaidStatus> maids(int start, int end) {
        return IntStream.range(start, end).mapToObj(i -> new TmaGiftStatusWire.MaidStatus(id(i), "Same name",
                1, 1, List.of(new TmaGiftStatusWire.Gift("minecraft:apple", 1)), 2000L, 0L, "",
                TmaGiftStatusWire.DeliveryState.TOO_FAR, 0)).toList();
    }
    private static TmaGiftStatusWire.Status page(int page, int total, List<TmaGiftStatusWire.MaidStatus> maids) {
        return new TmaGiftStatusWire.Status(1000L, true, false, true, 100, 20, 7, page, total, maids);
    }
}
