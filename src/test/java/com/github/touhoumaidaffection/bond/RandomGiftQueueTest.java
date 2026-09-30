package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RandomGiftQueueTest {
    @Test
    void legacySlotsRemainEarnedUntilActuallyPreparedAndDelivered() {
        var legacy = new RandomGiftQueue(3, List.of());
        assertEquals(3, legacy.consumeFirst().queued());
        var prepared = new RandomGiftQueue(legacy.queued(), List.of("minecraft:apple", "minecraft:bread"));
        var delivered = prepared.consumeFirst();
        assertEquals(2, delivered.queued());
        assertEquals(List.of("minecraft:bread"), delivered.prepared());
        assertEquals(1, delivered.consumeFirst().queued());
        assertEquals(1, delivered.consumeFirst().consumeFirst().queued());
    }

    @Test
    void removalOrNewBlacklistInvalidatesContentsNotEarnedSlots() {
        var queue = new RandomGiftQueue(3, List.of("gone:item", "minecraft:apple", "minecraft:bread"));
        var retained = queue.retain(id -> !id.equals("gone:item"));
        assertEquals(3, retained.queued());
        assertEquals(List.of("minecraft:apple", "minecraft:bread"), retained.prepared());
        assertEquals(new RandomGiftQueue(3, List.of()), retained.retain(id -> false));
    }

    @Test
    void malformedAndOversizedDataCannotCreateExtraGifts() {
        assertEquals(new RandomGiftQueue(0, List.of()), new RandomGiftQueue(-1, List.of("minecraft:apple")));
        var tooMany = new RandomGiftQueue(Integer.MAX_VALUE, java.util.Collections.nCopies(100, "minecraft:apple"));
        assertEquals(64, tooMany.queued());
        assertEquals(64, tooMany.prepared().size());
        var invalid = new RandomGiftQueue(3, List.of("bad", "UPPER:item", "x:" + "a".repeat(256), "minecraft:bread"));
        assertEquals(3, invalid.queued());
        assertEquals(List.of("minecraft:bread"), invalid.prepared());
    }

    @Test
    void explicitQueueReductionKeepsFifoPrefixRatherThanDuplicatingConsumedHead() {
        var queue = new RandomGiftQueue(3, List.of("minecraft:apple", "minecraft:bread", "minecraft:carrot"));
        assertEquals(List.of("minecraft:apple"), new RandomGiftQueue(1, queue.prepared()).prepared());
        assertEquals(List.of("minecraft:bread", "minecraft:carrot"), queue.consumeFirst().prepared());
    }
}
