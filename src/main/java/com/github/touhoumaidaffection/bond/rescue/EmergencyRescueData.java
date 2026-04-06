package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.ModCapabilities;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.BondManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class EmergencyRescueData {
    public static final String BACKUP_KEY = TouhouMaidAffection.MOD_ID + "_emergency_rescue_backup";
    private static final String RESCUER_TOKEN_PREFIX = "maid:";
    private static final String RESCUER_PROVIDER_PREFIX = "provider:";
    private static final String EMERGENCY_HEAL_ABILITY_ID = "emergency_heal";

    private EmergencyRescueData() {
    }

    public static long getLastReplenishDay(ServerPlayer player) {
        return get(player).getLastReplenishDay();
    }

    public static void setLastReplenishDay(ServerPlayer player, long day) {
        EmergencyRescueAttachment data = get(player);
        data.setLastReplenishDay(day);
        saveBackup(player, data);
    }

    public static List<String> getAvailableRescuerIds(ServerPlayer player) {
        return get(player).getAvailableRescuers();
    }

    public static List<String> getRegisteredRescuerIds(ServerPlayer player) {
        return get(player).getRegisteredRescuers();
    }

    public static void setAvailableRescuerIds(ServerPlayer player, List<String> rescuerIds) {
        EmergencyRescueAttachment data = get(player);
        data.replenish(rescuerIds);
        saveBackup(player, data);
    }

    public static boolean isRescueEnabled(ServerPlayer player) {
        return get(player).isRescueEnabled();
    }

    public static void setRescueEnabled(ServerPlayer player, boolean enabled) {
        EmergencyRescueAttachment data = get(player);
        data.setRescueEnabled(enabled);
        saveBackup(player, data);
    }

    public static void addRescuer(ServerPlayer player, String id) {
        EmergencyRescueAttachment data = get(player);
        data.addRescuer(id);
        saveBackup(player, data);
    }

    public static boolean hasRegisteredRescuer(ServerPlayer player, String rescuerId) {
        return get(player).hasRegisteredRescuer(rescuerId);
    }

    public static void markRegisteredRescuer(ServerPlayer player, String rescuerId) {
        EmergencyRescueAttachment data = get(player);
        data.markRegisteredRescuer(rescuerId);
        saveBackup(player, data);
    }

    public static boolean hasRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        String canonicalId = toCanonicalRescuerId(BondData.of(player), maidUuid);
        return hasRegisteredRescuer(player, canonicalId);
    }

    public static boolean isContributorAlreadyUnlocked(ServerPlayer player, UUID maidUuid) {
        String canonicalId = toCanonicalRescuerId(BondData.of(player), maidUuid);
        return hasRegisteredAlias(player, canonicalId);
    }

    public static void markRegisteredRescuer(ServerPlayer player, UUID maidUuid) {
        String canonicalId = toCanonicalRescuerId(BondData.of(player), maidUuid);
        markRegisteredRescuer(player, canonicalId);
    }

    public static void setRegisteredRescuers(ServerPlayer player, List<UUID> maidIds) {
        BondData bondData = BondData.of(player);
        Set<String> canonicalIds = new LinkedHashSet<>(maidIds.size());
        for (UUID maidId : maidIds) {
            String canonicalId = toCanonicalRescuerId(bondData, maidId);
            if (!canonicalId.isBlank()) {
                canonicalIds.add(canonicalId);
            }
        }
        EmergencyRescueAttachment data = get(player);
        data.setRegisteredRescuers(new ArrayList<>(canonicalIds));
        saveBackup(player, data);
    }

    public static void clearPoolAndRegistration(ServerPlayer player) {
        if (player == null) {
            return;
        }
        setAvailableRescuerIds(player, List.of());
        EmergencyRescueAttachment data = get(player);
        data.setRegisteredRescuers(List.of());
        saveBackup(player, data);
        setLastReplenishDay(player, 0L);
    }

    public static int getChargeCount(ServerPlayer player) {
        return get(player).getChargeCount();
    }

    public static int getMaxChargeCount(ServerPlayer player) {
        return buildDailyRescuerList(player).size();
    }

    public static void replenish(ServerPlayer player, List<String> allUnlockedIds) {
        EmergencyRescueAttachment data = get(player);
        data.replenish(allUnlockedIds);
        saveBackup(player, data);
    }

    public static String consumeCharge(ServerPlayer player) {
        BondData data = BondData.of(player);
        EmergencyRescueAttachment attachment = get(player);
        String consumed = attachment.consumeCharge(rescuerId -> hasYsmProfile(data, rescuerId));
        if (!consumed.isBlank()) {
            saveBackup(player, attachment);
        }
        return consumed;
    }

    public static String consumeOne(ServerPlayer player) {
        return consumeCharge(player);
    }

    public static List<String> buildDailyRescuerList(ServerPlayer player) {
        List<UUID> unlockedIds = BondManager.getUnlockedMaidIdsForAbility(player, EMERGENCY_HEAL_ABILITY_ID);
        int chargesPerMaid = Math.max(1, ModConfig.BOND_EMERGENCY_RESCUE_CHARGES_PER_MAID.get());
        BondData data = BondData.of(player);
        Set<String> canonicalIds = new LinkedHashSet<>(unlockedIds.size());
        for (UUID maidUuid : unlockedIds) {
            String canonicalId = toCanonicalRescuerId(data, maidUuid);
            if (!canonicalId.isBlank()) {
                canonicalIds.add(canonicalId);
            }
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
        BondData data = BondData.of(player);
        String rescuerId = toCanonicalRescuerId(data, maidUuid);
        if (rescuerId.isBlank()) {
            return;
        }
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
            String canonical = normalizeRescuerId(data, rescuerId, unlockedSet);
            if (canonical.isBlank()) {
                availableDirty = true;
                continue;
            }
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
            String canonical = toCanonicalRescuerId(data, maidId);
            if (!canonical.isBlank()) {
                expectedRegistered.add(canonical);
            }
        }
        Set<String> currentRegistered = new LinkedHashSet<>(getRegisteredRescuerIds(player));
        boolean registeredDirty = !currentRegistered.equals(expectedRegistered);
        if (registeredDirty) {
            EmergencyRescueAttachment attachment = get(player);
            attachment.setRegisteredRescuers(new ArrayList<>(expectedRegistered));
            saveBackup(player, attachment);
        }
        return availableDirty || registeredDirty;
    }

    public static EmergencyRescueAttachment get(ServerPlayer player) {
        EmergencyRescueAttachment data = player.getCapability(ModCapabilities.EMERGENCY_RESCUE)
                .orElseGet(EmergencyRescueAttachment::new);
        restoreBackupIfMissing(player, data);
        return data;
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
        String normalized = rescuerId.trim();
        if (normalized.startsWith(RESCUER_TOKEN_PREFIX)) {
            return tryParseUuid(normalized.substring(RESCUER_TOKEN_PREFIX.length())) != null;
        }
        return isProviderRescuerId(normalized);
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
        if (normalized.startsWith(RESCUER_PROVIDER_PREFIX)) {
            return normalized.substring(RESCUER_PROVIDER_PREFIX.length());
        }
        return normalized;
    }

    private static boolean hasRegisteredAlias(ServerPlayer player, String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return false;
        }
        BondData data = BondData.of(player);
        String targetCanonical = normalizeRescuerId(data, rescuerId, null);
        if (targetCanonical.isBlank()) {
            targetCanonical = rescuerId.trim();
        }
        for (String registered : getRegisteredRescuerIds(player)) {
            if (rescuerId.equals(registered)) {
                return true;
            }
            String registeredCanonical = normalizeRescuerId(data, registered, null);
            if (registeredCanonical.isBlank()) {
                registeredCanonical = registered.trim();
            }
            if (targetCanonical.equals(registeredCanonical)) {
                return true;
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
        boolean existedBefore = player.getCapability(ModCapabilities.EMERGENCY_RESCUE).isPresent();
        EmergencyRescueAttachment data = get(player);
        saveBackup(player, data);
        TouhouMaidAffection.LOGGER.info(
                "Emergency rescue attachment ready for player {} (preExisting={}, charges={}, lastDay={})",
                player.getGameProfile().getName(),
                existedBefore,
                data.getChargeCount(),
                data.getLastReplenishDay()
        );
    }

    private static String normalizeRescuerId(BondData data, String rescuerId, Set<UUID> preferredMaidIds) {
        UUID resolvedMaidUuid = resolveRescuerToMaidUuid(data, rescuerId, preferredMaidIds);
        if (resolvedMaidUuid != null) {
            return toCanonicalRescuerId(data, resolvedMaidUuid);
        }

        if (preferredMaidIds == null && isProviderRescuerId(rescuerId)) {
            String providerId = normalizeProviderId(toLegacyLookupId(rescuerId));
            if (!providerId.isBlank()) {
                return toProviderToken(providerId);
            }
        }
        return "";
    }

    private static String toCanonicalRescuerId(BondData data, UUID maidUuid) {
        if (maidUuid == null) {
            return "";
        }
        String providerId = normalizeProviderId(data.getMaidRescueProviderId(maidUuid));
        if (!providerId.isBlank()) {
            return toProviderToken(providerId);
        }
        return toRescuerToken(maidUuid);
    }

    private static String toProviderToken(String providerId) {
        String normalized = normalizeProviderId(providerId);
        if (normalized.isBlank()) {
            return "";
        }
        return RESCUER_PROVIDER_PREFIX + normalized;
    }

    private static String normalizeProviderId(String providerId) {
        if (providerId == null) {
            return "";
        }
        return providerId.trim();
    }

    private static boolean isProviderRescuerId(String rescuerId) {
        if (rescuerId == null || rescuerId.isBlank()) {
            return false;
        }
        if (!rescuerId.startsWith(RESCUER_PROVIDER_PREFIX)) {
            return false;
        }
        return !rescuerId.substring(RESCUER_PROVIDER_PREFIX.length()).isBlank();
    }

    private static void restoreBackupIfMissing(ServerPlayer player, EmergencyRescueAttachment data) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(BACKUP_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        boolean seemsFresh = data.getLastReplenishDay() == 0L
                && data.getChargeCount() == 0
                && data.getRegisteredRescuers().isEmpty();
        if (!seemsFresh) {
            return;
        }
        data.deserializeNBT(persistent.getCompound(BACKUP_KEY));
    }

    private static void saveBackup(ServerPlayer player, EmergencyRescueAttachment data) {
        player.getPersistentData().put(BACKUP_KEY, data.serializeNBT().copy());
    }
}
