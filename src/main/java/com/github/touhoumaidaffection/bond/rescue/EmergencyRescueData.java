package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.ModAttachments;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.BondManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyRescueData {
    private EmergencyRescueData() {
    }

    public static long getLastReplenishDay(ServerPlayer player) {
        return get(player).getLastReplenishDay();
    }

    public static void setLastReplenishDay(ServerPlayer player, long day) {
        get(player).setLastReplenishDay(day);
    }

    public static List<String> getAvailableRescuerIds(ServerPlayer player) {
        return get(player).getAvailableRescuers();
    }

    public static List<String> getRegisteredRescuerIds(ServerPlayer player) {
        return get(player).getRegisteredRescuers();
    }

    public static void setAvailableRescuerIds(ServerPlayer player, List<String> rescuerIds) {
        get(player).replenish(rescuerIds);
    }

    public static boolean isRescueEnabled(ServerPlayer player) {
        return get(player).isRescueEnabled();
    }

    public static void setRescueEnabled(ServerPlayer player, boolean enabled) {
        get(player).setRescueEnabled(enabled);
    }

    public static void addRescuer(ServerPlayer player, String id) {
        get(player).addRescuer(id);
    }

    public static boolean hasRegisteredRescuer(ServerPlayer player, String rescuerId) {
        return get(player).hasRegisteredRescuer(rescuerId);
    }

    public static void markRegisteredRescuer(ServerPlayer player, String rescuerId) {
        get(player).markRegisteredRescuer(rescuerId);
    }

    public static boolean hasRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        return hasRegisteredRescuer(player, maidUuid.toString());
    }

    public static void markRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        markRegisteredRescuer(player, maidUuid.toString());
    }

    public static void setRegisteredRescuers(ServerPlayer player, List<UUID> maidIds) {
        Set<String> providerIds = new java.util.LinkedHashSet<>(maidIds.size());
        for (UUID maidId : maidIds) {
            providerIds.add(resolveRescuerId(player, maidId));
        }
        get(player).setRegisteredRescuers(new java.util.ArrayList<>(providerIds));
    }

    public static int getChargeCount(ServerPlayer player) {
        return get(player).getChargeCount();
    }

    public static int getMaxChargeCount(ServerPlayer player) {
        return buildDailyRescuerList(player).size();
    }

    public static void replenish(ServerPlayer player, List<String> allUnlockedIds) {
        get(player).replenish(allUnlockedIds);
    }

    public static String consumeCharge(ServerPlayer player) {
        BondData data = BondData.of(player);
        return get(player).consumeCharge(rescuerId -> hasYsmProfile(data, rescuerId));
    }

    public static String consumeOne(ServerPlayer player) {
        return consumeCharge(player);
    }

    public static List<String> buildDailyRescuerList(ServerPlayer player) {
        List<UUID> unlockedIds = BondManager.getUnlockedMaidIdsForAbility(player, "emergency_heal");
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        Set<String> providerIds = new java.util.LinkedHashSet<>(unlockedIds.size());
        for (UUID maidUuid : unlockedIds) {
            providerIds.add(resolveRescuerId(player, maidUuid));
        }
        List<String> expanded = new java.util.ArrayList<>(providerIds.size() * chargesPerMaid);
        for (String providerId : providerIds) {
            for (int i = 0; i < chargesPerMaid; i++) {
                expanded.add(providerId);
            }
        }
        return expanded;
    }

    public static void grantImmediateRescueIfEligible(ServerPlayer player, UUID maidUuid) {
        String legacyId = maidUuid.toString();
        String rescuerId = resolveRescuerId(player, maidUuid);
        if (hasRegisteredAlias(player, rescuerId) || hasRegisteredRescuer(player, legacyId)) {
            return;
        }
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        for (int i = 0; i < chargesPerMaid; i++) {
            addRescuer(player, rescuerId);
        }
        markRegisteredRescuer(player, rescuerId);
        if (!legacyId.equals(rescuerId)) {
            markRegisteredRescuer(player, legacyId);
        }
    }

    public static EmergencyRescueAttachment get(ServerPlayer player) {
        return player.getData(ModAttachments.EMERGENCY_RESCUE);
    }

    private static boolean hasYsmProfile(BondData data, String rescuerId) {
        BondData.MaidProfileSnapshot byProvider = data.findMaidProfileByRescueProviderId(rescuerId);
        if (!byProvider.ysmModelId().isBlank()) {
            return true;
        }
        try {
            UUID maidUuid = UUID.fromString(rescuerId);
            return !data.getMaidYsmModelId(maidUuid).isBlank();
        } catch (IllegalArgumentException ignored) {
            return !data.findMaidProfileByModelId(rescuerId).ysmModelId().isBlank();
        }
    }

    private static String resolveRescuerId(ServerPlayer player, UUID maidUuid) {
        String providerId = BondManager.getMaidRescueProviderId(player, maidUuid);
        return providerId.isBlank() ? maidUuid.toString() : providerId;
    }

    private static boolean hasRegisteredAlias(ServerPlayer player, String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return false;
        }
        for (String registered : getRegisteredRescuerIds(player)) {
            if (rescuerId.equals(registered)) {
                return true;
            }
            try {
                UUID maidUuid = UUID.fromString(registered);
                if (rescuerId.equals(resolveRescuerId(player, maidUuid))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore non-UUID legacy ids.
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean existedBefore = player.hasData(ModAttachments.EMERGENCY_RESCUE);
        EmergencyRescueAttachment data = get(player);
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue attachment ready for player {} (preExisting={}, charges={}, lastDay={})",
                player.getGameProfile().getName(),
                existedBefore,
                data.getChargeCount(),
                data.getLastReplenishDay()
        );
    }
}
