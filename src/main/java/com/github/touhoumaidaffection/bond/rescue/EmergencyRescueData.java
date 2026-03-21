package com.github.touhoumaidaffection.bond.rescue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class EmergencyRescueData {
    private static final String ROOT_KEY = "touhou_maid_affection.emergency_rescue";
    private static final String AVAILABLE_KEY = "available_rescue_maids";
    private static final String LAST_DAY_KEY = "last_replenish_day";

    private EmergencyRescueData() {
    }

    public static long getLastReplenishDay(ServerPlayer player) {
        return getRoot(player).getLong(LAST_DAY_KEY);
    }

    public static void setLastReplenishDay(ServerPlayer player, long day) {
        CompoundTag root = getRoot(player);
        root.putLong(LAST_DAY_KEY, day);
        save(player, root);
    }

    public static List<String> getAvailableMaidModels(ServerPlayer player) {
        List<String> result = new ArrayList<>();
        ListTag list = getRoot(player).getList(AVAILABLE_KEY, Tag.TAG_STRING);
        for (Tag tag : list) {
            result.add(tag.getAsString());
        }
        return result;
    }

    public static void setAvailableMaidModels(ServerPlayer player, List<String> maidModels) {
        CompoundTag root = getRoot(player);
        ListTag list = new ListTag();
        for (String model : maidModels) {
            list.add(StringTag.valueOf(model));
        }
        root.put(AVAILABLE_KEY, list);
        save(player, root);
    }

    public static String consumeOne(ServerPlayer player) {
        List<String> available = getAvailableMaidModels(player);
        if (available.isEmpty()) {
            return "";
        }
        String consumed = available.remove(0);
        setAvailableMaidModels(player, available);
        return consumed;
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
