package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.ModEffects;
import com.github.touhoumaidaffection.ModSounds;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.KissMaidPayload;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class KissMaidHandler {

    private static final Map<MinecraftServer, SessionState> SESSION_STATES = new IdentityHashMap<>();

    private static Boolean carryOnLoaded = null;

    private static boolean isCarryOnLoaded() {
        if (carryOnLoaded == null) {
            carryOnLoaded = ModList.get().isLoaded("carryon");
        }
        return carryOnLoaded;
    }

    private static long getCooldownForLevel(int level) {
        return switch (level) {
            case 1 -> ModConfig.COOLDOWN_LEVEL_1.get();
            case 2 -> ModConfig.COOLDOWN_LEVEL_2.get();
            case 3 -> ModConfig.COOLDOWN_LEVEL_3.get();
            default -> ModConfig.COOLDOWN_LEVEL_0.get();
        };
    }

    @SubscribeEvent
    public static void onInteractMaid(InteractMaidEvent event) {
        Player player = event.getPlayer();
        EntityMaid maid = event.getMaid();

        // Only trigger when sneaking with empty main hand
        if (!player.isShiftKeyDown() || !event.getStack().isEmpty()) {
            return;
        }

        // CarryOn compatibility: when CarryOn is loaded, it uses sneak + both hands empty
        // to pick up entities. Only trigger kiss when offhand is NOT empty to avoid conflict.
        if (isCarryOnLoaded() && player.getOffhandItem().isEmpty()) {
            return;
        }

        // Only on server side
        if (player.level().isClientSide) {
            event.setCanceled(true);
            return;
        }

        if (executeKiss(player, maid)) {
            // Cancel to prevent opening the maid GUI when kiss succeeds
            event.setCanceled(true);
        }
    }

    public static void tryKissCarriedMaid(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        for (var passenger : player.getPassengers()) {
            if (passenger instanceof EntityMaid maid) {
                executeKiss(player, maid);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        SessionState sessionState = SESSION_STATES.get(server);
        if (sessionState == null) {
            return;
        }

        UUID playerId = player.getUUID();
        sessionState.cooldownsByPlayerAndMaid.remove(playerId);
        sessionState.kissTimestamps.remove(playerId);
        if (sessionState.isEmpty()) {
            SESSION_STATES.remove(server);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SESSION_STATES.remove(event.getServer());
    }

    private static boolean executeKiss(Player player, EntityMaid maid) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        SessionState sessionState = SESSION_STATES.computeIfAbsent(server, k -> new SessionState());

        // Tiered cooldown check based on maid's favorability level
        long currentTick = server.getTickCount();
        int favLevel = maid.getFavorabilityManager().getLevel();
        long cooldown = getCooldownForLevel(favLevel);

        UUID playerId = player.getUUID();
        UUID maidId = maid.getUUID();
        Map<UUID, Long> playerCooldowns = sessionState.cooldownsByPlayerAndMaid.computeIfAbsent(playerId, k -> new HashMap<>());
        Long lastKiss = playerCooldowns.get(maidId);
        if (lastKiss != null) {
            long delta = currentTick - lastKiss;
            if (delta < 0) {
                TouhouMaidAffection.LOGGER.debug("Detected tick rollback for player {} maid {} (current: {}, last: {}), resetting kiss state.",
                        playerId, maidId, currentTick, lastKiss);
                sessionState.cooldownsByPlayerAndMaid.remove(playerId);
                sessionState.kissTimestamps.remove(playerId);
            } else if (cooldown > 0 && delta < cooldown) {
                return false;
            }
        }

        // Record cooldown
        playerCooldowns.put(maidId, currentTick);

        // Apply favorability (dynamic Type with configured values)
        int favPoints = ModConfig.FAVORABILITY_POINTS.get();
        int favCooldown = ModConfig.FAVORABILITY_COOLDOWN.get();
        Type kissType = new Type("Kiss", favPoints, favCooldown);
        maid.getFavorabilityManager().apply(kissType);

        // Make the maid look at the player
        maid.getLookControl().setLookAt(player, 30.0F, 30.0F);

        // Play kiss sound at the midpoint between player and maid
        double midX = (player.getX() + maid.getX()) / 2.0;
        double midY = (player.getEyeY() + maid.getEyeY()) / 2.0;
        double midZ = (player.getZ() + maid.getZ()) / 2.0;
        player.level().playSound(null, midX, midY, midZ,
                ModSounds.KISS.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);

        // Broadcast particle packet to all tracking clients
        KissMaidPayload payload = new KissMaidPayload(maid.getId(), player.getId());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(maid, payload);

        // Buff system: track kiss timestamps and check threshold
        if (ModConfig.BUFF_ENABLED.get()) {
            handleBuffTrigger(sessionState, player, maid, currentTick, favLevel);
        }
        return true;
    }

    private static int getAmplifierForLevel(int level) {
        return switch (level) {
            case 1 -> ModConfig.BUFF_AMPLIFIER_LEVEL_1.get();
            case 2 -> ModConfig.BUFF_AMPLIFIER_LEVEL_2.get();
            case 3 -> ModConfig.BUFF_AMPLIFIER_LEVEL_3.get();
            default -> ModConfig.BUFF_AMPLIFIER_LEVEL_0.get();
        };
    }

    private static void handleBuffTrigger(SessionState sessionState, Player player, EntityMaid maid, long currentTick, int favLevel) {
        UUID playerId = player.getUUID();
        int threshold = ModConfig.BUFF_KISS_THRESHOLD.get();
        long window = ModConfig.BUFF_KISS_WINDOW.get();

        List<Long> timestamps = sessionState.kissTimestamps.computeIfAbsent(playerId, k -> new ArrayList<>());
        timestamps.add(currentTick);

        // Remove timestamps outside the window
        timestamps.removeIf(t -> (currentTick - t) > window);

        if (timestamps.size() >= threshold) {
            // Clear timestamps to reset counter
            timestamps.clear();

            int duration = ModConfig.BUFF_DURATION.get();
            int amplifier = getAmplifierForLevel(favLevel);

            // Apply Maid's Prayer (custom effect with built-in regeneration) to both
            player.addEffect(new MobEffectInstance(
                    ModEffects.MAIDS_PRAYER.getDelegate(), duration, amplifier, false, true, true));
            maid.addEffect(new MobEffectInstance(
                    ModEffects.MAIDS_PRAYER.getDelegate(), duration, amplifier, false, true, true));
        }
    }

    private static final class SessionState {
        private final Map<UUID, Map<UUID, Long>> cooldownsByPlayerAndMaid = new HashMap<>();
        private final Map<UUID, List<Long>> kissTimestamps = new HashMap<>();

        private boolean isEmpty() {
            return cooldownsByPlayerAndMaid.isEmpty() && kissTimestamps.isEmpty();
        }
    }
}
