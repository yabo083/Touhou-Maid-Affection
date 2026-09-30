package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomGiftClockTest {
    @Test
    void elapsedIntervalsAccumulateAndKeepFractionalRemainder() {
        var state = RandomGiftClock.reconcile(1, 1_000L, 1, 1, 7, 151_000L);
        assertEquals(new RandomGiftClock.State(3, 121_000L, 181_000L), state);
        assertEquals(state, RandomGiftClock.reconcile(state.queued(), state.lastWallClockMs(), 1, 1, 7, 151_000L));
    }

    @Test
    void reachingCapDropsBankedTimeButShrinkingCapDoesNotDestroyEarnedGifts() {
        assertEquals(new RandomGiftClock.State(7, 601_000L, 0L),
                RandomGiftClock.reconcile(6, 1_000L, 1, 1, 7, 601_000L));
        var shrunk = RandomGiftClock.reconcile(7, 1_000L, 1, 1, 2, 601_000L);
        assertEquals(new RandomGiftClock.State(7, 601_000L, 0L), shrunk);
        assertEquals(new RandomGiftClock.State(1, 601_000L, 661_000L),
                RandomGiftClock.reconcile(1, shrunk.lastWallClockMs(), 1, 1, 2, 601_000L));
    }

    @Test
    void changedIntervalAndMissingClockStartFreshWithoutRetroactiveEarning() {
        assertEquals(new RandomGiftClock.State(2, 901_000L, 1_021_000L),
                RandomGiftClock.reconcile(2, 1_000L, 1, 2, 7, 901_000L));
        assertEquals(new RandomGiftClock.State(2, 901_000L, 961_000L),
                RandomGiftClock.reconcile(2, 0L, 0, 1, 7, 901_000L));
    }

    @Test
    void backwardsClockDoesNotEarnAndVeryLongOfflineTimeCannotOverflowQueue() {
        assertEquals(new RandomGiftClock.State(1, 100_000L, 160_000L),
                RandomGiftClock.reconcile(1, 100_000L, 1, 1, 7, 1_000L));
        assertEquals(64, RandomGiftClock.reconcile(63, 1L, 1, 1, 64, Long.MAX_VALUE / 2).queued());
    }
}
