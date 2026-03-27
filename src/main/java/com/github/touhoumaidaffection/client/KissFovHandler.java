package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class KissFovHandler {

    private static long zoomStartTime = -1;
    private static int zoomInTicks;
    private static int holdTicks;
    private static int zoomOutTicks;
    private static float zoomStrength;

    // Camera angle forcing
    private static int targetMaidId = -1;
    private static float startYaw;
    private static float startPitch;
    private static float targetYaw;
    private static float targetPitch;
    private static boolean carriedKiss;

    /**
     * Trigger a zero-distance FOV zoom + camera snap to maid's face.
     */
    public static void trigger(int maidEntityId, boolean isCarriedKiss) {
        zoomStartTime = System.currentTimeMillis();
        zoomInTicks = ModConfig.FOV_ZOOM_IN_TICKS.get();
        holdTicks = ModConfig.FOV_HOLD_TICKS.get();
        zoomOutTicks = ModConfig.FOV_ZOOM_OUT_TICKS.get();
        zoomStrength = ModConfig.FOV_ZOOM_STRENGTH.get().floatValue();
        targetMaidId = maidEntityId;
        carriedKiss = isCarriedKiss;

        // Capture current camera angles and calculate target
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            startYaw = mc.player.getYRot();
            startPitch = mc.player.getXRot();

            Entity maid = mc.level.getEntity(maidEntityId);
            if (maid != null) {
                Vec3 eyePos = mc.player.getEyePosition();
                Vec3 targetLookPos = carriedKiss ? getCarriedMaidHeadTarget(mc.player, maid) : maid.getEyePosition();
                Vec3 diff = targetLookPos.subtract(eyePos);
                double dist = diff.horizontalDistance();
                targetYaw = (float) (Mth.atan2(diff.z, diff.x) * Mth.RAD_TO_DEG) - 90.0F;
                targetPitch = (float) -(Mth.atan2(diff.y, dist) * Mth.RAD_TO_DEG);
            } else {
                targetYaw = startYaw;
                targetPitch = startPitch;
            }
        }
    }

    /**
     * Calculate the current animation progress factor (0 to 1 to 0).
     * Returns -1 if animation is not active.
     */
    private static float getAnimFactor() {
        if (zoomStartTime < 0) return -1f;

        long elapsed = System.currentTimeMillis() - zoomStartTime;
        float inMs = zoomInTicks * 50f;
        float holdMs = holdTicks * 50f;
        float outMs = zoomOutTicks * 50f;

        if (elapsed > inMs + holdMs + outMs) {
            zoomStartTime = -1;
            targetMaidId = -1;
            carriedKiss = false;
            return -1f;
        }

        if (elapsed < inMs) {
            return smoothstep(elapsed / inMs);
        } else if (elapsed < inMs + holdMs) {
            return 1.0f;
        } else {
            return 1.0f - smoothstep((elapsed - inMs - holdMs) / outMs);
        }
    }

    /**
     * Approximate the visible maid head position during princess-carry.
     * TLM renderer applies an extra carry transform in EntityMaidRenderer.setupRotations:
     * translate(-0.375, 0.8325, 0.375) with additional rotations, so the visible head is not at maid eye center.
     */
    private static Vec3 getCarriedMaidHeadTarget(Entity player, Entity maid) {
        Vec3 playerEye = player.getEyePosition();
        Vec3 maidEye = maid.getEyePosition();

        Vec3 forward = player.getLookAngle();
        Vec3 flatForward = new Vec3(forward.x, 0.0, forward.z);
        if (flatForward.lengthSqr() < 1.0E-6) {
            flatForward = new Vec3(0.0, 0.0, 1.0);
        } else {
            flatForward = flatForward.normalize();
        }
        Vec3 right = new Vec3(-flatForward.z, 0.0, flatForward.x);

        // Tuned for TLM carry pose, and configurable in common.toml.
        double sideOffset = ModConfig.FOV_CARRIED_SIDE_OFFSET.get();
        double forwardOffset = ModConfig.FOV_CARRIED_FORWARD_OFFSET.get();
        double verticalOffset = ModConfig.FOV_CARRIED_VERTICAL_OFFSET.get();
        Vec3 carriedHead = playerEye
                .add(right.scale(sideOffset))
                .add(flatForward.scale(forwardOffset))
                .add(0.0, verticalOffset, 0.0);

        // Blend a bit toward real maid eye to reduce model-variant mismatch.
        return maidEye.lerp(carriedHead, 0.7);
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        float factor = getAnimFactor();
        if (factor < 0) return;

        float modifier = 1.0f - zoomStrength * factor;
        event.setNewFovModifier(event.getNewFovModifier() * modifier);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float factor = getAnimFactor();
        if (factor < 0 || targetMaidId < 0) return;

        // Smoothly interpolate camera angles toward the maid's face
        float lerpedYaw = Mth.rotLerp(factor, startYaw, targetYaw);
        float lerpedPitch = Mth.lerp(factor, startPitch, targetPitch);

        event.setYaw(lerpedYaw);
        event.setPitch(lerpedPitch);
    }

    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
