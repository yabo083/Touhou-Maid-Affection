package com.github.touhoumaidaffection.bond.service;

import java.util.Set;

final class RandomGiftPolicy {
    private static final Set<String> EXCLUDED_DEFAULT_GIFTS = Set.of(
            "minecraft:air",
            "minecraft:barrier",
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

    static boolean isExcludedDefaultGift(String itemId) {
        return itemId == null || EXCLUDED_DEFAULT_GIFTS.contains(itemId);
    }

    static boolean allows(String itemId, boolean explicit, boolean blacklisted,
                          boolean curatedOnly, boolean includeMods, int sampleSize) {
        if (blacklisted || itemId == null || "minecraft:air".equals(itemId)) return false;
        return explicit || (!isExcludedDefaultGift(itemId) && !curatedOnly
                && (itemId.startsWith("minecraft:") || (includeMods && sampleSize > 0)));
    }

    /** Namespace-balanced sample, randomized afresh for each preparation batch. */
    static <T> void addSampled(java.util.Map<String, java.util.List<T>> groups, java.util.Set<T> target,
                               int sampleSize, java.util.function.IntUnaryOperator nextInt) {
        var namespaces = new java.util.ArrayList<>(groups.keySet());
        shuffle(namespaces, nextInt);
        for (var items : groups.values()) shuffle(items, nextInt);
        int added = 0;
        for (int round = 0; added < sampleSize; round++) {
            boolean progressed = false;
            for (String namespace : namespaces) {
                var items = groups.get(namespace);
                if (round >= items.size()) continue;
                progressed = true;
                if (target.add(items.get(round))) added++;
                if (added >= sampleSize) break;
            }
            if (!progressed) break;
        }
    }

    private static <T> void shuffle(java.util.List<T> items, java.util.function.IntUnaryOperator nextInt) {
        for (int i = items.size() - 1; i > 0; i--) {
            int index = nextInt.applyAsInt(i + 1);
            T previous = items.set(i, items.get(index));
            items.set(index, previous);
        }
    }

}
