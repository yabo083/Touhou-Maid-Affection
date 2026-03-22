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

    public static void setAvailableRescuerIds(ServerPlayer player, List<String> rescuerIds) {
        get(player).replenish(rescuerIds);
    }

    public static void addRescuer(ServerPlayer player, String id) {
        get(player).addRescuer(id);
    }

    public static boolean hasRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        return get(player).hasRegisteredRescuer(maidUuid.toString());
    }

    public static void markRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        get(player).markRegisteredRescuer(maidUuid.toString());
    }

    public static void setRegisteredRescuers(ServerPlayer player, List<UUID> maidIds) {
        List<String> ids = new java.util.ArrayList<>(maidIds.size());
        for (UUID maidId : maidIds) {
            ids.add(maidId.toString());
        }
        get(player).setRegisteredRescuers(ids);
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
        List<String> expanded = new java.util.ArrayList<>(unlockedIds.size() * chargesPerMaid);
        for (UUID maidUuid : unlockedIds) {
            String rescuerId = maidUuid.toString();
            for (int i = 0; i < chargesPerMaid; i++) {
                expanded.add(rescuerId);
            }
        }
        return expanded;
    }

    public static void grantImmediateRescueIfEligible(ServerPlayer player, UUID maidUuid) {
        if (hasRegisteredRescuer(player, maidUuid)) {
            return;
        }
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        String rescuerId = maidUuid.toString();
        for (int i = 0; i < chargesPerMaid; i++) {
            addRescuer(player, rescuerId);
        }
        markRegisteredRescuer(player, maidUuid);
    }

    public static EmergencyRescueAttachment get(ServerPlayer player) {
        return player.getData(ModAttachments.EMERGENCY_RESCUE);
    }

    private static boolean hasYsmProfile(BondData data, String rescuerId) {
        try {
            UUID maidUuid = UUID.fromString(rescuerId);
            return !data.getMaidYsmModelId(maidUuid).isBlank();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
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
