package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.LapPillowAngleLockPayload;
import com.github.touhoumaidaffection.network.LapPillowExitPayload;
import com.github.touhoumaidaffection.network.LapPillowStartPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class BondKeyInputHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        LapPillowClientState.onClientTick(minecraft);

        while (BondKeyMappings.LAP_PILLOW.consumeClick()) {
            if (LapPillowClientState.isActive() || LapPillowClientState.isLapPillowSeat(minecraft.player.getVehicle())) {
                PacketDistributor.sendToServer(new LapPillowExitPayload(0));
                LapPillowClientState.markExitRequested();
                continue;
            }
            if (minecraft.hitResult instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof EntityMaid maid) {
                if (BondClientStateCache.hasState(maid.getUUID())
                        && !BondClientStateCache.isAbilityUnlocked(maid.getUUID(), "lap_pillow")) {
                    minecraft.player.displayClientMessage(Component.translatable("bond.lap_pillow.failed_locked"), true);
                    continue;
                }
                PacketDistributor.sendToServer(new LapPillowStartPayload(maid.getUUID()));
                LapPillowClientState.markStartRequested(maid.getUUID(), BondClientStateCache.getLapPillowPose(maid.getUUID()));
            }
        }

        while (BondKeyMappings.LAP_PILLOW_ANGLE_LOCK.consumeClick()) {
            if (!LapPillowClientState.isAngleLockAvailable()) {
                minecraft.player.displayClientMessage(Component.translatable("bond.lap_pillow.angle_lock.unavailable"), true);
                continue;
            }
            boolean enabled = LapPillowClientState.toggleAngleLock(minecraft);
            PacketDistributor.sendToServer(new LapPillowAngleLockPayload(enabled, LapPillowClientState.currentLockedYaw(minecraft)));
            minecraft.player.displayClientMessage(
                    enabled
                            ? Component.translatable("bond.lap_pillow.angle_lock.enabled")
                            : Component.translatable("bond.lap_pillow.angle_lock.disabled"),
                    true
            );
        }
    }
}
