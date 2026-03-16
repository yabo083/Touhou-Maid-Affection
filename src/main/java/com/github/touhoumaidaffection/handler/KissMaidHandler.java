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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KissMaidHandler {

    private static final Map<MinecraftServer, SessionState> SESSION = new IdentityHashMap<>();

    private static Boolean carryOnLoaded = null;

    private static final class SessionState {
        private final Map<UUID, Long> cooldowns = new HashMap<>();
        private final Map<UUID, List<Long>> kissTimestamps = new HashMap<>();
    }

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

        // Cancel to prevent opening the maid GUI
        event.setCanceled(true);

        executeKiss(player, maid);
    }

    public static void tryKissCarriedMaid(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof EntityMaid maid) {
                executeKiss(player, maid, true);
                return;
            }
        }
    }

    public static void executeKiss(Player player, EntityMaid maid) {
        executeKiss(player, maid, false);
    }

    private static void executeKiss(Player player, EntityMaid maid, boolean carriedKiss) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        SessionState session = SESSION.computeIfAbsent(server, s -> new SessionState());
        UUID playerId = player.getUUID();

        // Tiered cooldown check based on maid's favorability level
        long currentTick = server.getTickCount();
        int favLevel = maid.getFavorabilityManager().getLevel();
        long cooldown = getCooldownForLevel(favLevel);

        Long lastKiss = session.cooldowns.get(playerId);
        if (lastKiss != null) {
            long delta = currentTick - lastKiss;
            if (delta < 0) {
                session.cooldowns.remove(playerId);
                session.kissTimestamps.remove(playerId);
                TouhouMaidAffection.LOGGER.debug("Detected negative kiss cooldown delta, resetting state for player {}", playerId);
            } else if (cooldown > 0 && delta < cooldown) {
                return;
            }
        }

        // Record cooldown
        session.cooldowns.put(playerId, currentTick);

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
        KissMaidPayload payload = new KissMaidPayload(maid.getId(), player.getId(), carriedKiss);
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> maid), payload);

        // Buff system: track kiss timestamps and check threshold
        if (ModConfig.BUFF_ENABLED.get()) {
            handleBuffTrigger(session, player, maid, currentTick, favLevel);
        }
    }

    private static int getAmplifierForLevel(int level) {
        return switch (level) {
            case 1 -> ModConfig.BUFF_AMPLIFIER_LEVEL_1.get();
            case 2 -> ModConfig.BUFF_AMPLIFIER_LEVEL_2.get();
            case 3 -> ModConfig.BUFF_AMPLIFIER_LEVEL_3.get();
            default -> ModConfig.BUFF_AMPLIFIER_LEVEL_0.get();
        };
    }

    private static void handleBuffTrigger(SessionState session, Player player, EntityMaid maid, long currentTick, int favLevel) {
        UUID playerId = player.getUUID();
        int threshold = ModConfig.BUFF_KISS_THRESHOLD.get();
        long window = ModConfig.BUFF_KISS_WINDOW.get();

        List<Long> timestamps = session.kissTimestamps.computeIfAbsent(playerId, k -> new ArrayList<>());
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
                    ModEffects.MAIDS_PRAYER.get(), duration, amplifier, false, true, true));
            maid.addEffect(new MobEffectInstance(
                    ModEffects.MAIDS_PRAYER.get(), duration, amplifier, false, true, true));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }

        SessionState session = SESSION.get(server);
        if (session == null) {
            return;
        }

        UUID playerId = event.getEntity().getUUID();
        session.cooldowns.remove(playerId);
        session.kissTimestamps.remove(playerId);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SESSION.remove(event.getServer());
    }
}
