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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyRescueData {
    private static final String RESCUER_TOKEN_PREFIX = "maid:";
    private static final String EMERGENCY_HEAL_ABILITY_ID = "emergency_heal";

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
        return hasRegisteredRescuer(player, toRescuerToken(maidUuid));
    }

    public static void markRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        markRegisteredRescuer(player, toRescuerToken(maidUuid));
    }

    public static void setRegisteredRescuers(ServerPlayer player, List<UUID> maidIds) {
        Set<String> canonicalIds = new LinkedHashSet<>(maidIds.size());
        for (UUID maidId : maidIds) {
            canonicalIds.add(toRescuerToken(maidId));
        }
        get(player).setRegisteredRescuers(new ArrayList<>(canonicalIds));
    }

    public static void clearPoolAndRegistration(ServerPlayer player) {
        if (player == null) {
            return;
        }
        setAvailableRescuerIds(player, List.of());
        get(player).setRegisteredRescuers(List.of());
        setLastReplenishDay(player, 0L);
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
        List<UUID> unlockedIds = BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID);
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        Set<String> canonicalIds = new LinkedHashSet<>(unlockedIds.size());
        for (UUID maidUuid : unlockedIds) {
            canonicalIds.add(toRescuerToken(maidUuid));
        }
        List<String> expanded = new ArrayList<>(canonicalIds.size() * chargesPerMaid);
        for (String canonicalId : canonicalIds) {
            for (int i = 0; i < chargesPerMaid; i++) {
                expanded.add(canonicalId);
            }
        }
        return expanded;
    }

    public static void grantImmediateRescueIfEligible(ServerPlayer player, UUID maidUuid) {
        String rescuerId = toRescuerToken(maidUuid);
        if (hasRegisteredAlias(player, rescuerId)) {
            return;
        }
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        for (int i = 0; i < chargesPerMaid; i++) {
            addRescuer(player, rescuerId);
        }
        markRegisteredRescuer(player, rescuerId);
    }

    public static boolean normalizeRescuerState(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        List<UUID> unlockedIds = BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID);
        Set<UUID> unlockedSet = new LinkedHashSet<>(unlockedIds);
        BondData data = BondData.of(player);

        List<String> currentAvailable = getAvailableRescuerIds(player);
        List<String> normalizedAvailable = new ArrayList<>(currentAvailable.size());
        boolean availableDirty = false;
        for (String rescuerId : currentAvailable) {
            UUID maidUuid = resolveRescuerToMaidUuid(data, rescuerId, unlockedSet);
            if (maidUuid == null) {
                availableDirty = true;
                continue;
            }
            String canonical = toRescuerToken(maidUuid);
            normalizedAvailable.add(canonical);
            if (!canonical.equals(rescuerId)) {
                availableDirty = true;
            }
        }
        if (availableDirty) {
            setAvailableRescuerIds(player, normalizedAvailable);
        }

        Set<String> expectedRegistered = new LinkedHashSet<>(unlockedIds.size());
        for (UUID maidId : unlockedIds) {
            expectedRegistered.add(toRescuerToken(maidId));
        }
        Set<String> currentRegistered = new LinkedHashSet<>(getRegisteredRescuerIds(player));
        boolean registeredDirty = !currentRegistered.equals(expectedRegistered);
        if (registeredDirty) {
            setRegisteredRescuers(player, unlockedIds);
        }
        return availableDirty || registeredDirty;
    }

    public static EmergencyRescueAttachment get(ServerPlayer player) {
        return player.getData(ModAttachments.EMERGENCY_RESCUE);
    }

    private static boolean hasYsmProfile(BondData data, String rescuerId) {
        UUID resolved = resolveRescuerToMaidUuid(data, rescuerId, null);
        if (resolved != null && !data.getMaidYsmModelId(resolved).isBlank()) {
            return true;
        }
        String lookupId = toLegacyLookupId(rescuerId);
        if (lookupId.isBlank()) {
            return false;
        }
        return !data.findMaidProfileByModelId(lookupId).ysmModelId().isBlank();
    }

    public static UUID resolveRescuerToMaidUuid(ServerPlayer player, String rescuerId) {
        Set<UUID> unlockedSet = new LinkedHashSet<>(BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID));
        Set<UUID> preferred = unlockedSet.isEmpty() ? null : unlockedSet;
        return resolveRescuerToMaidUuid(BondData.of(player), rescuerId, preferred);
    }

    public static String toRescuerToken(UUID maidUuid) {
        return maidUuid == null ? "" : RESCUER_TOKEN_PREFIX + maidUuid;
    }

    public static boolean isCanonicalRescuerId(String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return false;
        }
        if (!rescuerId.startsWith(RESCUER_TOKEN_PREFIX)) {
            return false;
        }
        return tryParseUuid(rescuerId.substring(RESCUER_TOKEN_PREFIX.length())) != null;
    }

    public static UUID tryExtractMaidUuid(String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return null;
        }
        String normalized = rescuerId.trim();
        if (normalized.startsWith(RESCUER_TOKEN_PREFIX)) {
            return tryParseUuid(normalized.substring(RESCUER_TOKEN_PREFIX.length()));
        }
        return tryParseUuid(normalized);
    }

    public static String toLegacyLookupId(String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return "";
        }
        String normalized = rescuerId.trim();
        if (normalized.startsWith(RESCUER_TOKEN_PREFIX)) {
            return normalized.substring(RESCUER_TOKEN_PREFIX.length());
        }
        return normalized;
    }

    private static boolean hasRegisteredAlias(ServerPlayer player, String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return false;
        }
        BondData data = BondData.of(player);
        UUID targetUuid = resolveRescuerToMaidUuid(data, rescuerId, null);
        for (String registered : getRegisteredRescuerIds(player)) {
            if (rescuerId.equals(registered)) {
                return true;
            }
            if (targetUuid != null) {
                UUID registeredUuid = resolveRescuerToMaidUuid(data, registered, null);
                if (targetUuid.equals(registeredUuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static UUID resolveRescuerToMaidUuid(BondData data, String rescuerId, Set<UUID> preferredMaidIds) {
        UUID directUuid = tryExtractMaidUuid(rescuerId);
        if (isAllowed(directUuid, preferredMaidIds) && hasKnownMaidRecord(data, directUuid)) {
            return directUuid;
        }

        String lookupId = toLegacyLookupId(rescuerId);
        if (!lookupId.isBlank()) {
            UUID byProvider = data.findMaidUuidByRescueProviderId(lookupId, EMERGENCY_HEAL_ABILITY_ID);
            if (isAllowed(byProvider, preferredMaidIds)) {
                return byProvider;
            }
            UUID byModel = data.findMaidUuidByModelId(lookupId);
            if (isAllowed(byModel, preferredMaidIds)) {
                return byModel;
            }
        }

        if (isAllowed(directUuid, preferredMaidIds)) {
            return directUuid;
        }
        return null;
    }

    private static boolean isAllowed(UUID candidate, Set<UUID> preferredMaidIds) {
        if (candidate == null) {
            return false;
        }
        return preferredMaidIds == null || preferredMaidIds.contains(candidate);
    }

    private static boolean hasKnownMaidRecord(BondData data, UUID maidUuid) {
        if (maidUuid == null) {
            return false;
        }
        if (data.isBondUnlocked(maidUuid)) {
            return true;
        }
        return !data.getMaidModelId(maidUuid).isBlank()
                || !data.getMaidDisplayName(maidUuid).isBlank()
                || !data.getMaidSoundPackId(maidUuid).isBlank()
                || !data.getMaidYsmModelId(maidUuid).isBlank()
                || !data.getMaidRescueAction(maidUuid).isBlank();
    }

    private static UUID tryParseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
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
