package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.bond.lap.LapPillowAnchorEntity;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class LapPillowClientState {
    private static final int LOST_SEAT_GRACE_TICKS = 10;
    private static final int START_CONFIRM_TIMEOUT_TICKS = 80;
    private static final double ANCHOR_CONFIRM_RADIUS = 6.0D;

    private static boolean active;
    private static boolean startPending;
    private static boolean exitRequested;
    private static UUID maidUuid;
    private static LapPillowPoseSnapshot pose = LapPillowPoseSnapshot.maidSitPlayerLieDefault();
    private static int pendingConfirmTicks;
    private static int lostSeatTicks;
    private static boolean sessionContextConfirmed;
    private static boolean sleepBridgeActive;
    private static Direction sleepBridgeDirection;
    private static boolean angleLockEnabled;
    private static float lockedYaw;
    public static int renderingDepth = 0;

    private LapPillowClientState() {
    }

    public static void markStartRequested(UUID maidUuid, LapPillowPoseSnapshot poseSnapshot) {
        active = false;
        startPending = true;
        LapPillowClientState.maidUuid = maidUuid;
        pose = poseSnapshot == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : poseSnapshot.clamp();
        pendingConfirmTicks = 0;
        lostSeatTicks = 0;
        sessionContextConfirmed = false;
        exitRequested = false;
        sleepBridgeActive = false;
        sleepBridgeDirection = null;
        angleLockEnabled = false;
        lockedYaw = 0.0F;
    }

    public static void markExitRequested() {
        exitRequested = true;
        lostSeatTicks = LOST_SEAT_GRACE_TICKS - 2;
    }

    public static boolean toggleAngleLock(Minecraft minecraft) {
        if (!active || minecraft.player == null) {
            angleLockEnabled = false;
            return false;
        }
        angleLockEnabled = !angleLockEnabled;
        if (angleLockEnabled) {
            lockedYaw = minecraft.player.getYRot();
        }
        return angleLockEnabled;
    }

    public static float currentLockedYaw(Minecraft minecraft) {
        if (minecraft.player == null) {
            return lockedYaw;
        }
        return angleLockEnabled ? lockedYaw : minecraft.player.getYRot();
    }

    public static boolean isAngleLockAvailable() {
        return active;
    }

    public static void onClientTick(Minecraft minecraft) {
        if (!active && !startPending) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return;
        }
        AbstractClientPlayer player = minecraft.player;
        boolean seated = isLapPillowSeat(player.getVehicle());
        boolean hasAnchorContext = hasConfirmedAnchorContext(player);
        boolean contextConfirmed = seated || (pose.playerLying() && hasAnchorContext);

        if (startPending) {
            if (exitRequested) {
                clear();
                return;
            }
            if (!contextConfirmed) {
                pendingConfirmTicks++;
                sessionContextConfirmed = false;
                updateSleepBridgeState(null);
                if (pendingConfirmTicks > START_CONFIRM_TIMEOUT_TICKS) {
                    TouhouMaidAffection.LOGGER.info(
                            "[LapPillow] Client start confirmation timeout: maid={} requestedPose={} pendingTicks={}",
                            maidUuid,
                            pose.mode().serializedName(),
                            pendingConfirmTicks
                    );
                    clear();
                }
                return;
            }
            active = true;
            startPending = false;
            pendingConfirmTicks = 0;
            lostSeatTicks = 0;
            sessionContextConfirmed = true;
            TouhouMaidAffection.LOGGER.info(
                    "[LapPillow] Client session confirmed: maid={} pose={} context={}",
                    maidUuid,
                    pose.mode().serializedName(),
                    seated ? "seat" : "anchor"
            );
        }

        if (contextConfirmed && !exitRequested) {
            lostSeatTicks = 0;
            sessionContextConfirmed = true;
            if (pose.playerLying()) {
                player.setForcedPose(net.minecraft.world.entity.Pose.SLEEPING);
                if (angleLockEnabled) {
                    player.setYBodyRot(lockedYaw);
                    player.setYHeadRot(lockedYaw);
                    player.yBodyRotO = lockedYaw;
                    player.yHeadRotO = lockedYaw;
                }
            } else {
                player.setForcedPose(null);
                if (angleLockEnabled) {
                    player.setYBodyRot(lockedYaw);
                    player.setYHeadRot(lockedYaw);
                    player.yBodyRotO = lockedYaw;
                    player.yHeadRotO = lockedYaw;
                }
            }
            updateSleepBridgeState(player);
            return;
        }
        lostSeatTicks++;
        sessionContextConfirmed = false;
        updateSleepBridgeState(null);
        if (lostSeatTicks > LOST_SEAT_GRACE_TICKS || (exitRequested && lostSeatTicks > 1)) {
            clear();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isEngaged() {
        return active || startPending;
    }

    public static LapPillowPoseSnapshot pose() {
        return pose;
    }

    public static boolean shouldUseSleepPoseBridge(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        return active
                && !startPending
                && sessionContextConfirmed
                && pose.playerLying()
                && !exitRequested
                && minecraft.player != null
                && player.getUUID().equals(minecraft.player.getUUID());
    }

    public static Direction resolveSleepDirection(AbstractClientPlayer player) {
        return Direction.fromYRot(angleLockEnabled ? lockedYaw : player.getYRot());
    }

    public static boolean isLapPillowSeat(Entity entity) {
        return entity != null && entity.getTags().contains(LapPillowAnchorEntity.ANCHOR_TAG);
    }

    private static void clear() {
        active = false;
        startPending = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.setForcedPose(null);
        }
        maidUuid = null;
        pose = LapPillowPoseSnapshot.maidSitPlayerLieDefault();
        pendingConfirmTicks = 0;
        lostSeatTicks = 0;
        sessionContextConfirmed = false;
        exitRequested = false;
        sleepBridgeActive = false;
        sleepBridgeDirection = null;
        angleLockEnabled = false;
        lockedYaw = 0.0F;
    }

    private static boolean hasConfirmedAnchorContext(AbstractClientPlayer player) {
        if (!pose.playerLying()) {
            return false;
        }
        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(ANCHOR_CONFIRM_RADIUS),
                LapPillowClientState::isLapPillowSeat
        )) {
            if (!matchesOwner(player, entity) || !matchesMaid(entity)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean matchesOwner(AbstractClientPlayer player, Entity entity) {
        if (!(entity instanceof LapPillowAnchorEntity anchor)) {
            return true;
        }
        UUID ownerUuid = anchor.getOwnerPlayerUuid();
        return ownerUuid == null || ownerUuid.equals(player.getUUID());
    }

    private static boolean matchesMaid(Entity entity) {
        if (maidUuid == null || !(entity instanceof LapPillowAnchorEntity anchor)) {
            return true;
        }
        UUID anchorMaidUuid = anchor.getMaidUuid();
        return anchorMaidUuid == null || anchorMaidUuid.equals(maidUuid);
    }

    private static void updateSleepBridgeState(AbstractClientPlayer player) {
        boolean activeNow = player != null && shouldUseSleepPoseBridge(player);
        Direction directionNow = activeNow ? resolveSleepDirection(player) : null;
        if (activeNow == sleepBridgeActive && directionNow == sleepBridgeDirection) {
            return;
        }
        sleepBridgeActive = activeNow;
        sleepBridgeDirection = directionNow;
        TouhouMaidAffection.LOGGER.info(
                "[LapPillow] Lie bridge state: active={} pending={} contextConfirmed={} forcedPoseApplied={} seatPassenger={} bedOrientation={}",
                activeNow,
                startPending,
                sessionContextConfirmed,
                player != null && player.getForcedPose() == net.minecraft.world.entity.Pose.SLEEPING,
                player != null && isLapPillowSeat(player.getVehicle()),
                directionNow == null ? "none" : directionNow
        );
    }
}
