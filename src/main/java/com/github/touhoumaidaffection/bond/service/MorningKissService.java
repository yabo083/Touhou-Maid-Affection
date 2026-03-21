package com.github.touhoumaidaffection.bond.service;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.handler.KissMaidHandler;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.github.touhoumaidaffection.ysm.YSMMaidAnimation;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class MorningKissService {
    private static final Map<UUID, PendingMorningKiss> TASKS = new HashMap<>();

    private MorningKissService() {
    }

    public static boolean start(net.minecraft.world.entity.player.Player player, EntityMaid maid) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        double maxDistance = ModConfig.BOND_MORNING_KISS_MAX_DISTANCE.get();
        if (!maid.isAlive() || player.distanceToSqr(maid) > maxDistance * maxDistance) {
            serverPlayer.displayClientMessage(Component.translatable("bond.morning_kiss.failed_distance"), true);
            return false;
        }
        TASKS.put(serverPlayer.getUUID(), new PendingMorningKiss(
                serverPlayer.level().dimension(),
                serverPlayer.getUUID(),
                maid.getUUID(),
                serverPlayer.serverLevel().getGameTime() + ModConfig.BOND_MORNING_KISS_TIMEOUT_TICKS.get()
        ));
        maid.getNavigation().moveTo(serverPlayer, 1.0D);
        serverPlayer.displayClientMessage(Component.translatable("bond.morning_kiss.started", maid.getName()), true);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Iterator<PendingMorningKiss> iterator = TASKS.values().iterator();
        while (iterator.hasNext()) {
            PendingMorningKiss task = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(task.playerUuid());
            if (player == null || player.level().dimension() != task.dimension()) {
                iterator.remove();
                continue;
            }
            Entity entity = player.serverLevel().getEntity(task.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
                iterator.remove();
                continue;
            }
            if (player.serverLevel().getGameTime() > task.timeoutTick()) {
                iterator.remove();
                continue;
            }

            maid.getNavigation().moveTo(player, 1.0D);
            if (maid.distanceToSqr(player) <= 2.25D) {
                KissMaidHandler.performKiss(player, maid);
                YSMActionBridge.playIfAvailable(maid, YSMMaidAnimation.MORNING_KISS);
                iterator.remove();
            }
        }
    }

    private record PendingMorningKiss(ResourceKey<Level> dimension, UUID playerUuid, UUID maidUuid, long timeoutTick) {
    }
}
