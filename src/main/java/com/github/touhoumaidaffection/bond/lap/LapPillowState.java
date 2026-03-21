package com.github.touhoumaidaffection.bond.lap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class LapPillowState {
    private static final String ROOT_KEY = "touhou_maid_affection.lap_pillow";

    private LapPillowState() {
    }

    public static boolean isActive(ServerPlayer player) {
        return getRoot(player).getBoolean("active");
    }

    public static UUID getMaidUuid(ServerPlayer player) {
        String raw = getRoot(player).getString("maid_uuid");
        if (raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static void activate(ServerPlayer player, UUID maidUuid, long gameTime) {
        CompoundTag root = getRoot(player);
        root.putBoolean("active", true);
        root.putString("maid_uuid", maidUuid.toString());
        root.putLong("start_time", gameTime);
        save(player, root);
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(ROOT_KEY);
    }

    private static CompoundTag getRoot(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return persistent.getCompound(ROOT_KEY);
    }

    private static void save(ServerPlayer player, CompoundTag root) {
        player.getPersistentData().put(ROOT_KEY, root);
    }
}
