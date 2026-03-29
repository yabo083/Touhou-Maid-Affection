package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.LapPillowAngleLockPayload;
import com.github.touhoumaidaffection.network.LapPillowExitPayload;
import com.github.touhoumaidaffection.network.LapPillowStartPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class BondKeyInputHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        LapPillowClientState.onClientTick(minecraft);

        while (BondKeyMappings.LAP_PILLOW.consumeClick()) {
            if (LapPillowClientState.isEngaged() || LapPillowClientState.isLapPillowSeat(minecraft.player.getVehicle())) {
                TouhouMaidAffection.CHANNEL.sendToServer(new LapPillowExitPayload(0));
                LapPillowClientState.markExitRequested();
                continue;
            }
            if (minecraft.hitResult instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof EntityMaid maid) {
                boolean hasBondState = BondClientStateCache.hasState(maid.getUUID());
                boolean unlockedByBondState = hasBondState && BondClientStateCache.isAbilityUnlocked(maid.getUUID(), "lap_pillow");

                if (hasBondState && !unlockedByBondState) {
                    minecraft.player.displayClientMessage(Component.translatable("bond.lap_pillow.failed_locked"), true);
                    continue;
                }

                TouhouMaidAffection.CHANNEL.sendToServer(new LapPillowStartPayload(maid.getUUID()));
                if (unlockedByBondState) {
                    LapPillowClientState.markStartRequested(maid.getUUID(), BondClientStateCache.getLapPillowPose(maid.getUUID()));
                }
            }
        }

        while (BondKeyMappings.LAP_PILLOW_ANGLE_LOCK.consumeClick()) {
            if (!LapPillowClientState.isAngleLockAvailable()) {
                minecraft.player.displayClientMessage(Component.translatable("bond.lap_pillow.angle_lock.unavailable"), true);
                continue;
            }
            boolean enabled = LapPillowClientState.toggleAngleLock(minecraft);
            TouhouMaidAffection.CHANNEL.sendToServer(new LapPillowAngleLockPayload(enabled, LapPillowClientState.currentLockedYaw(minecraft)));
            minecraft.player.displayClientMessage(
                    enabled
                            ? Component.translatable("bond.lap_pillow.angle_lock.enabled")
                            : Component.translatable("bond.lap_pillow.angle_lock.disabled"),
                    true
            );
        }
    }
}
