package com.github.touhoumaidaffection.bond.lap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class LapPillowState {
    private static final String ROOT_KEY = "touhou_maid_affection.lap_pillow";

    private LapPillowState() {
    }

    public static boolean isActive(ServerPlayer player) {
        return getRoot(player).getBoolean("active");
    }

    public static UUID getMaidUuid(ServerPlayer player) {
        return readUuid(getRoot(player), "maid_uuid");
    }

    public static UUID getAnchorUuid(ServerPlayer player) {
        return readUuid(getRoot(player), "anchor_uuid");
    }

    public static boolean isSessionMaid(ServerPlayer player, UUID maidUuid) {
        if (!isActive(player) || maidUuid == null) {
            return false;
        }
        UUID activeMaid = getMaidUuid(player);
        return maidUuid.equals(activeMaid);
    }

    public static LapPillowPoseSnapshot getPose(ServerPlayer player) {
        CompoundTag root = getRoot(player);
        if (!root.contains("pose")) {
            return LapPillowPoseSnapshot.maidSitPlayerLieDefault();
        }
        CompoundTag poseTag = root.getCompound("pose");
        return new LapPillowPoseSnapshot(
                LapPillowMode.fromName(poseTag.getString("mode")),
                poseTag.getDouble("maid_offset_x"),
                poseTag.getDouble("maid_offset_y"),
                poseTag.getDouble("maid_offset_z"),
                readPlayerOffset(poseTag, "player_offset_x", "offset_x"),
                readPlayerOffset(poseTag, "player_offset_y", "offset_y"),
                readPlayerOffset(poseTag, "player_offset_z", "offset_z"),
                poseTag.getString("maid_action"),
                poseTag.getString("player_action")
        ).clamp();
    }

    public static boolean wasMaidSitting(ServerPlayer player) {
        return getRoot(player).getBoolean("maid_was_sitting");
    }

    public static boolean wasMaidSleeping(ServerPlayer player) {
        return getRoot(player).getBoolean("maid_was_sleeping");
    }

    public static boolean wasPlayerNoGravity(ServerPlayer player) {
        return getRoot(player).getBoolean("player_was_no_gravity");
    }

    public static void activate(ServerPlayer player,
                                UUID maidUuid,
                                UUID anchorUuid,
                                long gameTime,
                                LapPillowPoseSnapshot pose,
                                boolean maidWasSitting,
                                boolean maidWasSleeping,
                                boolean playerWasNoGravity) {
        CompoundTag root = getRoot(player);
        root.putBoolean("active", true);
        root.putString("maid_uuid", maidUuid.toString());
        root.putString("anchor_uuid", anchorUuid.toString());
        root.putLong("start_time", gameTime);
        root.putBoolean("maid_was_sitting", maidWasSitting);
        root.putBoolean("maid_was_sleeping", maidWasSleeping);
        root.putBoolean("player_was_no_gravity", playerWasNoGravity);
        root.putBoolean("angle_lock_enabled", false);
        root.putFloat("angle_lock_yaw", 0.0F);
        root.putString("applied_maid_action", "");
        root.put("pose", writePose(pose));
        save(player, root);
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(ROOT_KEY);
    }

    public static void updatePose(ServerPlayer player, LapPillowPoseSnapshot pose) {
        CompoundTag root = getRoot(player);
        root.put("pose", writePose(pose));
        save(player, root);
    }

    public static void enableAngleLock(ServerPlayer player, float yaw) {
        CompoundTag root = getRoot(player);
        root.putBoolean("angle_lock_enabled", true);
        root.putFloat("angle_lock_yaw", yaw);
        save(player, root);
    }

    public static void disableAngleLock(ServerPlayer player) {
        CompoundTag root = getRoot(player);
        root.putBoolean("angle_lock_enabled", false);
        save(player, root);
    }

    public static boolean isAngleLockEnabled(ServerPlayer player) {
        return getRoot(player).getBoolean("angle_lock_enabled");
    }

    public static float getAngleLockYaw(ServerPlayer player) {
        return getRoot(player).getFloat("angle_lock_yaw");
    }

    public static String getAppliedMaidAction(ServerPlayer player) {
        return getRoot(player).getString("applied_maid_action");
    }

    public static void setAppliedMaidAction(ServerPlayer player, String actionSignature) {
        CompoundTag root = getRoot(player);
        root.putString("applied_maid_action", actionSignature == null ? "" : actionSignature);
        save(player, root);
    }

    private static CompoundTag getRoot(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return persistent.getCompound(ROOT_KEY);
    }

    private static void save(ServerPlayer player, CompoundTag root) {
        player.getPersistentData().put(ROOT_KEY, root);
    }

    private static UUID readUuid(CompoundTag root, String key) {
        String raw = root.getString(key);
        if (raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static CompoundTag writePose(LapPillowPoseSnapshot pose) {
        LapPillowPoseSnapshot safe = pose == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : pose.clamp();
        CompoundTag poseTag = new CompoundTag();
        poseTag.putString("mode", safe.mode().serializedName());
        poseTag.putDouble("maid_offset_x", safe.maidOffsetX());
        poseTag.putDouble("maid_offset_y", safe.maidOffsetY());
        poseTag.putDouble("maid_offset_z", safe.maidOffsetZ());
        poseTag.putDouble("player_offset_x", safe.playerOffsetX());
        poseTag.putDouble("player_offset_y", safe.playerOffsetY());
        poseTag.putDouble("player_offset_z", safe.playerOffsetZ());
        poseTag.putString("maid_action", safe.maidActionId());
        poseTag.putString("player_action", safe.playerActionId());
        return poseTag;
    }

    private static double readPlayerOffset(CompoundTag tag, String currentKey, String legacyKey) {
        if (tag.contains(currentKey)) {
            return tag.getDouble(currentKey);
        }
        return tag.getDouble(legacyKey);
    }
}
