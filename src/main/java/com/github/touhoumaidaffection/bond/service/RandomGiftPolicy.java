package com.github.touhoumaidaffection.bond.service;

import java.util.Set;

final class RandomGiftPolicy {
    private static final Set<String> EXCLUDED_DEFAULT_GIFTS = Set.of(
            "minecraft:air",
            "minecraft:barrier",
            "minecraft:bedrock",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:command_block_minecart",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:light",
            "minecraft:debug_stick",
            "minecraft:knowledge_book"
    );

    private RandomGiftPolicy() {
    }

    static boolean includeAutomaticRegistryCandidates(boolean curatedPoolOnly) {
        return !curatedPoolOnly;
    }

    static boolean isExcludedDefaultGift(String itemId, boolean spawnEgg) {
        return spawnEgg || itemId == null || EXCLUDED_DEFAULT_GIFTS.contains(itemId);
    }
}
