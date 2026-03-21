package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.ModEffects;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.github.touhoumaidaffection.ysm.YSMMaidAnimation;
import com.github.touhoumaidaffection.network.LapPillowExitPayload;
import com.github.touhoumaidaffection.network.LapPillowStartPayload;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class LapPillowHandler {
    private LapPillowHandler() {
    }

    public static void handleStart(LapPillowStartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!BondManager.isAbilityUnlocked(player, payload.maidUuid(), "lap_pillow")) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
                return;
            }
            double maxDistance = ModConfig.BOND_LAP_PILLOW_MAX_DISTANCE.get();
            if (player.distanceToSqr(maid) > maxDistance * maxDistance) {
                return;
            }
            LapPillowState.activate(player, maid.getUUID(), player.level().getGameTime());
            player.setForcedPose(Pose.SLEEPING);
            player.addEffect(new MobEffectInstance(ModEffects.GOLDEN_DREAM.getDelegate(), 220, 0, false, true, true));
            YSMActionBridge.playIfAvailable(maid, YSMMaidAnimation.LAP_PILLOW);
        });
    }

    public static void handleExit(LapPillowExitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                clearLapPillow(player);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide || !LapPillowState.isActive(player)) {
            return;
        }
        UUID maidUuid = LapPillowState.getMaidUuid(player);
        if (maidUuid == null) {
            clearLapPillow(player);
            return;
        }
        Entity entity = player.serverLevel().getEntity(maidUuid);
        if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
            clearLapPillow(player);
            return;
        }
        double maxDistance = ModConfig.BOND_LAP_PILLOW_MAX_DISTANCE.get() + 1.5D;
        if (player.distanceToSqr(maid) > maxDistance * maxDistance) {
            clearLapPillow(player);
            return;
        }
        player.teleportTo(maid.getX(), maid.getY() + 0.35D, maid.getZ());
        player.setForcedPose(Pose.SLEEPING);
        if (!player.hasEffect(ModEffects.GOLDEN_DREAM.getDelegate())) {
            player.addEffect(new MobEffectInstance(ModEffects.GOLDEN_DREAM.getDelegate(), 220, 0, false, true, true));
        }
    }

    private static void clearLapPillow(ServerPlayer player) {
        LapPillowState.clear(player);
        player.setForcedPose(null);
        player.removeEffect(ModEffects.GOLDEN_DREAM.getDelegate());
    }
}
