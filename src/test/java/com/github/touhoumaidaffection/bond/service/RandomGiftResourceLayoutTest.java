package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGiftResourceLayoutTest {
    @Test
    void usesTheForge1201PluralItemTagDirectory() {
        Path dataRoot = Path.of("src", "main", "resources", "data", "touhou_maid_affection", "tags");

        assertTrue(Files.exists(dataRoot.resolve("items/bond_random_gift_pool.json")));
        assertTrue(Files.exists(dataRoot.resolve("items/bond_random_gift_blacklist.json")));
        assertFalse(Files.exists(dataRoot.resolve("item/bond_random_gift_pool.json")));
    }
}