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

    private static boolean active;
    private static boolean exitRequested;
    private static UUID maidUuid;
    private static LapPillowPoseSnapshot pose = LapPillowPoseSnapshot.maidSitPlayerLieDefault();
    private static int lostSeatTicks;
    private static boolean sleepBridgeActive;
    private static Direction sleepBridgeDirection;
    private static boolean angleLockEnabled;
    private static float lockedYaw;
    public static int renderingDepth = 0;

    private LapPillowClientState() {
    }

    public static void markStartRequested(UUID maidUuid, LapPillowPoseSnapshot poseSnapshot) {
        active = true;
        LapPillowClientState.maidUuid = maidUuid;
        pose = poseSnapshot == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : poseSnapshot.clamp();
        lostSeatTicks = 0;
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
        if (!active) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return;
        }
        boolean seated = isLapPillowSeat(minecraft.player.getVehicle());
        boolean keepLieBridge = pose.playerLying() && !exitRequested;
        if (seated || keepLieBridge) {
            lostSeatTicks = 0;
            if (pose.playerLying()) {
                minecraft.player.setForcedPose(net.minecraft.world.entity.Pose.SLEEPING);
                if (angleLockEnabled) {
                    minecraft.player.setYBodyRot(lockedYaw);
                    minecraft.player.setYHeadRot(lockedYaw);
                    minecraft.player.yBodyRotO = lockedYaw;
                    minecraft.player.yHeadRotO = lockedYaw;
                }
            } else {
                minecraft.player.setForcedPose(null);
                if (angleLockEnabled) {
                    minecraft.player.setYBodyRot(lockedYaw);
                    minecraft.player.setYHeadRot(lockedYaw);
                    minecraft.player.yBodyRotO = lockedYaw;
                    minecraft.player.yHeadRotO = lockedYaw;
                }
            }
            updateSleepBridgeState(minecraft.player);
            return;
        }
        lostSeatTicks++;
        updateSleepBridgeState(null);
        if (lostSeatTicks > LOST_SEAT_GRACE_TICKS || (exitRequested && lostSeatTicks > 1)) {
            clear();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static LapPillowPoseSnapshot pose() {
        return pose;
    }

    public static boolean shouldUseSleepPoseBridge(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        return active
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.setForcedPose(null);
        }
        active = false;
        maidUuid = null;
        pose = LapPillowPoseSnapshot.maidSitPlayerLieDefault();
        lostSeatTicks = 0;
        exitRequested = false;
        sleepBridgeActive = false;
        sleepBridgeDirection = null;
        angleLockEnabled = false;
        lockedYaw = 0.0F;
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
                "[LapPillow] Lie bridge state: active={} playerLieSource=vanilla_sleep_bridge renderSleepPoseActive={} forcedPoseApplied={} seatPassenger={} bedOrientation={}",
                activeNow,
                activeNow,
                activeNow,
                player != null && player.getForcedPose() == net.minecraft.world.entity.Pose.SLEEPING,
                player != null && isLapPillowSeat(player.getVehicle()),
                directionNow == null ? "none" : directionNow
        );
    }
}
