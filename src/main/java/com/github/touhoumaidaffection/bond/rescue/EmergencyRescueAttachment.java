package com.github.touhoumaidaffection.bond.rescue;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EmergencyRescueAttachment implements INBTSerializable<CompoundTag> {
    private static final String AVAILABLE_KEY = "available_rescue_maids";
    private static final String REGISTERED_KEY = "registered_rescue_maids";
    private static final String LAST_DAY_KEY = "last_replenish_day";

    private long lastReplenishDay;
    private final List<String> availableRescuers = new ArrayList<>();
    private final List<String> registeredRescuers = new ArrayList<>();

    public long getLastReplenishDay() {
        return lastReplenishDay;
    }

    public void setLastReplenishDay(long lastReplenishDay) {
        this.lastReplenishDay = Math.max(0L, lastReplenishDay);
    }

    public void addRescuer(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        availableRescuers.add(id);
    }

    public boolean hasRegisteredRescuer(String id) {
        return id != null && !id.isBlank() && registeredRescuers.contains(id);
    }

    public void markRegisteredRescuer(String id) {
        if (id == null || id.isBlank() || registeredRescuers.contains(id)) {
            return;
        }
        registeredRescuers.add(id);
    }

    public void setRegisteredRescuers(List<String> ids) {
        registeredRescuers.clear();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            if (id != null && !id.isBlank() && !registeredRescuers.contains(id)) {
                registeredRescuers.add(id);
            }
        }
    }

    public List<String> getRegisteredRescuers() {
        return List.copyOf(registeredRescuers);
    }

    public int getChargeCount() {
        return availableRescuers.size();
    }

    public String consumeCharge() {
        if (availableRescuers.isEmpty()) {
            return "";
        }
        return availableRescuers.remove(0);
    }

    public String consumeCharge(Predicate<String> preferredMatcher) {
        if (availableRescuers.isEmpty()) {
            return "";
        }
        if (preferredMatcher != null) {
            for (int i = 0; i < availableRescuers.size(); i++) {
                String rescuer = availableRescuers.get(i);
                if (preferredMatcher.test(rescuer)) {
                    availableRescuers.remove(i);
                    return rescuer;
                }
            }
        }
        return consumeCharge();
    }

    public void replenish(List<String> allUnlockedIds) {
        availableRescuers.clear();
        if (allUnlockedIds == null || allUnlockedIds.isEmpty()) {
            return;
        }
        for (String id : allUnlockedIds) {
            if (id != null && !id.isBlank()) {
                availableRescuers.add(id);
            }
        }
    }

    public List<String> getAvailableRescuers() {
        return List.copyOf(availableRescuers);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(LAST_DAY_KEY, lastReplenishDay);
        ListTag listTag = new ListTag();
        for (String rescuer : availableRescuers) {
            listTag.add(StringTag.valueOf(rescuer));
        }
        tag.put(AVAILABLE_KEY, listTag);
        ListTag registeredTag = new ListTag();
        for (String rescuer : registeredRescuers) {
            registeredTag.add(StringTag.valueOf(rescuer));
        }
        tag.put(REGISTERED_KEY, registeredTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        lastReplenishDay = Math.max(0L, nbt.getLong(LAST_DAY_KEY));
        availableRescuers.clear();
        ListTag listTag = nbt.getList(AVAILABLE_KEY, Tag.TAG_STRING);
        for (Tag tag : listTag) {
            String rescuer = tag.getAsString();
            if (!rescuer.isBlank()) {
                availableRescuers.add(rescuer);
            }
        }
        registeredRescuers.clear();
        ListTag registeredTag = nbt.getList(REGISTERED_KEY, Tag.TAG_STRING);
        for (Tag tag : registeredTag) {
            String rescuer = tag.getAsString();
            if (!rescuer.isBlank() && !registeredRescuers.contains(rescuer)) {
                registeredRescuers.add(rescuer);
            }
        }
    }
}
