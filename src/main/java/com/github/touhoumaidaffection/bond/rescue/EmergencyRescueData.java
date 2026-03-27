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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyRescueData {
    private static final String RESCUER_TOKEN_PREFIX = "maid:";
    private static final String EMERGENCY_HEAL_ABILITY_ID = "emergency_heal";
    private static final String PROVIDER_CONTRIBUTOR_KEY_PREFIX = "provider:";
    private static final String LEGACY_CONTRIBUTOR_KEY_PREFIX = "legacy:";

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
        if (maidIds == null || maidIds.isEmpty()) {
            get(player).setRegisteredRescuers(List.of());
            return;
        }
        BondData data = BondData.of(player);
        Map<String, String> canonicalByContributor = new LinkedHashMap<>(maidIds.size());
        for (UUID maidId : maidIds) {
            RescueContributorIdentity identity = resolveContributorIdentity(data, maidId);
            if (identity == null) {
                continue;
            }
            canonicalByContributor.putIfAbsent(identity.contributorKey(), identity.canonicalRescuerId());
        }
        get(player).setRegisteredRescuers(new ArrayList<>(canonicalByContributor.values()));
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
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        List<String> contributorIds = getUnlockedRescueContributorIds(player);
        List<String> expanded = new ArrayList<>(contributorIds.size() * chargesPerMaid);
        for (String contributorId : contributorIds) {
            for (int i = 0; i < chargesPerMaid; i++) {
                expanded.add(contributorId);
            }
        }
        return expanded;
    }

    public static void grantImmediateRescueIfEligible(ServerPlayer player, UUID maidUuid) {
        RescueContributorIdentity identity = resolveContributorIdentity(BondData.of(player), maidUuid);
        if (identity == null) {
            return;
        }
        String rescuerId = identity.canonicalRescuerId();
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
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());

        List<String> currentAvailable = getAvailableRescuerIds(player);
        List<String> normalizedAvailable = new ArrayList<>(currentAvailable.size());
        Map<String, Integer> contributorChargeCount = new LinkedHashMap<>();
        boolean availableDirty = false;
        for (String rescuerId : currentAvailable) {
            UUID maidUuid = resolveRescuerToMaidUuid(data, rescuerId, unlockedSet);
            if (maidUuid == null) {
                availableDirty = true;
                continue;
            }
            RescueContributorIdentity identity = resolveContributorIdentity(data, maidUuid);
            if (identity == null) {
                availableDirty = true;
                continue;
            }
            int existing = contributorChargeCount.getOrDefault(identity.contributorKey(), 0);
            if (existing >= chargesPerMaid) {
                availableDirty = true;
                continue;
            }
            contributorChargeCount.put(identity.contributorKey(), existing + 1);
            String canonical = identity.canonicalRescuerId();
            normalizedAvailable.add(canonical);
            if (!canonical.equals(rescuerId)) {
                availableDirty = true;
            }
        }
        if (availableDirty) {
            setAvailableRescuerIds(player, normalizedAvailable);
        }

        Set<String> expectedRegistered = new LinkedHashSet<>(getUnlockedRescueContributorIds(player));
        Set<String> currentRegistered = new LinkedHashSet<>(getRegisteredRescuerIds(player));
        boolean registeredDirty = !currentRegistered.equals(expectedRegistered);
        if (registeredDirty) {
            get(player).setRegisteredRescuers(new ArrayList<>(expectedRegistered));
        }
        return availableDirty || registeredDirty;
    }

    public static boolean isContributorAlreadyUnlocked(ServerPlayer player, UUID maidUuid) {
        if (player == null || maidUuid == null) {
            return false;
        }
        BondData data = BondData.of(player);
        RescueContributorIdentity target = resolveContributorIdentity(data, maidUuid);
        if (target == null) {
            return false;
        }
        for (UUID unlockedMaidId : BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID)) {
            if (maidUuid.equals(unlockedMaidId)) {
                continue;
            }
            RescueContributorIdentity candidate = resolveContributorIdentity(data, unlockedMaidId);
            if (candidate != null && target.contributorKey().equals(candidate.contributorKey())) {
                return true;
            }
        }
        return false;
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
        String targetKey = resolveContributorKey(data, rescuerId);
        if (targetKey.isBlank()) {
            return false;
        }
        for (String registered : getRegisteredRescuerIds(player)) {
            if (rescuerId.equals(registered)) {
                return true;
            }
            String registeredKey = resolveContributorKey(data, registered);
            if (targetKey.equals(registeredKey)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveContributorKey(BondData data, String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return "";
        }
        UUID maidUuid = resolveRescuerToMaidUuid(data, rescuerId, null);
        RescueContributorIdentity identity = resolveContributorIdentity(data, maidUuid);
        if (identity != null) {
            return identity.contributorKey();
        }
        String lookupId = toLegacyLookupId(rescuerId);
        if (!lookupId.isBlank()) {
            return LEGACY_CONTRIBUTOR_KEY_PREFIX + lookupId;
        }
        return rescuerId.trim();
    }

    private static List<String> getUnlockedRescueContributorIds(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        BondData data = BondData.of(player);
        List<UUID> unlockedIds = BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID);
        Map<String, String> canonicalByContributor = new LinkedHashMap<>(unlockedIds.size());
        for (UUID maidUuid : unlockedIds) {
            RescueContributorIdentity identity = resolveContributorIdentity(data, maidUuid);
            if (identity == null) {
                continue;
            }
            canonicalByContributor.putIfAbsent(identity.contributorKey(), identity.canonicalRescuerId());
        }
        return new ArrayList<>(canonicalByContributor.values());
    }

    private static RescueContributorIdentity resolveContributorIdentity(BondData data, UUID maidUuid) {
        if (data == null || maidUuid == null) {
            return null;
        }
        String fallbackCanonicalId = toRescuerToken(maidUuid);
        String providerId = data.getMaidRescueProviderId(maidUuid);
        if (providerId == null || providerId.isBlank()) {
            return new RescueContributorIdentity(fallbackCanonicalId, fallbackCanonicalId);
        }
        UUID representative = data.findMaidUuidByRescueProviderId(providerId, EMERGENCY_HEAL_ABILITY_ID);
        String canonicalId = toRescuerToken(representative == null ? maidUuid : representative);
        return new RescueContributorIdentity(PROVIDER_CONTRIBUTOR_KEY_PREFIX + providerId, canonicalId);
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

    private record RescueContributorIdentity(String contributorKey, String canonicalRescuerId) {
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
