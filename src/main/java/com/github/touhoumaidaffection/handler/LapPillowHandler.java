package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.ModEffects;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.lap.LapPillowAnchorEntity;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import com.github.touhoumaidaffection.bond.service.MorningKissService;
import com.github.touhoumaidaffection.bond.service.RandomGiftService;
import com.github.touhoumaidaffection.network.LapPillowExitPayload;
import com.github.touhoumaidaffection.network.LapPillowStartPayload;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.github.touhoumaidaffection.ysm.YSMCompatibility;
import com.github.touhoumaidaffection.ysm.YSMMaidAnimation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class LapPillowHandler {
    private static final double TELEPORT_EPSILON_SQR = 0.0009D;
    private static final double LIE_CORRECTION_TRIGGER_SQR = 0.0144D;
    private static final double MAID_LYING_VISUAL_Y_LIFT = 0.55D;

    private LapPillowHandler() {
    }

    public static void handleStart(LapPillowStartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!BondManager.isAbilityUnlocked(player, payload.maidUuid(), "lap_pillow")) {
                player.displayClientMessage(Component.translatable("bond.lap_pillow.failed_locked"), true);
                logReject(player, payload.maidUuid(), "ability_locked");
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
                logReject(player, payload.maidUuid(), "maid_missing_or_dead");
                return;
            }
            double maxDistance = ModConfig.BOND_LAP_PILLOW_MAX_DISTANCE.get();
            if (player.distanceToSqr(maid) > maxDistance * maxDistance) {
                logReject(player, maid.getUUID(), "too_far");
                return;
            }

            clearLapPillow(player, "restart_before_start");
            MorningKissService.cancelForPlayerExcept(player, maid.getUUID());
            RandomGiftService.cancelForPlayer(player);
            LapPillowPoseSnapshot pose = BondManager.getMaidLapPillowPose(player, maid.getUUID()).clamp();
            LapPillowAnchorEntity anchor = new LapPillowAnchorEntity(player.level(), player.getUUID(), maid.getUUID());
            anchor.setPoseSnapshot(pose);
            Vec3 anchorPos = maid.position();
            anchor.setPos(anchorPos);
            anchor.setYRot(player.getYRot());
            anchor.setXRot(maid.getXRot());
            player.level().addFreshEntity(anchor);

            if (!pose.playerLying()) {
                boolean ridingOk = player.startRiding(anchor, true);
                if (!ridingOk) {
                    anchor.discard();
                    logReject(player, maid.getUUID(), "start_riding_failed");
                    return;
                }
            } else {
                enforceNonRidingPlayerState(player, anchor);
            }

            LapPillowState.activate(
                    player,
                    maid.getUUID(),
                    anchor.getUUID(),
                    player.level().getGameTime(),
                    pose,
                    maid.isMaidInSittingPose(),
                    maid.isSleeping(),
                    player.isNoGravity()
            );
            applyMaidPose(maid, pose, anchor);
            applyPlayerPose(player, pose, anchor);
            applyEternalUtopia(player, maid);
            syncMaidActionState(player, maid, pose);
            TouhouMaidAffection.LOGGER.info(
                    "[LapPillow] Start success: player={} maid={} mode={} anchor={} ysmLoaded={} maidAction='{}' playerAction='{}' anchorPos=({}, {}, {}) fallback={}",
                    player.getScoreboardName(),
                    maid.getUUID(),
                    pose.mode().serializedName(),
                    anchor.getUUID(),
                    YSMCompatibility.isYSMLoaded(),
                    pose.maidActionId(),
                    pose.playerActionId(),
                    format(anchor.getX()),
                    format(anchor.getY()),
                    format(anchor.getZ()),
                    pose.maidActionId().isBlank() ? "default_pose" : "maid_ysm_action"
            );
            if (!pose.playerActionId().isBlank()) {
                TouhouMaidAffection.LOGGER.info(
                        "[LapPillow] Player YSM action reserved for future bridge: player={} maid={} action='{}'",
                        player.getScoreboardName(),
                        maid.getUUID(),
                        pose.playerActionId()
                );
            }
        });
    }

    public static void handleExit(LapPillowExitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                clearLapPillow(player, "manual_exit");
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide || !LapPillowState.isActive(player)) {
            return;
        }
        UUID maidUuid = LapPillowState.getMaidUuid(player);
        UUID anchorUuid = LapPillowState.getAnchorUuid(player);
        LapPillowPoseSnapshot pose = LapPillowState.getPose(player);
        if (maidUuid == null || anchorUuid == null) {
            clearLapPillow(player, "state_missing_ids");
            return;
        }

        Entity maidEntity = player.serverLevel().getEntity(maidUuid);
        Entity anchorEntity = player.serverLevel().getEntity(anchorUuid);
        if (!(maidEntity instanceof EntityMaid maid) || !maid.isAlive()) {
            clearLapPillow(player, "maid_missing_during_tick");
            return;
        }
        if (!(anchorEntity instanceof LapPillowAnchorEntity anchor) || !anchor.isAlive()) {
            clearLapPillow(player, "anchor_missing_during_tick");
            return;
        }

        double maxDistance = ModConfig.BOND_LAP_PILLOW_MAX_DISTANCE.get() + 1.5D;
        if (player.distanceToSqr(maid) > maxDistance * maxDistance) {
            clearLapPillow(player, "distance_break");
            return;
        }

        anchor.setPoseSnapshot(pose);
        anchor.setDeltaMovement(Vec3.ZERO);
        if (LapPillowState.isAngleLockEnabled(player)) {
            float lockedYaw = LapPillowState.getAngleLockYaw(player);
            player.setYRot(lockedYaw);
            player.setYHeadRot(lockedYaw);
            player.setYBodyRot(lockedYaw);
        }
        anchor.setXRot(0.0F);

        if (pose.playerLying()) {
            enforceNonRidingPlayerState(player, anchor);
        } else if (player.getVehicle() != anchor) {
            if (!player.startRiding(anchor, true)) {
                clearLapPillow(player, "rebind_riding_failed");
                return;
            }
        }

        applyMaidPose(maid, pose, anchor);
        applyPlayerPose(player, pose, anchor);
        applyEternalUtopia(player, maid);
        syncMaidActionState(player, maid, pose);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearLapPillow(player, "dimension_change");
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearLapPillow(player, "logout");
        }
    }

    private static void applyMaidPose(EntityMaid maid, LapPillowPoseSnapshot pose, LapPillowAnchorEntity anchor) {
        Vec3 maidPos = anchor.getMaidWorldPosition();
        boolean builtinMaidLying = isBuiltinLieAction(pose.maidActionId());
        if (builtinMaidLying) {
            maidPos = maidPos.add(0.0D, MAID_LYING_VISUAL_Y_LIFT, 0.0D);
        }
        if (maid.position().distanceToSqr(maidPos) > TELEPORT_EPSILON_SQR) {
            maid.setDeltaMovement(Vec3.ZERO);
            maid.setPos(maidPos.x, maidPos.y, maidPos.z);
        }
        maid.getNavigation().stop();
        maid.setYRot(anchor.getYRot());
        maid.setYHeadRot(anchor.getYRot());
        maid.setYBodyRot(anchor.getYRot());
        if (isBuiltinAction(pose.maidActionId())) {
            if (builtinMaidLying) {
                maid.setInSittingPose(false);
                maid.setPose(Pose.SLEEPING);
                if (maid.isSleeping()) {
                    maid.stopSleeping();
                }
            } else {
                if (maid.isSleeping()) {
                    maid.stopSleeping();
                }
                maid.setPose(Pose.STANDING);
                maid.setInSittingPose(true);
            }
        } else {
            maid.setInSittingPose(false);
            if (maid.isSleeping()) {
                maid.stopSleeping();
            }
            maid.setPose(Pose.STANDING);
        }
    }

    private static void applyPlayerPose(ServerPlayer player, LapPillowPoseSnapshot pose, LapPillowAnchorEntity anchor) {
        if (pose.playerLying()) {
            enforceNonRidingPlayerState(player, anchor);
            player.setNoGravity(true);

            Vec3 desired = anchor.getPlayerWorldPosition().add(0.0D, 0.35D, 0.0D);
            if (player.position().distanceToSqr(desired) > LIE_CORRECTION_TRIGGER_SQR) {
                Vec3 target = resolveSafePlayerPos(player, desired);
                if (player.position().distanceToSqr(target) > LIE_CORRECTION_TRIGGER_SQR) {
                    player.teleportTo(target.x, target.y, target.z);
                }
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            player.setForcedPose(Pose.SLEEPING);
        } else {
            player.setNoGravity(LapPillowState.wasPlayerNoGravity(player));
            player.setForcedPose(null);
        }
    }

    private static void applyEternalUtopia(ServerPlayer player, EntityMaid maid) {
        boolean showParticles = ModConfig.BOND_LAP_PILLOW_ETERNAL_UTOPIA_PARTICLES_ENABLED.get();
        MobEffectInstance active = player.getEffect(ModEffects.ETERNAL_UTOPIA.getDelegate());
        if (active == null || active.getDuration() < 60) {
            player.addEffect(new MobEffectInstance(ModEffects.ETERNAL_UTOPIA.getDelegate(), 220, 0, false, showParticles, true));
            TouhouMaidAffection.LOGGER.info(
                    "[EternalUtopia] Applied/refresh: player={} duration=220",
                    player.getScoreboardName()
            );
        }
        MobEffectInstance maidActive = maid.getEffect(ModEffects.ETERNAL_UTOPIA.getDelegate());
        if (maidActive == null || maidActive.getDuration() < 60) {
            maid.addEffect(new MobEffectInstance(ModEffects.ETERNAL_UTOPIA.getDelegate(), 220, 0, false, showParticles, true));
        }
    }

    private static void syncMaidActionState(ServerPlayer player, EntityMaid maid, LapPillowPoseSnapshot pose) {
        String desiredSignature = normalizeMaidActionSignature(pose);
        if (desiredSignature.equals(LapPillowState.getAppliedMaidAction(player))) {
            return;
        }
        applyMaidAction(maid, pose);
        LapPillowState.setAppliedMaidAction(player, desiredSignature);
    }

    private static void applyMaidAction(EntityMaid maid, LapPillowPoseSnapshot pose) {
        stopMaidYsm(maid);
        if (!isBuiltinAction(pose.maidActionId())) {
            YSMActionBridge.playIfAvailable(maid, pose.maidActionId());
            return;
        }
        if (pose.mode() == com.github.touhoumaidaffection.bond.lap.LapPillowMode.MAID_SIT_PLAYER_LIE) {
            YSMActionBridge.playIfAvailable(maid, YSMMaidAnimation.LAP_PILLOW);
        }
    }

    private static void stopMaidYsm(EntityMaid maid) {
        try {
            maid.stopRouletteAnim();
        } catch (Exception ignored) {
        }
    }

    private static boolean isBuiltinAction(String actionId) {
        return actionId == null || actionId.isBlank() || actionId.startsWith("builtin:");
    }

    private static boolean isBuiltinLieAction(String actionId) {
        return "builtin:lie".equals(actionId);
    }

    private static String normalizeMaidActionSignature(LapPillowPoseSnapshot pose) {
        if (isBuiltinAction(pose.maidActionId())) {
            return isBuiltinLieAction(pose.maidActionId()) ? "builtin:lie" : "builtin:sit";
        }
        return pose.maidActionId().trim();
    }

    private static void clearLapPillow(ServerPlayer player, String reason) {
        if (!LapPillowState.isActive(player)) {
            return;
        }

        UUID maidUuid = LapPillowState.getMaidUuid(player);
        UUID anchorUuid = LapPillowState.getAnchorUuid(player);
        boolean restoreSit = LapPillowState.wasMaidSitting(player);
        boolean restoreSleep = LapPillowState.wasMaidSleeping(player);
        boolean restoreNoGravity = LapPillowState.wasPlayerNoGravity(player);

        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.setNoGravity(restoreNoGravity);
        player.setForcedPose(null);
        player.removeEffect(ModEffects.ETERNAL_UTOPIA.getDelegate());

        if (anchorUuid != null) {
            Entity anchor = player.serverLevel().getEntity(anchorUuid);
            if (anchor != null) {
                anchor.discard();
            }
        }

        if (maidUuid != null) {
            Entity entity = player.serverLevel().getEntity(maidUuid);
            if (entity instanceof EntityMaid maid) {
                stopMaidYsm(maid);
                maid.removeEffect(ModEffects.ETERNAL_UTOPIA.getDelegate());
                maid.setPose(Pose.STANDING);
                if (maid.isSleeping() && !restoreSleep) {
                    maid.stopSleeping();
                }
                maid.setInSittingPose(restoreSit);
                if (restoreSleep && !maid.isSleeping()) {
                    maid.startSleeping(maid.blockPosition());
                }
            }
        }

        LapPillowState.clear(player);
        TouhouMaidAffection.LOGGER.info(
                "[LapPillow] Cleared: player={} maid={} anchor={} reason={}",
                player.getScoreboardName(),
                maidUuid,
                anchorUuid,
                reason
        );
    }

    private static void logReject(ServerPlayer player, UUID maidUuid, String reason) {
        TouhouMaidAffection.LOGGER.info(
                "[LapPillow] Start rejected: player={} maid={} reason={}",
                player.getScoreboardName(),
                maidUuid,
                reason
        );
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static void enforceNonRidingPlayerState(ServerPlayer player, LapPillowAnchorEntity anchor) {
        if (player.getVehicle() != null) {
            player.stopRiding();
        }
        if (!anchor.getPassengers().isEmpty()) {
            for (Entity passenger : List.copyOf(anchor.getPassengers())) {
                if (passenger == player || passenger.getUUID().equals(player.getUUID())) {
                    passenger.stopRiding();
                }
            }
        }
    }

    private static Vec3 resolveSafePlayerPos(ServerPlayer player, Vec3 desired) {
        double x = desired.x;
        double y = desired.y;
        double z = desired.z;
        for (int i = 0; i < 4; i++) {
            BlockPos feet = BlockPos.containing(x, y, z);
            BlockPos head = BlockPos.containing(x, y + player.getBbHeight() * 0.9D, z);
            BlockState feetState = player.level().getBlockState(feet);
            BlockState headState = player.level().getBlockState(head);
            if (!feetState.blocksMotion() && !headState.blocksMotion()) {
                break;
            }
            y += 0.25D;
        }
        return new Vec3(x, y, z);
    }

}
