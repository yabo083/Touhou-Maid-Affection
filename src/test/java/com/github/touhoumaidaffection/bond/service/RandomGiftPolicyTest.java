package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGiftPolicyTest {
    @Test
    void curatedPoolDoesNotIncludeAutomaticRegistryCandidates() {
        assertFalse(RandomGiftPolicy.includeAutomaticRegistryCandidates(true));
        assertTrue(RandomGiftPolicy.includeAutomaticRegistryCandidates(false));
    }

    @Test
    void legacyAutomaticPoolStillRejectsDangerousItems() {
        assertTrue(RandomGiftPolicy.isExcludedDefaultGift("minecraft:bedrock", false));
        assertTrue(RandomGiftPolicy.isExcludedDefaultGift("minecraft:creeper_spawn_egg", true));
        assertFalse(RandomGiftPolicy.isExcludedDefaultGift("minecraft:apple", false));
    }
}
