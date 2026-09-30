package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGiftPolicyTest {
    @Test
    void explicitTagsOverrideModesAndDefaultExclusionsButNeverBlacklistOrAir() {
        assertFalse(RandomGiftPolicy.allows("minecraft:apple", false, false, true, true, 8));
        assertTrue(RandomGiftPolicy.allows("minecraft:apple", false, false, false, false, 0));
        assertFalse(RandomGiftPolicy.allows("example:tea", false, false, false, false, 8));
        assertFalse(RandomGiftPolicy.allows("example:tea", false, false, false, true, 0));
        assertTrue(RandomGiftPolicy.allows("example:tea", true, false, true, false, 0));
        assertFalse(RandomGiftPolicy.allows("example:tea", true, true, false, true, 8));
        assertTrue(RandomGiftPolicy.allows("minecraft:command_block", true, false, false, true, 8));
        assertFalse(RandomGiftPolicy.allows("minecraft:command_block", false, false, false, true, 8));
        assertFalse(RandomGiftPolicy.allows("minecraft:command_block", true, true, false, true, 8));
        assertFalse(RandomGiftPolicy.allows("minecraft:air", true, false, false, true, 8));
    }

    @Test
    void freshSamplesCanReachPreviouslyOmittedModItems() {
        var seen = new java.util.HashSet<String>();
        for (int seed = 0; seed < 100; seed++) {
            var groups = new java.util.LinkedHashMap<String, java.util.List<String>>();
            groups.put("a", new java.util.ArrayList<>(java.util.List.of("a:one", "a:two", "a:three")));
            groups.put("b", new java.util.ArrayList<>(java.util.List.of("b:one", "b:two", "b:three")));
            var sample = new java.util.LinkedHashSet<String>();
            RandomGiftPolicy.addSampled(groups, sample, 2, new java.util.Random(seed)::nextInt);
            org.junit.jupiter.api.Assertions.assertEquals(2, sample.size());
            org.junit.jupiter.api.Assertions.assertEquals(1, sample.stream().filter(id -> id.startsWith("a:")).count());
            org.junit.jupiter.api.Assertions.assertEquals(1, sample.stream().filter(id -> id.startsWith("b:")).count());
            seen.addAll(sample);
        }
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of("a:one", "a:two", "a:three", "b:one", "b:two", "b:three"), seen);
    }

    @Test
    void sampleCapAndExhaustionNeverDuplicateExplicitCandidates() {
        var groups = new java.util.LinkedHashMap<String, java.util.List<String>>();
        groups.put("a", new java.util.ArrayList<>(java.util.List.of("a:one", "a:two")));
        var target = new java.util.LinkedHashSet<>(java.util.List.of("a:one"));
        RandomGiftPolicy.addSampled(groups, target, 0, bound -> 0);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of("a:one"), target);
        RandomGiftPolicy.addSampled(groups, target, 64, bound -> 0);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of("a:one", "a:two"), target);
    }

    @Test
    void legacyAutomaticPoolRejectsOnlyImmersionBreakingTechnicalItems() {
        assertTrue(RandomGiftPolicy.isExcludedDefaultGift("minecraft:command_block"));
        assertFalse(RandomGiftPolicy.isExcludedDefaultGift("minecraft:bedrock"));
        assertFalse(RandomGiftPolicy.isExcludedDefaultGift("minecraft:creeper_spawn_egg"));
        assertFalse(RandomGiftPolicy.isExcludedDefaultGift("minecraft:apple"));
    }
}
