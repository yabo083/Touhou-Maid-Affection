package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModCapabilities;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class EmergencyRescueCapabilityHandler {
    private static final String CLONE_SYNC_KEY = TouhouMaidAffection.MOD_ID + "_emergency_rescue_clone";

    private EmergencyRescueCapabilityHandler() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            EmergencyRescueCapabilityProvider provider = new EmergencyRescueCapabilityProvider();
            CompoundTag persistent = player.getPersistentData();
            CompoundTag seed = readSeedSnapshot(persistent);
            if (!seed.isEmpty()) {
                provider.deserializeNBT(seed);
            }
            persistent.put(EmergencyRescueData.BACKUP_KEY, provider.serializeNBT().copy());
            event.addCapability(EmergencyRescueCapabilityProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) || !(event.getOriginal() instanceof ServerPlayer original)) {
            return;
        }

        CompoundTag snapshot = new CompoundTag();
        CompoundTag originalPersistent = original.getPersistentData();
        if (originalPersistent.contains(EmergencyRescueData.BACKUP_KEY, Tag.TAG_COMPOUND)) {
            snapshot = originalPersistent.getCompound(EmergencyRescueData.BACKUP_KEY).copy();
        }

        original.reviveCaps();
        try {
            if (snapshot.isEmpty()) {
                snapshot = original.getCapability(ModCapabilities.EMERGENCY_RESCUE)
                        .map(data -> data.serializeNBT().copy())
                        .orElse(snapshot);
            }
        } finally {
            original.invalidateCaps();
        }

        if (snapshot.isEmpty()) {
            return;
        }

        CompoundTag finalSnapshot = snapshot.copy();
        CompoundTag newPersistent = newPlayer.getPersistentData();
        newPersistent.put(CLONE_SYNC_KEY, finalSnapshot.copy());
        newPersistent.put(EmergencyRescueData.BACKUP_KEY, finalSnapshot.copy());
        newPlayer.getCapability(ModCapabilities.EMERGENCY_RESCUE).ifPresent(newData -> {
            newData.deserializeNBT(finalSnapshot);
            newPersistent.remove(CLONE_SYNC_KEY);
            newPersistent.put(EmergencyRescueData.BACKUP_KEY, newData.serializeNBT().copy());
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyPendingCloneSnapshot(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        applyPendingCloneSnapshot(event.getEntity());
    }

    private static void applyPendingCloneSnapshot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(CLONE_SYNC_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag snapshot = persistent.getCompound(CLONE_SYNC_KEY);
        player.getCapability(ModCapabilities.EMERGENCY_RESCUE).ifPresent(data -> {
            data.deserializeNBT(snapshot);
            persistent.remove(CLONE_SYNC_KEY);
            persistent.put(EmergencyRescueData.BACKUP_KEY, data.serializeNBT().copy());
        });
    }

    private static CompoundTag readSeedSnapshot(CompoundTag persistent) {
        if (persistent.contains(CLONE_SYNC_KEY, Tag.TAG_COMPOUND)) {
            return persistent.getCompound(CLONE_SYNC_KEY).copy();
        }
        if (persistent.contains(EmergencyRescueData.BACKUP_KEY, Tag.TAG_COMPOUND)) {
            return persistent.getCompound(EmergencyRescueData.BACKUP_KEY).copy();
        }
        return new CompoundTag();
    }
}
